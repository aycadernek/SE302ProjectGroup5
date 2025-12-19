package com.examify.model;

import java.time.LocalDate;

public class SearchCriteria {
    private String studentId;
    private String courseCode;
    private String classroomId;
    private LocalDate examDate;
    private Integer scheduleId;
    
    public static class Builder {
        private SearchCriteria criteria = new SearchCriteria();
        
        public Builder studentId(String studentId) {
            criteria.studentId = studentId;
            return this;
        }
        
        public Builder courseCode(String courseCode) {
            criteria.courseCode = courseCode;
            return this;
        }
        
        public Builder classroomId(String classroomId) {
            criteria.classroomId = classroomId;
            return this;
        }
        
        public Builder examDate(LocalDate examDate) {
            criteria.examDate = examDate;
            return this;
        }
        
        public Builder scheduleId(Integer scheduleId) {
            criteria.scheduleId = scheduleId;
            return this;
        }
        
        public SearchCriteria build() {
            return criteria;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getStudentId() { return studentId; }
    public String getCourseCode() { return courseCode; }
    public String getClassroomId() { return classroomId; }
    public LocalDate getExamDate() { return examDate; }
    public Integer getScheduleId() { return scheduleId; }
    
    public boolean hasFilters() {
        return studentId != null || courseCode != null || 
               classroomId != null || examDate != null || scheduleId != null;
    }
}




