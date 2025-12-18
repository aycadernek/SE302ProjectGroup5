package com.examify.model;

import com.examify.model.entities.Exam;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DatabaseConnection {

    private final Gson gson;

    public DatabaseConnection() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    
    public void exportSchedule(List<Exam> schedule, String format, String filePath) {
        if (schedule == null || schedule.isEmpty()) {
            System.out.println("Warning: Schedule is empty, nothing to export.");
            return;
        }

     
        switch (format.toUpperCase()) {
            case "JSON":
                exportToJson(schedule, filePath);
                break;
            case "CSV":
                exportToCsv(schedule, filePath);
                break;
            case "EXCEL":
              
                String excelPath = filePath.endsWith(".csv") ? filePath : filePath + ".csv";
                exportToCsv(schedule, excelPath);
                break;
            case "PDF":
                exportToPdf(schedule, filePath);
                break;
            default:
                System.err.println("Error: Unsupported format selected: " + format);
        }
    }


    private void exportToJson(List<Exam> schedule, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
          
            String jsonOutput = gson.toJson(schedule);
            
            writer.write(jsonOutput);
            System.out.println("Export Success: Schedule exported to JSON at " + filePath);
        } catch (IOException e) {
            System.err.println("Export Error (JSON): " + e.getMessage());
        }
    }

    private void exportToCsv(List<Exam> schedule, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("Course Code,Classroom,Day,Time Slot,Number of Students");
            writer.newLine();

            for (Exam exam : schedule) {
                String line = String.format("%s,%s,%d,%d,%d",
                        exam.getCourse().getCode(),
                        exam.getClassroom().getId(),
                        exam.getDay(),
                        exam.getTimeSlot(),
                        exam.getCourse().getEnrolledStudents().size()
                );
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Export Success: Schedule exported to CSV at " + filePath);
        } catch (IOException e) {
            System.err.println("Export Error (CSV): " + e.getMessage());
        }
    }

    private void exportToPdf(List<Exam> schedule, String filePath) {
        System.out.println("Info: PDF export functionality requires external libraries.");
        System.out.println("Simulating PDF export for: " + filePath);
    }
}
