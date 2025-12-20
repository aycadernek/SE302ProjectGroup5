
package com.examify.model;

import com.examify.model.entities.*;
import com.examify.model.ExamScheduler.SchedulingException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleManager {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleManager.class);
    private Schedule currentSchedule;
    private final DatabaseConnection dbConnection;
    private final ExamScheduler examScheduler;
    
    public ScheduleManager(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
        this.examScheduler = new ExamScheduler();
    }
    
    public Schedule createSchedule(String name, LocalDate startDate,
                                   LocalDate endDate, int minSlot, int maxSlot,
                                   List<Course> courses, List<Classroom> classrooms)
            throws SchedulingException {

        Schedule schedule = examScheduler.generateSchedule(
                name, courses, classrooms, startDate, endDate, minSlot, maxSlot);

        schedule.setMinSlot(minSlot);
        schedule.setMaxSlot(maxSlot);
     

        try {
            int scheduleId = dbConnection.saveSchedule(schedule);
            schedule.setScheduleId(scheduleId);
            currentSchedule = schedule;

            return schedule;

        } catch (Exception e) {
            throw new SchedulingException("Failed to save schedule: " + e.getMessage(), e);
        }
    }
    
    public List<Schedule> getAllSchedules() {
        try {
            return dbConnection.loadAllSchedules();
        } catch (Exception e) {
            logger.error("Failed to load all schedules", e);
            return new ArrayList<>(); 
        }
    }
    
    public Schedule getSchedule(int scheduleId) {
        try {
            return dbConnection.loadSchedule(scheduleId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load schedule: " + e.getMessage(), e);
        }
    }
    
    public DatabaseConnection getDbConnection() {
        return dbConnection;
    }

    public Course getCourseWithStudents(String courseCode) {
        try {
            return dbConnection.loadCourse(courseCode);
        } catch (Exception e) {
            logger.error("Failed to load course with students: {}", courseCode, e);
            return null;
        }
    }
    
    public List<Exam> searchExams(SearchCriteria criteria) {
        try {
            return dbConnection.searchExams(criteria);
        } catch (Exception e) {
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }
    
    public List<Exam> filterByStudent(String studentId) {
        return searchExams(SearchCriteria.builder().studentId(studentId).build());
    }
    
    public List<Exam> filterByCourse(String courseCode) {
        return searchExams(SearchCriteria.builder().courseCode(courseCode).build());
    }
    
    public List<Exam> filterByClassroom(String classroomId) {
        return searchExams(SearchCriteria.builder().classroomId(classroomId).build());
    }
    
    public List<Exam> filterByDate(LocalDate date) {
        return searchExams(SearchCriteria.builder().examDate(date).build());
    }
    
    public Map<String, List<Exam>> getExamsByStudent() {
        List<Exam> allExams = currentSchedule != null ? currentSchedule.getExams() : new ArrayList<>();
        return allExams.stream()
            .collect(Collectors.groupingBy(Exam::getStudentName));
    }
    
    public Map<String, List<Exam>> getExamsByClassroom() {
        List<Exam> allExams = currentSchedule != null ? currentSchedule.getExams() : new ArrayList<>();
        return allExams.stream()
            .collect(Collectors.groupingBy(Exam::getRoomNumber));
    }
    
    public Map<LocalDate, List<Exam>> getExamsByDate() {
        List<Exam> allExams = currentSchedule != null ? currentSchedule.getExams() : new ArrayList<>();
        return allExams.stream()
            .collect(Collectors.groupingBy(Exam::getExamDate));
    }
    
    public void updateSchedule(Schedule schedule) {
        try {
            dbConnection.updateSchedule(schedule);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update schedule: " + e.getMessage(), e);
        }
    }

    public void deleteSchedule(int scheduleId) {
        try {
            dbConnection.deleteSchedule(scheduleId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete schedule: " + e.getMessage(), e);
        }
    }

    public Schedule recreateSchedule(int oldScheduleId, String name, LocalDate startDate, LocalDate endDate, int minSlot, int maxSlot, List<Course> courses, List<Classroom> classrooms) throws SchedulingException {
        try {
            deleteSchedule(oldScheduleId);
            return createSchedule(name, startDate, endDate, minSlot, maxSlot, courses, classrooms);
        } catch (Exception e) {
            throw new SchedulingException("Failed to recreate schedule: " + e.getMessage(), e);
        }
    }

    public List<Course> getCoursesWithDetails(List<String> courseCodes) {
        try {
            return dbConnection.loadCourses(courseCodes);
        } catch (Exception e) {
            logger.error("Failed to load courses with details", e);
            return new ArrayList<>();
        }
    }

    public List<Classroom> getClassroomsWithDetails(List<String> classroomIds) {
        try {
            return dbConnection.loadClassrooms(classroomIds);
        } catch (Exception e) {
            logger.error("Failed to load classrooms with details", e);
            return new ArrayList<>();
        }
    }
     
    
    public Schedule getCurrentSchedule() {
        return currentSchedule;
    }
    
    public void setCurrentSchedule(Schedule schedule) {
        this.currentSchedule = schedule;
    }
}