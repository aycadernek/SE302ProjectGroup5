package com.examify;

import com.examify.model.FileImportService;
import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import com.examify.model.entities.Student;
//dao imports will be added

import java.util.List;

public class Main {
    public static void main(String[] args) {


        //test file import
        FileImportService importService = new FileImportService();

        try {
            List<Student> students = importService.importStudentsFile("sampleData_AllStudents.csv");
            System.out.println("Students loaded: " + students.size());

            List<Classroom> classrooms = importService.importClassroomsFile("sampleData_AllClassroomsAndTheirCapacities.csv");
            System.out.println("Classrooms loaded: " + classrooms.size());

            List<Course> courses = importService.importCoursesFile("sampleData_AllCourses.csv");
            System.out.println("Courses loaded: " + courses.size());

            importService.importEnrollmentsFile("sampleData_AllAttendanceLists.csv", courses);

            for (Course co : courses) {
                System.out.println("Course: " + co.getCode() + " - Students: " + co.getEnrolledStudents().size());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
