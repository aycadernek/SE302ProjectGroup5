package com.examify.model.entities;

import java.time.LocalDateTime;

public class Enrollment {
    private int enrollmentId;
    private String studentId;
    private String courseCode;
    private LocalDateTime createdAt;
    
    public Enrollment() {}
    
    public Enrollment(String studentId, String courseCode) {
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.createdAt = LocalDateTime.now();
    }
    
    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return String.format("Enrollment{student=%s, course=%s}", 
            studentId, courseCode);
    }
}