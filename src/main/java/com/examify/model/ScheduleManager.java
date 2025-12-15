package com.examify.model;

import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import com.examify.model.entities.Exam;
import com.examify.model.entities.Student;

import java.util.ArrayList;
import java.util.List;

public class ScheduleManager {
    //Singleton
    private static ScheduleManager instance;

    //data in memory
    private List<Student> students;
    private List<Course> courses;
    private List<Classroom> classrooms;
    private List<Exam> generatedSchedule;

    private int totalDays = 5;
    private int slotsPerDay = 4;

    //private constructor
    public ScheduleManager() {
        this.students = new ArrayList<>();
        this.courses = new ArrayList<>();
        this.classrooms = new ArrayList<>();
        this.generatedSchedule = new ArrayList<>();
    }

    // Singleton getInstance()
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

    // connect student with course, course with student
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
        return generatedSchedule;
    }
    public void setSchedule(List<Exam> schedule) {
        this.generatedSchedule = schedule;
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

    // to clear old data when importing new ones
    public void clearAllData() {
        students.clear();
        courses.clear();
        classrooms.clear();
        generatedSchedule.clear();
    }
}
