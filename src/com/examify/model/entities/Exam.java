package com.examify.model.entities;

import java.time.LocalDate;

public class Exam {
    private int examId;
    private int scheduleId;
    private String courseCode;
    private String classroomId;
    private LocalDate examDate;
    private int slot;
    private int duration = 2;
    private int capacity;
    
    public Exam() {}
    
    public Exam(String courseCode, String classroomId, LocalDate examDate, int slot) {
        this.courseCode = courseCode;
        this.classroomId = classroomId;
        this.examDate = examDate;
        this.slot = slot;
    }
    
    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }
    
    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }
    
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    
    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }
    
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public String getCourseName() { return courseCode; }
    public String getRoomNumber() { return classroomId; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getStudentName() { return ""; } 
    
    @Override
    public String toString() {
        return String.format("Exam{course=%s, date=%s, slot=%d, room=%s}", 
            courseCode, examDate, slot, classroomId);
    }
}