package com.examify.model;

import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import com.examify.model.entities.Student;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileImportService {
    private final Gson gson = new Gson();

    // --- CSV ---
    //1. CLASSROOM CSV IMPORT
    public List<Classroom> importClassroomsCSVFile(String filePath) {
        List<Classroom> classrooms = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Skip first line depending on the sample csv file "ALL OF THE CLASSROOMS; AND THEIR CAPACITIES..."
            br.readLine();

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Format: Classroom_01;40
                String[] parts = line.split(";");
                if (parts.length >= 2) {
                    Classroom c = new Classroom();
                    c.setId(parts[0].trim());
                    try {
                        c.setCapacity(Integer.parseInt(parts[1].trim()));
                        classrooms.add(c);
                    } catch (NumberFormatException e) {
                        e.printStackTrace(); //we can print to the error file
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("CSV Classroom file could not read: " + e.getMessage()); //
        }
        return classrooms;
    }

    //2. STUDENT CSV IMPORT
    public List<Student> importStudentsCSVFile(String filePath) {
        List<Student> students = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // skip first line, "ALL OF THE STUDENTS IN THE SYSTEM"
            br.readLine();

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                // Format: Std_ID_001
                students.add(new Student(line.trim()));
            }
        } catch (IOException e) {
            System.out.println("CSV Student file could not read: " + e.getMessage()); //
        }
        return students;
    }

    // 3. COURSE IMPORT CSV
    // This function only imports the Course names
    public List<Course> importCoursesCSVFile(String courseFile) {
        List<Course> courses = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(courseFile))) {
            String line;
            // Skip the first header line
            br.readLine();

            while ((line = br.readLine()) != null) {
                // If line is not empty, create a new course
                if (!line.trim().isEmpty()) {
                    courses.add(new Course(line.trim()));
                }
            }
        } catch (IOException e) {
            System.out.println("CSV Courses file could not read: " + e.getMessage()); //we can print to the error file
        }
        return courses;
    }

    //4. ENROLLMENTS CSV
    public void importEnrollmentsCSVFile(String attendanceFile, List<Course> existingCourses) {
        try (BufferedReader br = new BufferedReader(new FileReader(attendanceFile))) {
            String line;
            String currentCourseId = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) continue;

                // Check if this line is a list of students (starts with [ )
                if (line.startsWith("['")) {
                    if (currentCourseId != null) {

                        // remove [ ] and ' characters
                        String cleanLine = line.replace("[", "").replace("]", "").replace("'", "");

                        // Split to get IDs
                        String[] studentIds = cleanLine.split(",");

                        // Find the matching course in our list
                        for (Course c : existingCourses) {
                            if (c.getCode().equals(currentCourseId)) {

                                // Add each student ID to the course
                                for (String sId : studentIds) {
                                    String trimmedID = sId.trim();
                                    // Make sure ID is not empty
                                    if (!trimmedID.isEmpty()) {
                                        Student s = new Student(trimmedID);
                                        c.addStudent(s);
                                    }
                                }
                                // We found, so stop searching
                                break;
                            }
                        }
                    }
                } else {
                    // If it's not a list, it is Course Code (CourseCode_01)
                    currentCourseId = line;
                }
            }
            System.out.println("Enrollments loaded.");

        } catch (IOException e) {
            System.out.println("CSV Enrollments file could not read: " + e.getMessage()); //we can print to the error file
        }
    }


    // --- JSON ---
    // 1. CLASSROOM JSON (Format :[{"id":"Classroom_01", "capacity":30}, ....])
    public List<Classroom> importClassroomsJson(String filePath){
        List<Classroom> classrooms = new ArrayList<>();
        try (FileReader reader = new FileReader(filePath)) {
            Type list = new TypeToken<ArrayList<Classroom>>(){}.getType();
            classrooms = gson.fromJson(reader, list);
            System.out.println("Classrooms are loaded from JSON.");
        } catch (IOException e) {
            System.out.println("Json Classroom file could not read: " + e.getMessage());
        }
        return classrooms;
    }

    //2. STUDENTS JSON (Format: [{"id":"S_01"}, ...])
    public List<Student> importStudentsJson(String filePath) {
        List<Student> students = new ArrayList<>();
        try (FileReader reader = new FileReader(filePath)) {
            Type list = new TypeToken<ArrayList<Student>>(){}.getType();
            students = gson.fromJson(reader, list);
            System.out.println("Students loaded from JSON.");
        } catch (IOException e) {
            System.out.println("Json Students file could not read: " + e.getMessage());
        }
        return students;
    }

    // 3. COURSE JSON (Format: [{"code":"CS101"}, ...])
    public List<Course> importCoursesJson(String filePath) {
        List<Course> courses = new ArrayList<>();
        try (FileReader reader = new FileReader(filePath)) {
            Type list = new TypeToken<ArrayList<Course>>(){}.getType();
            courses = gson.fromJson(reader, list);
            System.out.println("Courses loaded from JSON.");
        } catch (IOException e) {
            System.out.println("Json Courses file could not read: " + e.getMessage());
        }
        return courses;
    }


    // 4. ENROLLMENT JSON (Format: [{"courseCode":"CS101", "studentIds":["S_1", "S_2"]}, ...])
    public void importEnrollmentsJson(String filePath, List<Course> existingCourses) {
        try (FileReader reader = new FileReader(filePath)) {
            Type list = new TypeToken<ArrayList<EnrollmentDATA>>(){}.getType();
            List<EnrollmentDATA> datas = gson.fromJson(reader, list);

            if (datas != null) {
                for (EnrollmentDATA data : datas) {
                    for (Course c : existingCourses) {
                        if (c.getCode().equals(data.courseCode)) {
                            for (String sId : data.studentIds) {
                                if (sId != null && !sId.trim().isEmpty()) {
                                    c.addStudent(new Student(sId.trim()));
                                }
                            }
                            break;
                        }
                    }
                }
            }
            System.out.println("Enrollments loaded from JSON.");

        } catch (IOException e) {
            System.out.println("Json Enrollments file could not read: " + e.getMessage());
        }
    }

    private static class EnrollmentDATA { //temp data holder
        String courseCode;
        List<String> studentIds;
    }

}