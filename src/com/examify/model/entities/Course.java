package com.examify.model.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Course {
    private String courseCode;
    private int studentCount;
    private Set<String> enrolledStudents = new HashSet<>();
    private LocalDateTime createdAt;
    
    public Course() {}
    
    public Course(String courseCode) {
        this.courseCode = courseCode;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    
    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    
    public Set<String> getEnrolledStudents() { return enrolledStudents; }
    public void setEnrolledStudents(Set<String> enrolledStudents) { 
        this.enrolledStudents = enrolledStudents;
        this.studentCount = enrolledStudents.size();
    }
    
    public void addEnrolledStudent(String studentId) {
        this.enrolledStudents.add(studentId);
        this.studentCount = enrolledStudents.size();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return String.format("Course{code=%s, students=%d}", courseCode, studentCount);
    }
}