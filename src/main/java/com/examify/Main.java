package com.examify.main;

import com.examify.model.ExamScheduler;
import com.examify.model.FileImportService;
import com.examify.model.ScheduleManager;
import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import com.examify.model.entities.Exam;
import com.examify.model.entities.Student;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        //input files
        String classroomFile = "data_classrooms.json";
        String studentFile = "data_students.json";
        String courseFile = "data_courses.json";
        String enrollmentFile = "data_enrollments.json";

        FileImportService fileService = new FileImportService();
        ScheduleManager manager = ScheduleManager.getInstance();

        // Clear data
        manager.clearAllData();

        System.out.println("IMPORT JSON FILES");

        List<Classroom> classrooms = fileService.importClassroomsJson(classroomFile);
        if (classrooms != null) {
            for (Classroom c : classrooms) manager.addClassroom(c);
            System.out.println("Classrooms loaded: " + manager.getClassrooms().size());
        }
        List<Student> students = fileService.importStudentsJson(studentFile);
        if (students != null) {
            for (Student s : students) manager.addStudent(s);
            System.out.println("Students loaded: " + manager.getStudents().size());
        }
        List<Course> courses = fileService.importCoursesJson(courseFile);
        if (courses != null) {
            for (Course c : courses) manager.addCourse(c);
            System.out.println("Courses loaded: " + manager.getCourses().size());
        }
        fileService.importEnrollmentsJson(enrollmentFile, manager.getCourses());
        System.out.println("Enrollments processed.\n");


        //Run the Algorithm ---
        System.out.println("Generating Schedule");

        ExamScheduler scheduler = new ExamScheduler();
        List<Exam> schedule = scheduler.generateSchedule();

        // --- 3. Display Results ---
        if (schedule.isEmpty()) {
            System.err.println("ERROR: Could not generate a schedule.");
            System.err.println("Error Logs:");
            for(String err : scheduler.getErrorLog()) {
                System.out.println(" - " + err);
            }
        } else {
            System.out.println("Schedule generated.\n");
            printSchedule(schedule);
        }
    }

    // print the schedule
    private static void printSchedule(List<Exam> exams) {
        String format = "%-15s %-10s %-7s %-8s %-10s %n";
        System.out.format("Course Code - Classroom - Day  -  Slot - Students  %n");

        for (Exam e : exams) {
            System.out.format(format, e.getCourse().getCode(), e.getClassroom().getId(), e.getDay(), e.getTimeSlot(), e.getCourse().getEnrolledStudents().size());
        }
    }
}