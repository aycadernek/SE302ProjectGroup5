package com.examify.model.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Course {
    private String code;
    private String name;

    private List<Student> enrolledStudents;

    public Course(String code, String name) {
        this.code = code;
        this.name = name;
        this.enrolledStudents = new ArrayList<>();
    }

    public void addStudent(Student student) {
        if (!enrolledStudents.contains(student)) {
            enrolledStudents.add(student);
        }
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public List<Student> getEnrolledStudents() {
        return enrolledStudents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(code, course.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code + " - " + name;
    }
}
