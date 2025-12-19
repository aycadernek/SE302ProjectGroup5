package com.examify.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern STUDENT_ID_PATTERN = 
        Pattern.compile("^\\d{9}$");
    private static final Pattern COURSE_CODE_PATTERN = 
        Pattern.compile("^[A-Z]{3}\\d{3}$");
    
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidStudentId(String studentId) {
        return studentId != null && STUDENT_ID_PATTERN.matcher(studentId).matches();
    }
    
    public static boolean isValidCourseCode(String courseCode) {
        return courseCode != null && COURSE_CODE_PATTERN.matcher(courseCode).matches();
    }
    
    public static boolean isPositiveInteger(String value) {
        try {
            int num = Integer.parseInt(value);
            return num > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    public static boolean isValidCapacity(int capacity) {
        return capacity > 0 && capacity <= 1000; 
    }
}