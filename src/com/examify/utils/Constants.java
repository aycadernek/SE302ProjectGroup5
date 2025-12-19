package com.examify.utils;

public class Constants {
    public static final String APP_NAME = "Examify";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_TITLE = APP_NAME + " - University Exam Scheduler";
    
    public static final String DB_NAME = "examify.db";
    public static final int DB_TIMEOUT = 30; 
    
    public static final int DEFAULT_SLOTS_PER_DAY = 4;
    public static final int DEFAULT_MAX_EXAMS_PER_DAY = 2;
    public static final int MIN_GAP_BETWEEN_EXAMS = 1; 
    public static final int DEFAULT_EXAM_DURATION = 2; 
    
    public static final String[] SUPPORTED_IMPORT_FORMATS = {".csv", ".json", ".xlsx", ".xls"};
    public static final String[] SUPPORTED_EXPORT_FORMATS = {".pdf", ".xlsx", ".csv", ".json"};
    
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 800;
    public static final int DIALOG_WIDTH = 600;
    public static final int DIALOG_HEIGHT = 400;
    
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String[] SUPPORTED_LANGUAGES = {"en", "tr"};
    
    public static final String ERROR_DATABASE_CONNECTION = "Database connection failed";
    public static final String ERROR_FILE_NOT_FOUND = "File not found";
    public static final String ERROR_INVALID_FORMAT = "Invalid file format";
    public static final String ERROR_SCHEDULING_FAILED = "Failed to generate schedule";
    public static final String ERROR_VALIDATION_FAILED = "Validation failed";
    
    public static final String SUCCESS_IMPORT = "Data imported successfully";
    public static final String SUCCESS_EXPORT = "Schedule exported successfully";
    public static final String SUCCESS_SCHEDULE_CREATED = "Schedule created successfully";
    
    public static final String CONFIG_DIR = System.getProperty("user.home") + "/.examify";
    public static final String LOG_DIR = CONFIG_DIR + "/logs";
    public static final String EXPORT_DIR = CONFIG_DIR + "/exports";
}