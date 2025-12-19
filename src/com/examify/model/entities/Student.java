package com.examify.model.entities;

import java.time.LocalDateTime;

public class Student {
    private String studentId;
    private LocalDateTime createdAt;
    
    public Student() {}
    
    public Student(String studentId) {
        this.studentId = studentId;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return String.format("Student{id=%s}", studentId);
    }
}