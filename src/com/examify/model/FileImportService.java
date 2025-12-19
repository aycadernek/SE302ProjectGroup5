package com.examify.model;

import com.examify.model.entities.Classroom;
import com.examify.model.entities.Course;
import com.examify.model.entities.Student;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVParserBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileImportService {
    private static final Logger logger = LoggerFactory.getLogger(FileImportService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DatabaseConnection dbConnection;

    private static final Map<String, String> STUDENT_HEADERS = Map.ofEntries(
        Map.entry("student_id", "student_id"), Map.entry("student id", "student_id"),
        Map.entry("studentid", "student_id"), Map.entry("stud_id", "student_id"),
        Map.entry("stud id", "student_id"), Map.entry("sid", "student_id")
    );

    private static final Map<String, String> COURSE_HEADERS = Map.ofEntries(
        Map.entry("course_code", "course_code"), Map.entry("course code", "course_code"),
        Map.entry("coursecode", "course_code"), Map.entry("code", "course_code")
    );

    private static final Map<String, String> CLASSROOM_HEADERS = Map.ofEntries(
        Map.entry("classroom_id", "classroom_id"), Map.entry("classroom id", "classroom id"),
        Map.entry("room", "classroom_id"), Map.entry("capacity", "capacity")
    );

    private static final Map<String, String> ENROLLMENT_HEADERS = Map.ofEntries(
        Map.entry("student_id", "student_id"), Map.entry("course_code", "course_code")
    );

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    public FileImportService(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }
    
    public enum DataType {
        STUDENTS, COURSES, CLASSROOMS, ENROLLMENTS
    }
    
    public ImportResult importData(Path filePath, DataType dataType) {
        ImportResult result = new ImportResult();
        try {
            if (!Files.exists(filePath)) {
                result.addError("File not found: " + filePath);
                return result;
            }

            String fileName = filePath.getFileName().toString().toLowerCase();

            if (fileName.endsWith(".csv")) {
                if (isSpecialMultiCourseFormat(filePath)) {
                    logger.info("Detected special multi-course enrollment file format.");
                    return processSpecialMultiCourseEnrollmentFile(filePath);
                }
                if (dataType == DataType.CLASSROOMS) {
                    logger.info("Classroom data type detected. Using dedicated classroom reader.");
                    List<Map<String, String>> rawData = readClassroomCSV(filePath);
                    return processStandardImport(rawData, dataType);
                }
            }
            
            List<Map<String, String>> rawData;
            if (fileName.endsWith(".csv")) {
                rawData = readStandardCSV(filePath);
            } else if (fileName.endsWith(".json")) {
                rawData = readJSON(filePath);
            } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                rawData = readExcel(filePath);
            } else {
                result.addError("Unsupported file format.");
                return result;
            }

            return processStandardImport(rawData, dataType);

        } catch (Exception e) {
            logger.error("Import failed", e);
            result.addError("Import failed: " + e.getMessage());
        }
        return result;
    }

    private ImportResult processStandardImport(List<Map<String, String>> rawData, DataType dataType) throws SQLException {
        ImportResult result = new ImportResult();
        if (rawData == null) {
            result.addError("Could not read or process file.");
            return result;
        }
        List<Map<String, String>> normalizedData = normalizeData(rawData, dataType);
        ValidationResult validation = validateData(normalizedData, dataType);
        result.addErrors(validation.getErrors());
        result.addWarnings(validation.getWarnings());

        if (validation.isValid()) {
            int importedCount = saveToDatabase(validation.getValidData(), dataType);
            result.setImportedCount(importedCount);
            result.setSuccess(true);
            result.setMessage(String.format("Successfully imported %d %s", importedCount, dataType.toString().toLowerCase()));
        }
        return result;
    }

    private boolean isSpecialMultiCourseFormat(Path filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String firstLine = reader.readLine();
            String secondLine = reader.readLine();
            return firstLine != null && secondLine != null &&
                   !firstLine.contains(",") && !firstLine.contains(";") &&
                   secondLine.trim().startsWith("[") && secondLine.trim().endsWith("]");
        } catch (IOException e) {
            return false;
        }
    }

    private ImportResult processSpecialMultiCourseEnrollmentFile(Path filePath) {
        ImportResult result = new ImportResult();
        int totalImported = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String courseCode = line.trim();
                if (courseCode.isEmpty()) continue;

                String studentListLine = reader.readLine();
                if (studentListLine == null) continue;

                List<String> studentIds = parseStudentList(studentListLine);
                if (studentIds.isEmpty()) continue;

                saveToDatabase(List.of(Map.of("course_code", courseCode)), DataType.COURSES);
                List<Map<String, String>> studentsToSave = studentIds.stream().map(id -> Map.of("student_id", id)).collect(Collectors.toList());
                saveToDatabase(studentsToSave, DataType.STUDENTS);

                List<Map<String, String>> enrollmentsToSave = studentIds.stream().map(id -> Map.of("student_id", id, "course_code", courseCode, "semester", "FALL2024")).collect(Collectors.toList());
                totalImported += saveToDatabase(enrollmentsToSave, DataType.ENROLLMENTS);
            }
            result.setSuccess(true);
            result.setImportedCount(totalImported);
            result.setMessage("Successfully imported " + totalImported + " total enrollments.");

        } catch (Exception e) {
            logger.error("Failed to process special multi-course enrollment file", e);
            result.addError("Processing failed: " + e.getMessage());
            result.setSuccess(false);
        }
        return result;
    }
    
    private List<String> parseStudentList(String studentListLine) {
        List<String> studentIds = new ArrayList<>();
        Pattern pattern = Pattern.compile("'([^']*)'");
        Matcher matcher = pattern.matcher(studentListLine);
        while (matcher.find()) {
            studentIds.add(matcher.group(1));
        }
        return studentIds;
    }

    private List<Map<String, String>> readClassroomCSV(Path filePath) {
        List<Map<String, String>> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String firstLine = reader.readLine();
            logger.debug("readClassroomCSV: First line read: '{}'", firstLine);

            if (firstLine == null || !firstLine.trim().equalsIgnoreCase("ALL OF THE CLASSROOMS; AND THEIR CAPACITIES IN THE SYSTEM")) {
                logger.warn("Classroom file does not have the expected special header. Falling back to standard CSV reader.");
                try {
                    return readStandardCSV(filePath);
                } catch (CsvException e) {
                    logger.error("Fallback to standard CSV failed for classroom file.", e);
                    return new ArrayList<>();
                }
            }
            
            String line;
            int lineNumber = 1; 
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                logger.debug("readClassroomCSV: Processing line {}: '{}'", lineNumber, line);
                if (line.trim().isEmpty()) {
                    logger.debug("readClassroomCSV: Skipping empty line {}.", lineNumber);
                    continue;
                }
                
                String[] parts = line.split(";", 2);
                if (parts.length == 2) {
                    String classroomId = parts[0].trim();
                    String capacity = parts[1].trim();
                    if (!classroomId.isEmpty() && !capacity.isEmpty()) {
                        data.add(Map.of("classroom_id", classroomId, "capacity", capacity));
                        logger.debug("readClassroomCSV: Parsed successfully from line {}: ID='{}', Capacity='{}'", lineNumber, classroomId, capacity);
                    } else {
                        logger.warn("readClassroomCSV: Skipping malformed line {} - Extracted ID: '{}', Capacity: '{}'", lineNumber, classroomId, capacity);
                    }
                } else {
                    logger.warn("readClassroomCSV: Skipping malformed line {} - Split parts: {}", lineNumber, parts.length, Arrays.toString(parts));
                }
            }
        } catch (Exception e) { 
            logger.error("readClassroomCSV: Failed to read special classroom CSV", e);
        }
        return data;
    }

    private List<Map<String, String>> readStandardCSV(Path filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(filePath.toFile())).withSkipLines(0).build()) {
            List<String[]> allLines = reader.readAll();
            if (allLines.isEmpty()) return new ArrayList<>();

            String[] headers = allLines.get(0);
            allLines.remove(0);

            List<Map<String, String>> data = new ArrayList<>();
            for (String[] row : allLines) {
                Map<String, String> record = new HashMap<>();
                for (int i = 0; i < headers.length && i < row.length; i++) {
                    record.put(headers[i], row[i]);
                }
                data.add(record);
            }
            return data;
        }
    }

    private List<Map<String, String>> normalizeData(List<Map<String, String>> rawData, DataType dataType) {
        List<Map<String, String>> normalized = new ArrayList<>();
        for (Map<String, String> record : rawData) {
            Map<String, String> normalizedRecord = new HashMap<>();
            for (Map.Entry<String, String> entry : record.entrySet()) {
                String normalizedKey = mapHeaderToField(entry.getKey(), dataType);
                if (normalizedKey != null) {
                    normalizedRecord.put(normalizedKey, normalizeValue(entry.getValue()));
                }
            }
            if (!normalizedRecord.isEmpty()) normalized.add(normalizedRecord);
        }
        return normalized;
    }

    private String mapHeaderToField(String header, DataType dataType) {
        String normalizedHeader = header.trim().toLowerCase();
        return switch (dataType) {
            case STUDENTS -> STUDENT_HEADERS.getOrDefault(normalizedHeader, normalizedHeader);
            case COURSES -> COURSE_HEADERS.getOrDefault(normalizedHeader, normalizedHeader);
            case CLASSROOMS -> CLASSROOM_HEADERS.getOrDefault(normalizedHeader, normalizedHeader);
            case ENROLLMENTS -> ENROLLMENT_HEADERS.getOrDefault(normalizedHeader, normalizedHeader);
        };
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }
    
    private List<Map<String, String>> readJSON(Path filePath) throws IOException {
        byte[] jsonData = Files.readAllBytes(filePath);
        List<Map<String, String>> data = new ArrayList<>();
        try {
            List<Map<String, Object>> array = objectMapper.readValue(jsonData, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            for (Map<String, Object> item : array) {
                Map<String, String> record = new HashMap<>();
                for (Map.Entry<String, Object> entry : item.entrySet()) {
                    record.put(entry.getKey().toLowerCase(), 
                        entry.getValue() != null ? entry.getValue().toString() : "");
                }
                data.add(record);
            }
        } catch (Exception e) {
            Map<String, Object> single = objectMapper.readValue(jsonData, Map.class);
            Map<String, String> record = new HashMap<>();
            for (Map.Entry<String, Object> entry : single.entrySet()) {
                record.put(entry.getKey().toLowerCase(), 
                    entry.getValue() != null ? entry.getValue().toString() : "");
            }
            data.add(record);
        }
        return data;
    }
    
    private List<Map<String, String>> readExcel(Path filePath) throws IOException {
        List<Map<String, String>> data = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(filePath.toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) return data;
            
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim().toLowerCase());
            }
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> record = new HashMap<>();
                for (int i = 0; i < headers.size() && i < row.getLastCellNum(); i++) {
                    Cell currentCell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    record.put(headers.get(i), getCellValue(currentCell));
                }
                data.add(record);
            }
        }
        return data;
    }
    
    private String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }
    
    private ValidationResult validateData(List<Map<String, String>> rawData, DataType dataType) {
        ValidationResult result = new ValidationResult();
        List<Map<String, String>> validData = new ArrayList<>();
        for(Map<String, String> row : rawData){
            if (dataType == DataType.CLASSROOMS) {
                if(row.get("classroom_id") == null || row.get("classroom_id").isEmpty()){
                     result.addError(0, "Classroom ID is required.");
                }
                if(row.get("capacity") == null || row.get("capacity").isEmpty()){
                     result.addError(0, "Capacity is required.");
                }
            }
             validData.add(row);
        }
        result.setValidData(validData);
        return result;
    }
    
    private int saveToDatabase(List<Map<String, String>> validData, DataType dataType) throws SQLException {
        return switch (dataType) {
            case STUDENTS -> saveStudents(validData);
            case COURSES -> saveCourses(validData);
            case CLASSROOMS -> saveClassrooms(validData);
            case ENROLLMENTS -> saveEnrollments(validData);
        };
    }
    
    private int saveStudents(List<Map<String, String>> data) throws SQLException {
        String sql = "INSERT OR IGNORE INTO students (student_id) VALUES (?)";
        try (var pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            for (Map<String, String> record : data) {
                String studentId = record.get("student_id");
                if (studentId != null && !studentId.isEmpty()) {
                    pstmt.setString(1, studentId);
                    pstmt.addBatch();
                }
            }
            return Arrays.stream(pstmt.executeBatch()).filter(r -> r >= 0).sum();
        }
    }
    
    private int saveCourses(List<Map<String, String>> data) throws SQLException {
        String sql = "INSERT OR IGNORE INTO courses (course_code) VALUES (?)";
        try (var pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            for (Map<String, String> record : data) {
                String courseCode = record.get("course_code");
                if (courseCode != null && !courseCode.isEmpty()) {
                    pstmt.setString(1, courseCode);
                    pstmt.addBatch();
                }
            }
            return Arrays.stream(pstmt.executeBatch()).filter(r -> r >= 0).sum();
        }
    }
    
    private int saveClassrooms(List<Map<String, String>> data) throws SQLException {
        String sql = "INSERT OR REPLACE INTO classrooms (classroom_id, capacity) VALUES (?, ?)";
        try (var pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            for (Map<String, String> record : data) {
                String classroomId = record.get("classroom_id");
                String capacityStr = record.get("capacity");
                if (classroomId != null && !classroomId.isEmpty() && capacityStr != null && !capacityStr.isEmpty()) {
                    pstmt.setString(1, classroomId);
                    pstmt.setInt(2, Integer.parseInt(capacityStr));
                    pstmt.addBatch();
                }
            }
            return Arrays.stream(pstmt.executeBatch()).filter(r -> r >= 0).sum();
        }
    }
    
    private int saveEnrollments(List<Map<String, String>> data) throws SQLException {
        String sql = "INSERT OR IGNORE INTO enrollments (student_id, course_code, semester) VALUES (?, ?, ?)";
        try (var pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            for (Map<String, String> record : data) {
                String studentId = record.get("student_id");
                String courseCode = record.get("course_code");
                String semester = record.get("semester");
                if (studentId != null && !studentId.isEmpty() && courseCode != null && !courseCode.isEmpty()) {
                    pstmt.setString(1, studentId);
                    pstmt.setString(2, courseCode);
                    pstmt.setString(3, semester != null ? semester.trim() : "FALL2024");
                    pstmt.addBatch();
                }
            }
            return Arrays.stream(pstmt.executeBatch()).filter(r -> r >= 0).sum();
        }
    }
    
    public static class ImportResult {
        private boolean success = false;
        private String message;
        private int importedCount = 0;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getImportedCount() { return importedCount; }
        public void setImportedCount(int importedCount) { this.importedCount = importedCount; }
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
        public void addErrors(List<String> errors) { this.errors.addAll(errors); }
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        public void addWarnings(List<String> warnings) { this.warnings.addAll(warnings); }
    }
    
    public static class ValidationResult {
        private List<Map<String, String>> validData = new ArrayList<>();
        private Map<Integer, List<String>> lineErrors = new HashMap<>();
        private Map<Integer, List<String>> lineWarnings = new HashMap<>();
        public boolean isValid() { return lineErrors.isEmpty(); }
        public boolean hasErrorsForLine(int lineNumber) { return lineErrors.containsKey(lineNumber); }
        public List<String> getErrors() {
            List<String> allErrors = new ArrayList<>();
            lineErrors.forEach((line, errs) -> errs.forEach(e -> allErrors.add("Line " + line + ": " + e)));
            return allErrors;
        }
        public List<String> getWarnings() {
            List<String> allWarnings = new ArrayList<>();
            lineWarnings.forEach((line, warns) -> warns.forEach(w -> allWarnings.add("Line " + line + ": " + w)));
            return allWarnings;
        }
        public void addError(int lineNumber, String error) { lineErrors.computeIfAbsent(lineNumber, k -> new ArrayList<>()).add(error); }
        public void addWarning(int lineNumber, String warning) { lineWarnings.computeIfAbsent(lineNumber, k -> new ArrayList<>()).add(warning); }
        public List<Map<String, String>> getValidData() { return validData; }
        public void setValidData(List<Map<String, String>> validData) { this.validData = validData; }
    }
}