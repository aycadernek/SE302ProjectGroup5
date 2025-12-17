package com.examify.model;

import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import com.examify.model.entities.Exam;
import com.examify.model.entities.Student;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScheduleManager {
    //singleton
    private static ScheduleManager instance;

    //data in memory
    private List<Student> students;
    private List<Course> courses;
    private List<Classroom> classrooms;
    private List<Exam> masterSchedule;

    private int totalDays = 5;
    private int slotsPerDay = 4;

    //private constructor
    public ScheduleManager() {
        this.students = new ArrayList<>();
        this.courses = new ArrayList<>();
        this.classrooms = new ArrayList<>();
        this.masterSchedule = new ArrayList<>();
    }

    // singleton getInstance()
    public static ScheduleManager getInstance() {
        if (instance == null) {
            instance = new ScheduleManager();
        }
        return instance;
    }

    // adding data

    public void addStudent(Student s) {
        if (!students.contains(s)) {
            students.add(s);
        }
    }

    public void addCourse(Course c) {
        if (!courses.contains(c)) {
            courses.add(c);
        }
    }

    public void addClassroom(Classroom c) {
        classrooms.add(c);
    }

    // connect student with course
    public void enrollStudent(String studentId, String courseCode) {
        Student student = findStudentById(studentId);
        Course course = findCourseByCode(courseCode);

        if (student != null && course != null) {
            student.addCourse(course);
            course.addStudent(student);
        }
    }

    // helper search methods
    public Student findStudentById(String id) {
        return students.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public Course findCourseByCode(String code) {
        return courses.stream()
                .filter(c -> c.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    // searching and filtering implementation
    public List<Exam> searchSchedule(String query, String filterType) {
        if (masterSchedule == null || masterSchedule.isEmpty()) {
            return new ArrayList<>();
        }

        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(masterSchedule);
        }

        String q = query.toLowerCase().trim();
        List<Exam> results = new ArrayList<>(); // result list R

        for (Exam e : masterSchedule) {
            boolean match = false;

            if (filterType == null) filterType = "Course";

            switch (filterType) {
                case "Student":
                    for (Student s : e.getCourse().getEnrolledStudents()) {
                        if (s.getId().toLowerCase().contains(q)) {
                            match = true;
                            break;
                        }
                    }
                    break;

                case "Course":
                    if (e.getCourse().getCode().toLowerCase().contains(q)) {
                        match = true;
                    }
                    break;

                case "Classroom":
                    if (e.getClassroom().getId().toLowerCase().contains(q)) {
                        match = true;
                    }
                    break;

                case "Date":
                    if (String.valueOf(e.getDay()).equals(q)) {
                        match = true;
                    }
                    break;
            }

            if (match) {
                results.add(e);
            }
        }

        results.sort(Comparator.comparingInt(Exam::getDay).thenComparingInt(Exam::getTimeSlot));

        return results;
    }
    // getters and setters
    public List<Student> getStudents() {
        return students;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public List<Classroom> getClassrooms() {
        return classrooms;
    }

    public List<Exam> getSchedule() {
        return masterSchedule;
    }
    public void setSchedule(List<Exam> schedule) {
        this.masterSchedule = schedule;
    }

    public int getTotalDays() {
        return totalDays;
    }
    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public int getSlotsPerDay() {
        return slotsPerDay;
    }
    public void setSlotsPerDay(int slotsPerDay) {
        this.slotsPerDay = slotsPerDay;
    }

    // clear data
    public void clearAllData() {
        students.clear();
        courses.clear();
        classrooms.clear();
        masterSchedule.clear();
    }
}
