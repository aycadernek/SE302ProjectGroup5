package com.examify.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class DateUtils {
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };
    
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateString.trim(), formatter);
            } catch (DateTimeParseException e) {
                
            }
        }
        
        throw new IllegalArgumentException("Invalid date format: " + dateString);
    }
    
    public static String formatDate(LocalDate date, String pattern) {
        if (date == null) return "";
        return DateTimeFormatter.ofPattern(pattern).format(date);
    }
    
    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6; 
    }
    
    public static boolean isDateInRange(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}