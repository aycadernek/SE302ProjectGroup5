package com.examify.model;

import com.examify.model.entities.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ExamScheduler {

    public static final int DEFAULT_MAX_EXAMS_PER_DAY = 2;
    public static final int DEFAULT_MIN_GAP_BETWEEN_EXAMS = 1;
    public static final int DEFAULT_EXAM_DURATION_HOURS = 2;

    private int maxExamsPerDay;
    private int minGapBetweenExams;
    private List<Conflict> conflicts;
    private Random random;

    public ExamScheduler() {
        this(DEFAULT_MAX_EXAMS_PER_DAY, DEFAULT_MIN_GAP_BETWEEN_EXAMS);
    }

    public ExamScheduler(int maxExamsPerDay, int minGapBetweenExams) {
        this.maxExamsPerDay = maxExamsPerDay;
        this.minGapBetweenExams = minGapBetweenExams;
        this.conflicts = new ArrayList<>();
        this.random = new Random();
    }

   
    public Schedule generateSchedule(
            String scheduleName,
            List<Course> courses,
            List<Classroom> classrooms,
            LocalDate startDate,
            LocalDate endDate,
            int minSlot,
            int maxSlot) throws SchedulingException {

        conflicts.clear();

        if (minSlot < 0 || maxSlot < minSlot) { 
            throw new SchedulingException("Invalid slot range provided.");
        }
        int slotsPerDay = maxSlot - minSlot + 1;

        validateInputs(courses, classrooms, startDate, endDate, slotsPerDay);

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long totalAvailableSlots = totalDays * slotsPerDay * classrooms.size();

        if (courses.size() > totalAvailableSlots) {
            throw new SchedulingException(
                    String.format("Insufficient resources: %d courses need %d exam slots, "
                                    + "but only %d slots available (Days: %d, Slots/Day: %d, Rooms: %d)",
                            courses.size(), courses.size(), totalAvailableSlots,
                            totalDays, slotsPerDay, classrooms.size())
            );
        }

        List<Course> sortedCourses = preprocessCourses(courses);
        List<Classroom> sortedClassrooms = preprocessClassrooms(classrooms);
        Schedule schedule = new Schedule(scheduleName, startDate, endDate, slotsPerDay);
        schedule.setMinSlot(minSlot);
        schedule.setMaxSlot(maxSlot);
        schedule.setMaxExamsPerDay(maxExamsPerDay);
        Map<String, Map<LocalDate, Set<Integer>>> studentSchedule = new HashMap<>();
        Map<LocalDate, Map<Integer, Set<String>>> classroomOccupancy = new HashMap<>();
        Map<String, ExamSlotAssignment> courseAssignments = new HashMap<>();
        Map<LocalDate, Map<Integer, Set<String>>> forbiddenSlots = new HashMap<>();
        boolean scheduleComplete = false;
        int maxAttempts = 3;
        int attempt = 0;

        while (!scheduleComplete && attempt < maxAttempts) {
            try {
                scheduleComplete = attemptSchedule(
                        schedule, sortedCourses, sortedClassrooms,
                        studentSchedule, classroomOccupancy, courseAssignments, forbiddenSlots,
                        minSlot, maxSlot,
                        attempt
                );
            } catch (SchedulingException e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw new SchedulingException(
                            String.format("Failed to generate schedule after %d attempts. Last error: %s",
                                    maxAttempts, e.getMessage()), e);
                }
                schedule.getExams().clear();
                studentSchedule.clear();
                classroomOccupancy.clear();
                courseAssignments.clear();
                forbiddenSlots.clear();
            }
        }

        if (!scheduleComplete) {
            throw new SchedulingException("Failed to generate complete schedule");
        }

        optimizeSchedule(schedule, sortedCourses, sortedClassrooms);
        detectAndResolveConflicts(schedule, courses);

        return schedule;
    }

    private void validateInputs(List<Course> courses, List<Classroom> classrooms,
                                LocalDate startDate, LocalDate endDate, int slotsPerDay)
            throws SchedulingException {

        if (courses == null || courses.isEmpty()) {
            throw new SchedulingException("No courses provided for scheduling");
        }

        if (classrooms == null || classrooms.isEmpty()) {
            throw new SchedulingException("No classrooms available for exams");
        }

        if (startDate == null || endDate == null) {
            throw new SchedulingException("Exam period dates must be specified");
        }

        if (startDate.isAfter(endDate)) {
            throw new SchedulingException("Start date must be before end date");
        }

        if (slotsPerDay <= 0) {
            throw new SchedulingException("Slots per day must be positive");
        }

        Set<String> courseIds = new HashSet<>();
        for (Course course : courses) {
            if (courseIds.contains(course.getCourseCode())) {
                throw new SchedulingException("Duplicate course code: " + course.getCourseCode());
            }
            courseIds.add(course.getCourseCode());
        }

        int maxCapacity = classrooms.stream()
                .mapToInt(Classroom::getCapacity)
                .max()
                .orElse(0);

        List<String> oversizedCourses = courses.stream()
                .filter(course -> course.getStudentCount() > maxCapacity)
                .map(Course::getCourseCode)
                .collect(Collectors.toList());

        if (!oversizedCourses.isEmpty()) {
            throw new SchedulingException(
                    String.format("Courses exceed maximum classroom capacity (%d): %s",
                            maxCapacity, oversizedCourses)
            );
        }

        for (Course course : courses) {
            if (course.getStudentCount() != course.getEnrolledStudents().size()) {
                throw new SchedulingException(
                        String.format("Student count mismatch for course %s", course.getCourseCode()));
            }
        }
    }


    private List<Course> preprocessCourses(List<Course> courses) {
        return courses.stream()
                .sorted((c1, c2) -> {
                    int cmp = Integer.compare(c2.getStudentCount(), c1.getStudentCount());
                    if (cmp != 0) return cmp;

                    return c2.getCourseCode().compareTo(c1.getCourseCode());
                })
                .collect(Collectors.toList());
    }

    private List<Classroom> preprocessClassrooms(List<Classroom> classrooms) {
        return classrooms.stream()
                .sorted(Comparator
                        .comparingInt(Classroom::getCapacity)
                        .thenComparing(Classroom::getClassroomId))
                .collect(Collectors.toList());
    }

    private boolean attemptSchedule(
            Schedule schedule,
            List<Course> courses,
            List<Classroom> classrooms,
            Map<String, Map<LocalDate, Set<Integer>>> studentSchedule,
            Map<LocalDate, Map<Integer, Set<String>>> classroomOccupancy,
            Map<String, ExamSlotAssignment> courseAssignments,
            Map<LocalDate, Map<Integer, Set<String>>> forbiddenSlots,
            int minSlot,
            int maxSlot,
            int attempt) throws SchedulingException {

        LocalDate startDate = schedule.getStartDate();
        LocalDate endDate = schedule.getEndDate();
        int slotsPerDay = maxSlot - minSlot + 1;

        if (attempt > 0) {
            Collections.shuffle(courses, random);
        }

        List<Course> pendingCourses = new ArrayList<>(courses);
        int backtrackLimit = courses.size() * 2; 
        int backtracks = 0;

        for (int i = 0; i < pendingCourses.size(); i++) {
            Course course = pendingCourses.get(i);
            boolean placed = false;

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                if (placed) break;

                for (Classroom classroom : classrooms) {
                    if (placed) break;

                    if (classroom.getCapacity() < course.getStudentCount()) {
                        continue;
                    }

                    for (int slot = minSlot; slot <= maxSlot; slot++) {
                        if (placed) break;

                        if (isClassroomOccupied(date, slot, classroom.getClassroomId(), classroomOccupancy)) {
                            continue;
                        }

                        if (!checkStudentAvailability(course, date, slot, studentSchedule, forbiddenSlots)) {
                            continue;
                        }

                        Exam exam = createExam(course, classroom, date, slot);
                        schedule.addExam(exam);

                        updateStudentSchedule(course, date, slot, studentSchedule);
                        updateClassroomOccupancy(date, slot, classroom.getClassroomId(), classroomOccupancy);
                        updateForbiddenSlots(course, date, slot, forbiddenSlots, slotsPerDay, studentSchedule);

                        courseAssignments.put(course.getCourseCode(),
                                new ExamSlotAssignment(date, slot, classroom.getClassroomId()));

                        placed = true;
                    }
                }
            }

            if (!placed) {
                if (schedule.getExams().isEmpty() || backtracks >= backtrackLimit) {
                    throw new SchedulingException(
                            String.format("Cannot place course %s (students: %d). "
                                            + "Consider increasing exam period or slots per day.",
                                    course.getCourseCode(), course.getStudentCount()));
                }

                backtracks++;
                Exam lastExam = schedule.getExams().remove(schedule.getExams().size() - 1);
                Course lastCourse = pendingCourses.get(i - 1);

                removeConstraints(lastCourse, lastExam, studentSchedule,
                        classroomOccupancy, forbiddenSlots, slotsPerDay);
                courseAssignments.remove(lastCourse.getCourseCode());

                pendingCourses.add(lastCourse);
                pendingCourses.add(course);
                pendingCourses.remove(i - 1);
                pendingCourses.remove(i - 1);

                i = Math.max(0, i - 2);
            }
        }

        return schedule.getExams().size() == courses.size();
    }

    private boolean checkStudentAvailability(Course course, LocalDate date, int slot,
                                             Map<String, Map<LocalDate, Set<Integer>>> studentSchedule,
                                             Map<LocalDate, Map<Integer, Set<String>>> forbiddenSlots) {
        Map<Integer, Set<String>> dayForbiddenSlots = forbiddenSlots.get(date);
        if (dayForbiddenSlots != null) {
            Set<String> forbiddenStudents = dayForbiddenSlots.get(slot);
            if (forbiddenStudents != null) {
                for (String studentId : course.getEnrolledStudents()) {
                    if (forbiddenStudents.contains(studentId)) {
                        return false; 
                    }
                }
            }
        }

        for (String studentId : course.getEnrolledStudents()) {
            Map<LocalDate, Set<Integer>> studentDaySchedule = studentSchedule.get(studentId);
            if (studentDaySchedule != null) {
                Set<Integer> daySlots = studentDaySchedule.get(date);
                if (daySlots != null) {
                    if (daySlots.size() >= maxExamsPerDay) {
                        return false;
                    }

                    for (int existingSlot : daySlots) {
                        if (Math.abs(existingSlot - slot) <= minGapBetweenExams) {
                            return false;
                        }
                    }
                }
            }
        }

        return true; 
    }

    private boolean isClassroomOccupied(LocalDate date, int slot, String classroomId,
                                        Map<LocalDate, Map<Integer, Set<String>>> classroomOccupancy) {
        return Optional.ofNullable(classroomOccupancy.get(date))
                .map(slotOccupancy -> slotOccupancy.get(slot))
                .map(occupiedClassrooms -> occupiedClassrooms.contains(classroomId))
                .orElse(false);
    }

    private void updateStudentSchedule(Course course, LocalDate date, int slot,
                                       Map<String, Map<LocalDate, Set<Integer>>> studentSchedule) {

        for (String studentId : course.getEnrolledStudents()) {
            studentSchedule.computeIfAbsent(studentId, k -> new HashMap<>())
                    .computeIfAbsent(date, k -> new HashSet<>())
                    .add(slot);
        }
    }

    private void updateClassroomOccupancy(LocalDate date, int slot, String classroomId,
                                          Map<LocalDate, Map<Integer, Set<String>>> classroomOccupancy) {

        classroomOccupancy.computeIfAbsent(date, k -> new HashMap<>())
                .computeIfAbsent(slot, k -> new HashSet<>())
                .add(classroomId);
    }


    private void updateForbiddenSlots(Course course, LocalDate date, int slot,
                                      Map<LocalDate, Map<Integer, Set<String>>> forbiddenSlots,
                                      int slotsPerDay,
                                      Map<String, Map<LocalDate, Set<Integer>>> studentSchedule) {

        for (String studentId : course.getEnrolledStudents()) {
            Map<LocalDate, Set<Integer>> studentDaySchedule = studentSchedule.get(studentId);
            if (studentDaySchedule != null) {
                Set<Integer> daySlots = studentDaySchedule.get(date);
                if (daySlots != null) {
                    markSlotForbidden(date, slot, studentId, forbiddenSlots);

                    if (slot > 0) {
                        markSlotForbidden(date, slot - 1, studentId, forbiddenSlots);
                    }
                    if (slot < slotsPerDay - 1) {
                        markSlotForbidden(date, slot + 1, studentId, forbiddenSlots);
                    }

                    if (daySlots.size() >= maxExamsPerDay) {
                        for (int s = 0; s < slotsPerDay; s++) {
                            markSlotForbidden(date, s, studentId, forbiddenSlots);
                        }
                    }
                }
            }
        }
    }

    private void markSlotForbidden(LocalDate date, int slot, String studentId,
                                   Map<LocalDate, Map<Integer, Set<String>>> forbiddenSlots) {

        forbiddenSlots.computeIfAbsent(date, k -> new HashMap<>())
                .computeIfAbsent(slot, k -> new HashSet<>())
                .add(studentId);
    }


    private void removeConstraints(Course course, Exam exam,
                                   Map<String, Map<LocalDate, Set<Integer>>> studentSchedule,
                                   Map<LocalDate, Map<Integer, Set<String>>> classroomOccupancy,
                                   Map<LocalDate, Map<Integer, Set<String>>> forbiddenSlots,
                                   int slotsPerDay) {

        LocalDate examDate = exam.getExamDate();
        int examSlot = exam.getSlot();

        for (String studentId : course.getEnrolledStudents()) {
            Optional.ofNullable(studentSchedule.get(studentId))
                    .map(daySchedule -> daySchedule.get(examDate))
                    .ifPresent(daySlots -> {
                        daySlots.remove(examSlot);
                        if (daySlots.isEmpty()) {
                            studentSchedule.get(studentId).remove(examDate);
                        }
                    });
        }

        Optional.ofNullable(classroomOccupancy.get(examDate))
                .map(slotOccupancy -> slotOccupancy.get(examSlot))
                .ifPresent(occupied -> {
                    occupied.remove(exam.getClassroomId());
                    if (occupied.isEmpty()) {
                        classroomOccupancy.get(examDate).remove(examSlot);
                    }
                });

        for (String studentId : course.getEnrolledStudents()) {
            if (forbiddenSlots.containsKey(examDate)) {
                forbiddenSlots.get(examDate).values().forEach(studentSet -> studentSet.remove(studentId));
            }

            Set<Integer> remainingSlots = Optional.ofNullable(studentSchedule.get(studentId))
                    .map(daySchedule -> daySchedule.get(examDate))
                    .orElse(Collections.emptySet());

            for (int remainingSlot : remainingSlots) {
                markSlotForbidden(examDate, remainingSlot, studentId, forbiddenSlots); 
                if (remainingSlot > 0) {
                    markSlotForbidden(examDate, remainingSlot - 1, studentId, forbiddenSlots); 
                }
                if (remainingSlot < slotsPerDay - 1) {
                    markSlotForbidden(examDate, remainingSlot + 1, studentId, forbiddenSlots); 
                }
            }

            if (remainingSlots.size() >= maxExamsPerDay) {
                for (int s = 0; s < slotsPerDay; s++) {
                    markSlotForbidden(examDate, s, studentId, forbiddenSlots);
                }
            }
        }
    }

    private Exam createExam(Course course, Classroom classroom, LocalDate date, int slot) {
        Exam exam = new Exam(course.getCourseCode(), classroom.getClassroomId(), date, slot);
        exam.setDuration(DEFAULT_EXAM_DURATION_HOURS);
        exam.setCapacity(classroom.getCapacity());
        return exam;
    }


    private void optimizeSchedule(Schedule schedule, List<Course> courses, List<Classroom> classrooms) {
        balanceClassroomUsage(schedule, classrooms);

        minimizeStudentTravel(schedule, courses);

        schedule.getExams().sort(Comparator
                .comparing(Exam::getExamDate)
                .thenComparing(Exam::getSlot));
    }

    private void balanceClassroomUsage(Schedule schedule, List<Classroom> classrooms) {
        Map<String, Integer> classroomUsage = new HashMap<>();
        for (Exam exam : schedule.getExams()) {
            classroomUsage.merge(exam.getClassroomId(), 1, Integer::sum);
        }

    }

    private void minimizeStudentTravel(Schedule schedule, List<Course> courses) {
        
        Map<String, List<Exam>> examsByStudent = new HashMap<>();
        Map<String, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getCourseCode, c -> c));

        for (Exam exam : schedule.getExams()) {
            Course course = courseMap.get(exam.getCourseCode());
            if (course != null) {
                for (String studentId : course.getEnrolledStudents()) {
                    examsByStudent.computeIfAbsent(studentId, k -> new ArrayList<>())
                            .add(exam);
                }
            }
        }

      
    }

    private void detectAndResolveConflicts(Schedule schedule, List<Course> courses) {
        conflicts.clear();

        Map<String, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getCourseCode, c -> c));


        Map<LocalDate, Map<Integer, Set<String>>> classroomCheck = new HashMap<>();
        for (Exam exam : schedule.getExams()) {
            classroomCheck.computeIfAbsent(exam.getExamDate(), k -> new HashMap<>())
                    .computeIfAbsent(exam.getSlot(), k -> new HashSet<>())
                    .add(exam.getClassroomId());
        }

     
        Map<String, Map<LocalDate, List<Exam>>> examsByStudent = new HashMap<>();
        for (Exam exam : schedule.getExams()) {
            Course course = courseMap.get(exam.getCourseCode());
            if (course != null) {
                for (String studentId : course.getEnrolledStudents()) {
                    examsByStudent.computeIfAbsent(studentId, k -> new HashMap<>())
                            .computeIfAbsent(exam.getExamDate(), k -> new ArrayList<>())
                            .add(exam);
                }
            }
        }


        for (var studentEntry : examsByStudent.entrySet()) {
            String studentId = studentEntry.getKey();
            Map<LocalDate, List<Exam>> examsByDate = studentEntry.getValue();

            for (var dateEntry : examsByDate.entrySet()) {
                LocalDate date = dateEntry.getKey();
                List<Exam> exams = dateEntry.getValue();

       
                if (exams.size() > maxExamsPerDay) {
                    conflicts.add(new Conflict(
                            Conflict.Type.MAX_EXAMS_VIOLATION,
                            String.format("Student %s has %d exams on %s (max: %d)",
                                    studentId, exams.size(), date, maxExamsPerDay),
                            studentId, date, exams
                    ));
                }

          
                List<Integer> slots = exams.stream()
                        .map(Exam::getSlot)
                        .sorted()
                        .collect(Collectors.toList());

                for (int i = 0; i < slots.size() - 1; i++) {
                    final int nextSlot = slots.get(i + 1);
                    final int currentSlot = slots.get(i);
                    if (nextSlot - currentSlot <= minGapBetweenExams) {
                        List<Exam> consecutiveExams = exams.stream()
                                .filter(e -> e.getSlot() == currentSlot || e.getSlot() == nextSlot)
                                .collect(Collectors.toList());

                        conflicts.add(new Conflict(
                                Conflict.Type.CONSECUTIVE_EXAMS,
                                String.format("Student %s has consecutive exams at slots %d and %d on %s",
                                        studentId, slots.get(i), slots.get(i + 1), date),
                                studentId, date, consecutiveExams
                        ));
                    }
                }
            }
        }


        if (!conflicts.isEmpty()) {
            autoResolveConflicts(schedule, conflicts);
        }
    }

    private void autoResolveConflicts(Schedule schedule, List<Conflict> conflictsToResolve) {


        for (Conflict conflict : conflictsToResolve) {
            if (conflict.getType() == Conflict.Type.CONSECUTIVE_EXAMS &&
                    !conflict.getExams().isEmpty()) {

                Exam examToMove = conflict.getExams().get(0);

            }
        }
    }

    public List<Conflict> getConflicts() {
        return new ArrayList<>(conflicts);
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }



    private static class ExamSlotAssignment {
        final LocalDate date;
        final int slot;
        final String classroomId;

        ExamSlotAssignment(LocalDate date, int slot, String classroomId) {
            this.date = date;
            this.slot = slot;
            this.classroomId = classroomId;
        }
    }

    public static class Conflict {
        public enum Type {
            MAX_EXAMS_VIOLATION,
            CONSECUTIVE_EXAMS,
            CLASSROOM_DOUBLE_BOOKING,
            CAPACITY_EXCEEDED,
            UNAVAILABLE_CLASSROOM
        }

        private final Type type;
        private final String message;
        private final String studentId;
        private final LocalDate date;
        private final List<Exam> exams;

        public Conflict(Type type, String message, String studentId,
                        LocalDate date, List<Exam> exams) {
            this.type = type;
            this.message = message;
            this.studentId = studentId;
            this.date = date;
            this.exams = new ArrayList<>(exams);
        }

        public Type getType() { return type; }
        public String getMessage() { return message; }
        public String getStudentId() { return studentId; }
        public LocalDate getDate() { return date; }
        public List<Exam> getExams() { return new ArrayList<>(exams); }

        @Override
        public String toString() {
            return String.format("Conflict[%s]: %s", type, message);
        }
    }

    public static class SchedulingException extends Exception {
        public SchedulingException(String message) {
            super(message);
        }

        public SchedulingException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public ScheduleMetrics calculateMetrics(Schedule schedule, List<Course> courses) {
        Map<String, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getCourseCode, c -> c));

        int totalStudents = 0;
        int totalExams = schedule.getExams().size();
        int totalClassroomsUsed = (int) schedule.getExams().stream()
                .map(Exam::getClassroomId)
                .distinct()
                .count();

        Map<String, Integer> examsPerStudent = new HashMap<>();
        for (Exam exam : schedule.getExams()) {
            Course course = courseMap.get(exam.getCourseCode());
            if (course != null) {
                for (String studentId : course.getEnrolledStudents()) {
                    examsPerStudent.merge(studentId, 1, Integer::sum);
                }
            }
        }

        double avgExamsPerStudent = examsPerStudent.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return new ScheduleMetrics(totalExams, totalClassroomsUsed, avgExamsPerStudent);
    }

    public static class ScheduleMetrics {
        private final int totalExams;
        private final int classroomsUsed;
        private final double averageExamsPerStudent;

        public ScheduleMetrics(int totalExams, int classroomsUsed, double averageExamsPerStudent) {
            this.totalExams = totalExams;
            this.classroomsUsed = classroomsUsed;
            this.averageExamsPerStudent = averageExamsPerStudent;
        }

        public int getTotalExams() { return totalExams; }
        public int getClassroomsUsed() { return classroomsUsed; }
        public double getAverageExamsPerStudent() { return averageExamsPerStudent; }
    }

    public List<String> getImprovementSuggestions(Schedule schedule, List<Course> courses) {
        List<String> suggestions = new ArrayList<>();

        Map<String, Long> classroomUsage = schedule.getExams().stream()
                .collect(Collectors.groupingBy(Exam::getClassroomId, Collectors.counting()));

        long maxUsage = classroomUsage.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        classroomUsage.forEach((classroomId, usage) -> {
            if (usage < maxUsage / 3) {
                suggestions.add(String.format(
                        "Classroom %s is underutilized (%d exams vs max %d)",
                        classroomId, usage, maxUsage));
            }
        });

        Map<LocalDate, Long> examsPerDay = schedule.getExams().stream()
                .collect(Collectors.groupingBy(Exam::getExamDate, Collectors.counting()));

        long maxExamsPerDay = examsPerDay.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        long minExamsPerDay = examsPerDay.values().stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0);

        if (maxExamsPerDay - minExamsPerDay > 5) {
            suggestions.add("Exam distribution across days is uneven");
        }

        return suggestions;
    }
}
