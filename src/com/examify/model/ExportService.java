package com.examify.model;

import com.examify.model.entities.Exam;
import com.examify.model.entities.Schedule;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ExportService {
     
    public static void exportToPDF(Schedule schedule, String outputPath) throws IOException {
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Exam Schedule: " + schedule.getName()));
        document.add(new Paragraph("Period: " + schedule.getStartDate() + " to " + schedule.getEndDate()));

        Table table = new Table(5);
        table.addHeaderCell("Course");
        table.addHeaderCell("Date");
        table.addHeaderCell("Slot");
        table.addHeaderCell("Classroom");
        table.addHeaderCell("Capacity");

        for (Exam exam : schedule.getExams()) {
            table.addCell(exam.getCourseName());
            table.addCell(exam.getExamDate().toString());
            table.addCell(String.valueOf(exam.getSlot()));
            table.addCell(exam.getRoomNumber());
            table.addCell(String.valueOf(exam.getCapacity()));
        }

        document.add(table);
        document.close();
    }

    public static void exportToExcel(Schedule schedule, String outputPath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Schedule");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Course");
        headerRow.createCell(1).setCellValue("Date");
        headerRow.createCell(2).setCellValue("Slot");
        headerRow.createCell(3).setCellValue("Classroom");
        headerRow.createCell(4).setCellValue("Capacity");

        int rowNum = 1;
        for (Exam exam : schedule.getExams()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(exam.getCourseName());
            row.createCell(1).setCellValue(exam.getExamDate().toString());
            row.createCell(2).setCellValue(exam.getSlot());
            row.createCell(3).setCellValue(exam.getRoomNumber());
            row.createCell(4).setCellValue(exam.getCapacity());
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }
        workbook.close();
    }

    public static void exportToCSV(Schedule schedule, String outputPath) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Course;Date;Slot;Classroom;Capacity\n");

        for (Exam exam : schedule.getExams()) {
            csv.append(exam.getCourseName()).append(";")
               .append(exam.getExamDate()).append(";")
               .append(exam.getSlot()).append(";")
               .append(exam.getRoomNumber()).append(";")
               .append(exam.getCapacity()).append("\n");
        }

        Files.write(Paths.get(outputPath), csv.toString().getBytes());
    }

    public static void exportToJSON(Schedule schedule, String outputPath) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("\"name\": \"").append(schedule.getName()).append("\",\n");
        json.append("\"startDate\": \"").append(schedule.getStartDate()).append("\",\n");
        json.append("\"endDate\": \"").append(schedule.getEndDate()).append("\",\n");
        json.append("\"exams\": [\n");

        for (int i = 0; i < schedule.getExams().size(); i++) {
            Exam exam = schedule.getExams().get(i);
            json.append("  {\n");
            json.append("    \"course\": \"").append(exam.getCourseName()).append("\",\n");
            json.append("    \"date\": \"").append(exam.getExamDate()).append("\",\n");
            json.append("    \"slot\": ").append(exam.getSlot()).append(",\n");
            json.append("    \"classroom\": \"").append(exam.getRoomNumber()).append("\",\n");
            json.append("    \"capacity\": ").append(exam.getCapacity()).append("\n");
            json.append("  }");
            if (i < schedule.getExams().size() - 1) json.append(",");
            json.append("\n");
        }

        json.append("]\n");
        json.append("}");

        Files.write(Paths.get(outputPath), json.toString().getBytes());
    }
}