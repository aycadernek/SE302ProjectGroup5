package com.examify;

import com.examify.model.FileImportService;
import com.examify.model.ScheduleManager;
import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import com.examify.model.entities.Exam;
import com.examify.model.entities.Student;
import com.examify.model.ExamScheduler;

//dao imports will be added

import java.util.List;

public class Main {
    public static void main(String[] args) {
        //test scheduling
        ScheduleManager manager = ScheduleManager.getInstance();
        //test file import
        FileImportService importService = new FileImportService();

        manager.clearAllData();

        try {
            List<Student> students = importService.importStudentsFile("sampleData_AllStudents.csv");
            for (Student s : students) {
                manager.addStudent(s);
            }
            System.out.println("Students loaded: " + manager.getStudents().size());

            List<Classroom> classrooms = importService.importClassroomsFile("sampleData_AllClassroomsAndTheirCapacities.csv");
            for (Classroom c : classrooms) {
                manager.addClassroom(c);
            }
            System.out.println("Classrooms loaded: " + manager.getClassrooms().size());

            List<Course> courses = importService.importCoursesFile("sampleData_AllCourses.csv");

            importService.importEnrollmentsFile("sampleData_AllAttendanceLists.csv", courses);

            for (Course co : courses) {
                manager.addCourse(co);
            }
            System.out.println("Courses loaded: " + manager.getCourses().size());

            if (!manager.getCourses().isEmpty()) {
                Course sampleCourse = manager.getCourses().get(0);
                System.out.println("Example course (" + sampleCourse.getCode() + ") Size: " + sampleCourse.getEnrolledStudents().size());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        // initiating algorithm
        ExamScheduler scheduler = new ExamScheduler();
        long startTime = System.currentTimeMillis();

        List<Exam> schedule = scheduler.generateSchedule();

        long endTime = System.currentTimeMillis();
        System.out.println("Algorithm completed. time: " + (endTime - startTime) + "ms");

        // report results
        System.out.println("\n--- results report ---");
        System.out.println("total matched exam: " + schedule.size());

        List<String> errors = scheduler.getErrorLog();
        if (!errors.isEmpty()) {
            System.out.println("WARNINGS/ERRORS (" + errors.size() + "):");
            for (String err : errors) {
                System.out.println(" - " + err);
            }
        } else {
            System.out.println("There are no conflicts or missing courses.");
        }

        // searching test
        System.out.println("\n--- searching test (CourseCode_01) ---");
        if (!schedule.isEmpty()) {
            String searchKey = "CourseCode_01";
            List<Exam> results = manager.searchSchedule(searchKey, "Course");
            System.out.println("Result: " + results.size() + " match found.");
            for (Exam e : results) {
                System.out.println("   -> " + e);
            }
        } else {
            System.out.println("The schedule was empty, so the search test could not be performed.");
        }

        System.out.println("\n=== TEST COMPLETED SUCCESSFULLY ===");
    }
}
