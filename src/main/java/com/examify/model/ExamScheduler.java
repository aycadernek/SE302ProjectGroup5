package com.examify.model;

import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import com.examify.model.entities.Exam;
import com.examify.model.entities.Student;

import java.util.*;

public class ExamScheduler {

    // Final result list
    private List<Exam> scheduledExams;
    private List<String> errorLog;

    // holds the occupied classrooms for each (day, slot) pair
    private Map<String, List<String>> slotOccupancy;

    // holds which slots are occupied for each student on each day
    private Map<String, List<Integer>> studentSchedule;

    // holds the students whose exams cannot be scheduled in each (day, slot) pair
    private Map<String, Set<String>> forbidden;

    public ExamScheduler() {
        this.scheduledExams = new ArrayList<>();
        this.errorLog = new ArrayList<>();
    }

    public List<String> getErrorLog() {
        return errorLog;
    }

    // main algorithm (find_schedule_greedy)
    public List<Exam> generateSchedule() {
        ScheduleManager manager = ScheduleManager.getInstance();

        // input Data
        List<Course> allCourses = new ArrayList<>(manager.getCourses());
        List<Classroom> classrooms = manager.getClassrooms();

        // initial constraints from Manager
        int numDays = manager.getTotalDays();
        int numSlotsPerDay = manager.getSlotsPerDay();
        int maxSlots = 10;

        // checks whether a schedule can be created, total courses > slot*day*rooms
        while (allCourses.size() > (numSlotsPerDay * numDays * classrooms.size())) {
            if (numSlotsPerDay < maxSlots) {
                numSlotsPerDay++;
                errorLog.add("INFO: Increased slots per day to " + numSlotsPerDay + " to fit courses.");
            } else {
                numDays++;
                errorLog.add("INFO: Increased number of days to " + numDays + " to fit courses.");
            }
        }

        // courses are sorted in descending order according to the number of students
        allCourses.sort((c1, c2) -> Integer.compare(c2.getEnrolledStudents().size(), c1.getEnrolledStudents().size()));

        // maximum capacity check
        int maxRoomCapacity = classrooms.stream().mapToInt(Classroom::getCapacity).max().orElse(0);
        for (Course c : allCourses) {
            if (c.getEnrolledStudents().size() > maxRoomCapacity) {
                errorLog.add("CRITICAL ERROR: Course " + c.getCode() + " size exceeds max room capacity.");
                return new ArrayList<>(); // cannot generate schedule
            }
        }


        boolean scheduleGenerated = false;
        // main loop runs until a schedule is generated
        while (!scheduleGenerated) {
            scheduledExams.clear();
            slotOccupancy = new HashMap<>();
            studentSchedule = new HashMap<>();
            forbidden = new HashMap<>();

            boolean restartNeeded = false;

            // iterate over sorted courses
            courseLoop:
            for (Course course : allCourses) {
                boolean assigned = false; // assigned initialized as false

                // suitable classrooms sorted in ascending order
                List<Classroom> suitableClassrooms = new ArrayList<>();
                for (Classroom r : classrooms) {
                    if (r.getCapacity() >= course.getEnrolledStudents().size()) {
                        suitableClassrooms.add(r);
                    }
                }
                suitableClassrooms.sort(Comparator.comparingInt(Classroom::getCapacity));

                // nested loops for day and slot
                daySlotLoop:
                for (int day = 1; day <= numDays; day++) {
                    for (int slot = 1; slot <= numSlotsPerDay; slot++) {

                        // check student availability
                        if (!checkStudentAvailability(course, day, slot)) {
                            continue; // if conflict, continue with next pair
                        }

                        // check room availability
                        for (Classroom room : suitableClassrooms) {
                            if (isRoomFree(room, day, slot)) {
                                // update main list
                                scheduledExams.add(new Exam(course, room, day, slot));

                                // update slot occupancy
                                String key = day + "_" + slot;
                                slotOccupancy.computeIfAbsent(key, k -> new ArrayList<>()).add(room.getId());

                                updateStudentSchedule(course, day, slot);
                                updateForbidden(course, day, slot, numSlotsPerDay);

                                assigned = true;
                                break daySlotLoop;
                            }
                        }
                    }
                }

                // if assigned is still not true, slot and day numbers are increased
                if (!assigned) {
                    restartNeeded = true;
                    break courseLoop;
                }
            }

            if (restartNeeded) {

                if (numSlotsPerDay < maxSlots) {
                    numSlotsPerDay++;
                    errorLog.add("RESTART: Increasing slots to " + numSlotsPerDay);
                } else {
                    numDays++;
                    errorLog.add("RESTART: Increasing days to " + numDays);
                }

            } else {
                scheduleGenerated = true; // success
            }
        }

        // update manager with final values
        manager.setTotalDays(numDays);
        manager.setSlotsPerDay(numSlotsPerDay);
        manager.setSchedule(new ArrayList<>(scheduledExams));

        return scheduledExams;
    }

    // helper 1: check_student_availability
    private boolean checkStudentAvailability(Course course, int day, int slot) {
        String key = day + "_" + slot;
        Set<String> forbiddenStudents = forbidden.getOrDefault(key, new HashSet<>());

        for (Student s : course.getEnrolledStudents()) {
            // checks whether all students are in the forbidden list
            if (forbiddenStudents.contains(s.getId())) {
                return false;
            }

            // checks whether students have more than two exams on same day
            List<Integer> slotsTaken = studentSchedule.getOrDefault(s.getId() + "_" + day, new ArrayList<>());
            if (slotsTaken.size() >= 2) return false;

            // checks whether they have two consecutive exams
            for (int takenSlot : slotsTaken) {
                if (Math.abs(takenSlot - slot) == 1) return false;
            }
        }
        return true;
    }

    // helper 2: update_student_schedule
    private void updateStudentSchedule(Course course, int day, int slot) {
        for (Student s : course.getEnrolledStudents()) {
            // adds the slot to the place corresponding to the given day in student schedule
            String key = s.getId() + "_" + day;
            studentSchedule.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
        }
    }

    // helper 3: update_forbidden
    private void updateForbidden(Course course, int day, int slot, int numSlotsPerDay) {
        for (Student s : course.getEnrolledStudents()) {
            // add to current slot
            addToForbidden(day, slot, s.getId());

            // add to previous slot (consecutive)
            if (slot > 1) addToForbidden(day, slot - 1, s.getId());

            // add to next slot (consecutive)
            if (slot < numSlotsPerDay) addToForbidden(day, slot + 1, s.getId());

            // if student has reached a total of two exams, all slots on that day
            List<Integer> slotsTaken = studentSchedule.getOrDefault(s.getId() + "_" + day, new ArrayList<>());
            if (slotsTaken.size() >= 2) {
                for (int i = 1; i <= numSlotsPerDay; i++) {
                    addToForbidden(day, i, s.getId());
                }
            }
        }
    }

    private void addToForbidden(int day, int slot, String studentId) {
        forbidden.computeIfAbsent(day + "_" + slot, k -> new HashSet<>()).add(studentId);
    }

    // helper 4: is_room_free
    private boolean isRoomFree(Classroom room, int day, int slot) {
        String key = day + "_" + slot;
        List<String> occupiedRooms = slotOccupancy.getOrDefault(key, new ArrayList<>());

        // if the class is present there, it returns false
        return !occupiedRooms.contains(room.getId());
    }
}