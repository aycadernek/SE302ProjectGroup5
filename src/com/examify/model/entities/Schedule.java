package com.examify.model.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Schedule {
    private int scheduleId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private int slotsPerDay;
    private int minSlot;
    private int maxSlot;
    private int maxExamsPerDay = 2;
    private String status = "draft";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Exam> exams = new ArrayList<>();
    
    public Schedule() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Schedule(String name, LocalDate startDate, LocalDate endDate, int slotsPerDay) {
        this();
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.slotsPerDay = slotsPerDay;
    }
    
    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public int getSlotsPerDay() { return slotsPerDay; }
    public void setSlotsPerDay(int slotsPerDay) { this.slotsPerDay = slotsPerDay; }

    public int getMinSlot() { return minSlot; }
    public void setMinSlot(int minSlot) { this.minSlot = minSlot; }

    public int getMaxSlot() { return maxSlot; }
    public void setMaxSlot(int maxSlot) { this.maxSlot = maxSlot; }

    
    public int getMaxExamsPerDay() { return maxExamsPerDay; }
    public void setMaxExamsPerDay(int maxExamsPerDay) { this.maxExamsPerDay = maxExamsPerDay; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<Exam> getExams() { return exams; }
    public void setExams(List<Exam> exams) { this.exams = exams; }
    
    public void addExam(Exam exam) {
        this.exams.add(exam);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void removeExam(Exam exam) {
        this.exams.remove(exam);
        this.updatedAt = LocalDateTime.now();
    }
    
    public int getTotalExams() {
        return exams.size();
    }
    
    @Override
    public String toString() {
        return String.format("Schedule{id=%d, name=%s, exams=%d, period=%s to %s}", 
            scheduleId, name, exams.size(), startDate, endDate);
    }
}