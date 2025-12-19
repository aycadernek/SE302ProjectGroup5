package com.examify.model.entities;

import java.time.LocalDateTime;

public class Classroom {
    private String classroomId;
    private int capacity;
    private LocalDateTime createdAt;
    
    public Classroom() {}
    
    public Classroom(String classroomId, int capacity) {
        this.classroomId = classroomId;
        this.capacity = capacity;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }
    
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return String.format("Classroom{id=%s, capacity=%d}", classroomId, capacity);
    }
}