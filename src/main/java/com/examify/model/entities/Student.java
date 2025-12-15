package com.examify.model.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Student {
    String id;

    private List<Course> enrolledCourses;

    public Student(String id) {
        this.id = id;
        this.enrolledCourses = new ArrayList<>();
    }

    public void addCourse(Course course) {
        if (!enrolledCourses.contains(course)) {
            enrolledCourses.add(course);
        }
    }

    public String getId() {
        return id;
    }


    public List<Course> getEnrolledCourses() {
        return enrolledCourses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Student student = (Student) o;
        return Objects.equals(id, student.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
