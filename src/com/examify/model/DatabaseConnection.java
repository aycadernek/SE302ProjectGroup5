package com.examify.model;

import com.examify.model.entities.*;
import java.sql.*;
import java.util.*;
import java.time.LocalDate;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String DB_URL = "jdbc:sqlite:examify.db";
    private static final DatabaseConnection instance = new DatabaseConnection();

    private Connection connection;

    private DatabaseConnection() {
        initializeDatabase();
    }

    public static DatabaseConnection getInstance() {
        return instance;
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            
            connection = DriverManager.getConnection(DB_URL);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            
            createTables();
            logger.info("Database initialized successfully");
        } catch (ClassNotFoundException e) {
            logger.severe("SQLite JDBC driver not found: " + e.getMessage());
            throw new RuntimeException("SQLite JDBC driver not found. Make sure sqlite-jdbc dependency is included.", e);
        } catch (SQLException e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    private void createTables() throws SQLException {
        String[] createTableStatements = {
            
            """
            CREATE TABLE IF NOT EXISTS students (
                student_id TEXT PRIMARY KEY,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            
            """
            CREATE TABLE IF NOT EXISTS courses (
                schedule_id INTEGER NOT NULL,
                course_code TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (schedule_id, course_code),
                FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE
            )
            """,
            
            
            """
            CREATE TABLE IF NOT EXISTS classrooms (
                schedule_id INTEGER NOT NULL,
                classroom_id TEXT NOT NULL,
                capacity INTEGER NOT NULL CHECK(capacity > 0),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (schedule_id, classroom_id),
                FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE
            )
            """,
            
            
            """
            CREATE TABLE IF NOT EXISTS schedules (
                schedule_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                start_date DATE NOT NULL,
                end_date DATE NOT NULL,
                slots_per_day INTEGER NOT NULL CHECK(slots_per_day > 0),
                min_slot_number INTEGER DEFAULT 1,
                max_slot_number INTEGER DEFAULT 8,
                max_exams_per_day INTEGER DEFAULT 2 CHECK(max_exams_per_day > 0),
                status TEXT DEFAULT 'draft',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            """
            CREATE TABLE IF NOT EXISTS enrollments (
                enrollment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                schedule_id INTEGER NOT NULL,
                student_id TEXT NOT NULL,
                course_code TEXT NOT NULL,
                FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE,
                FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
                FOREIGN KEY (schedule_id, course_code) REFERENCES courses(schedule_id, course_code) ON DELETE CASCADE,
                UNIQUE(schedule_id, student_id, course_code)
            )
            """,
            
            """
            CREATE TABLE IF NOT EXISTS exams (
                exam_id INTEGER PRIMARY KEY AUTOINCREMENT,
                schedule_id INTEGER NOT NULL,
                course_code TEXT NOT NULL,
                classroom_id TEXT NOT NULL,
                exam_date DATE NOT NULL,
                slot INTEGER NOT NULL CHECK(slot >= 0),
                duration INTEGER DEFAULT 2,
                FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE,
                FOREIGN KEY (schedule_id, course_code) REFERENCES courses(schedule_id, course_code),
                FOREIGN KEY (schedule_id, classroom_id) REFERENCES classrooms(schedule_id, classroom_id),
                UNIQUE(schedule_id, classroom_id, exam_date, slot),
                UNIQUE(schedule_id, course_code)
            )
            """
        };
        
        for (String sql : createTableStatements) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                logger.severe("Failed to execute SQL: " + sql);
                logger.severe("Error: " + e.getMessage());
                throw e;
            }
        }
        
        String[] indexStatements = {
            "CREATE INDEX IF NOT EXISTS idx_enrollments_student ON enrollments(student_id)",
            "CREATE INDEX IF NOT EXISTS idx_enrollments_course ON enrollments(course_code)",
            "CREATE INDEX IF NOT EXISTS idx_exams_schedule ON exams(schedule_id)",
            "CREATE INDEX IF NOT EXISTS idx_exams_date ON exams(exam_date)"
        };
        
        for (String sql : indexStatements) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                logger.warning("Failed to create index: " + sql + " - " + e.getMessage());
            }
        }
    }
     public int insertInitialSchedule(String name, LocalDate startDate, LocalDate endDate, int slotsPerDay, int minSlot, int maxSlot) throws SQLException {
        String insertScheduleSQL = """
            INSERT INTO schedules (name, start_date, end_date, slots_per_day, min_slot_number, max_slot_number, status)
            VALUES (?, ?, ?, ?, ?, ?, 'draft')
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertScheduleSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setDate(2, java.sql.Date.valueOf(startDate));
            pstmt.setDate(3, java.sql.Date.valueOf(endDate));
            pstmt.setInt(4, slotsPerDay);
            pstmt.setInt(5, minSlot);
            pstmt.setInt(6, maxSlot);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Failed to get schedule ID");
            }
        }
    }

    public void saveExams(int scheduleId, List<Exam> exams) throws SQLException {
        String deleteOldExamsSQL = "DELETE FROM exams WHERE schedule_id = ?";
        String insertExamSQL = """
            INSERT INTO exams (schedule_id, course_code, classroom_id, exam_date, slot, duration)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try {
            connection.setAutoCommit(false);
            
            try (PreparedStatement pstmt = connection.prepareStatement(deleteOldExamsSQL)) {
                pstmt.setInt(1, scheduleId);
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = connection.prepareStatement(insertExamSQL)) {
                for (Exam exam : exams) {
                    pstmt.setInt(1, scheduleId);
                    pstmt.setString(2, exam.getCourseCode());
                    pstmt.setString(3, exam.getClassroomId());
                    pstmt.setDate(4, java.sql.Date.valueOf(exam.getExamDate()));
                    pstmt.setInt(5, exam.getSlot());
                    pstmt.setInt(6, exam.getDuration());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            String updateStatusSQL = "UPDATE schedules SET status = 'finalized', updated_at = CURRENT_TIMESTAMP WHERE schedule_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateStatusSQL)) {
                pstmt.setInt(1, scheduleId);
                pstmt.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    public int saveSchedule(Schedule schedule) throws SQLException {
        String insertScheduleSQL = """
            INSERT INTO schedules (name, start_date, end_date, slots_per_day, min_slot_number, max_slot_number, max_exams_per_day, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        String insertExamSQL = """
            INSERT INTO exams (schedule_id, course_code, classroom_id, exam_date, slot, duration)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try {
            connection.setAutoCommit(false);
            
            int scheduleId;
            try (PreparedStatement pstmt = connection.prepareStatement(insertScheduleSQL, 
                    Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, schedule.getName());
                pstmt.setDate(2, java.sql.Date.valueOf(schedule.getStartDate()));
                pstmt.setDate(3, java.sql.Date.valueOf(schedule.getEndDate()));
                pstmt.setInt(4, schedule.getSlotsPerDay());
                pstmt.setInt(5, schedule.getMinSlot());
                pstmt.setInt(6, schedule.getMaxSlot());
                pstmt.setInt(7, schedule.getMaxExamsPerDay());
                pstmt.setString(8, "finalized");
                pstmt.executeUpdate();
                
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    scheduleId = rs.getInt(1);
                } else {
                    throw new SQLException("Failed to get schedule ID");
                }
            }
            
            try (PreparedStatement pstmt = connection.prepareStatement(insertExamSQL)) {
                try (PreparedStatement deletePstmt = connection.prepareStatement("DELETE FROM exams WHERE schedule_id = ?")) {
                    deletePstmt.setInt(1, scheduleId);
                    deletePstmt.executeUpdate();
                }

                for (Exam exam : schedule.getExams()) {
                    pstmt.setInt(1, scheduleId);
                    pstmt.setString(2, exam.getCourseCode());
                    pstmt.setString(3, exam.getClassroomId());
                    pstmt.setDate(4, java.sql.Date.valueOf(exam.getExamDate()));
                    pstmt.setInt(5, exam.getSlot());
                    pstmt.setInt(6, exam.getDuration());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            connection.commit();
            return scheduleId;
            
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    public void updateExamsForSchedule(int scheduleId, String name, LocalDate startDate, LocalDate endDate, int minSlot, int maxSlot, List<Exam> exams) throws SQLException {
        String updateScheduleSQL = """
            UPDATE schedules SET name = ?, start_date = ?, end_date = ?, 
            slots_per_day = ?, min_slot_number = ?, max_slot_number = ?, 
            updated_at = CURRENT_TIMESTAMP
            WHERE schedule_id = ?
        """;
        
        String deleteExamsSQL = "DELETE FROM exams WHERE schedule_id = ?";
        
        String insertExamSQL = """
            INSERT INTO exams (schedule_id, course_code, classroom_id, exam_date, slot, duration)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try {
            connection.setAutoCommit(false);
            
            try (PreparedStatement pstmt = connection.prepareStatement(updateScheduleSQL)) {
                pstmt.setString(1, name);
                pstmt.setDate(2, java.sql.Date.valueOf(startDate));
                pstmt.setDate(3, java.sql.Date.valueOf(endDate));
                pstmt.setInt(4, (maxSlot - minSlot + 1));
                pstmt.setInt(5, minSlot);
                pstmt.setInt(6, maxSlot);
                pstmt.setInt(7, scheduleId);
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = connection.prepareStatement(deleteExamsSQL)) {
                pstmt.setInt(1, scheduleId);
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = connection.prepareStatement(insertExamSQL)) {
                for (Exam exam : exams) {
                    pstmt.setInt(1, scheduleId);
                    pstmt.setString(2, exam.getCourseCode());
                    pstmt.setString(3, exam.getClassroomId());
                    pstmt.setDate(4, java.sql.Date.valueOf(exam.getExamDate()));
                    pstmt.setInt(5, exam.getSlot());
                    pstmt.setInt(6, exam.getDuration());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void finalizeScheduleUpdate(int actualId, int tempId, String name, LocalDate startDate, LocalDate endDate, int minSlot, int maxSlot, List<Exam> exams) throws SQLException {
        try (Statement pragmaStmt = connection.createStatement()) {
            pragmaStmt.execute("PRAGMA foreign_keys = OFF");
            try {
                connection.setAutoCommit(false);
                
                try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM exams WHERE schedule_id = ?")) {
                    pstmt.setInt(1, actualId);
                    pstmt.executeUpdate();
                }

                // 2. Refresh Courses & Enrollments
                // Check if we have ANY data in tempId (either courses or enrollments)
                boolean hasTempEnrollments;
                try (PreparedStatement pstmt = connection.prepareStatement("SELECT 1 FROM enrollments WHERE schedule_id = ? LIMIT 1")) {
                    pstmt.setInt(1, tempId);
                    hasTempEnrollments = pstmt.executeQuery().next();
                }
                
                boolean hasTempCourses;
                try (PreparedStatement pstmt = connection.prepareStatement("SELECT 1 FROM courses WHERE schedule_id = ? LIMIT 1")) {
                    pstmt.setInt(1, tempId);
                    hasTempCourses = pstmt.executeQuery().next();
                }

                if (hasTempCourses || hasTempEnrollments) {
                    // Delete old ones
                    try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM enrollments WHERE schedule_id = ?")) { pstmt.setInt(1, actualId); pstmt.executeUpdate(); }
                    try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM courses WHERE schedule_id = ?")) { pstmt.setInt(1, actualId); pstmt.executeUpdate(); }
                    
                    // Move new ones
                    try (PreparedStatement pstmt = connection.prepareStatement("UPDATE courses SET schedule_id = ? WHERE schedule_id = ?")) { pstmt.setInt(1, actualId); pstmt.setInt(2, tempId); pstmt.executeUpdate(); }
                    try (PreparedStatement pstmt = connection.prepareStatement("UPDATE enrollments SET schedule_id = ? WHERE schedule_id = ?")) { pstmt.setInt(1, actualId); pstmt.setInt(2, tempId); pstmt.executeUpdate(); }
                }

                boolean hasNewClassrooms;
                try (PreparedStatement pstmt = connection.prepareStatement("SELECT 1 FROM classrooms WHERE schedule_id = ? LIMIT 1")) {
                    pstmt.setInt(1, tempId);
                    hasNewClassrooms = pstmt.executeQuery().next();
                }
                if (hasNewClassrooms) {
                    try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM classrooms WHERE schedule_id = ?")) { pstmt.setInt(1, actualId); pstmt.executeUpdate(); }
                    try (PreparedStatement pstmt = connection.prepareStatement("UPDATE classrooms SET schedule_id = ? WHERE schedule_id = ?")) { pstmt.setInt(1, actualId); pstmt.setInt(2, tempId); pstmt.executeUpdate(); }
                }
                
                String updateSQL = "UPDATE schedules SET name=?, start_date=?, end_date=?, slots_per_day=?, min_slot_number=?, max_slot_number=?, updated_at=CURRENT_TIMESTAMP WHERE schedule_id=?";
                try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
                    pstmt.setString(1, name);
                    pstmt.setDate(2, java.sql.Date.valueOf(startDate));
                    pstmt.setDate(3, java.sql.Date.valueOf(endDate));
                    pstmt.setInt(4, (maxSlot - minSlot + 1));
                    pstmt.setInt(5, minSlot);
                    pstmt.setInt(6, maxSlot);
                    pstmt.setInt(7, actualId);
                    pstmt.executeUpdate();
                }
                
                String insertExamSQL = "INSERT INTO exams (schedule_id, course_code, classroom_id, exam_date, slot, duration) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(insertExamSQL)) {
                    for (Exam exam : exams) {
                        pstmt.setInt(1, actualId);
                        pstmt.setString(2, exam.getCourseCode());
                        pstmt.setString(3, exam.getClassroomId());
                        pstmt.setDate(4, java.sql.Date.valueOf(exam.getExamDate()));
                        pstmt.setInt(5, exam.getSlot());
                        pstmt.setInt(6, exam.getDuration());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
                
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
                pragmaStmt.execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    public void cleanupTemporaryData(int tempId) throws SQLException {
        String[] deleteSqls = {
            "DELETE FROM exams WHERE schedule_id = ?",
            "DELETE FROM enrollments WHERE schedule_id = ?",
            "DELETE FROM classrooms WHERE schedule_id = ?",
            "DELETE FROM courses WHERE schedule_id = ?"
        };
        for (String sql : deleteSqls) {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, tempId);
                pstmt.executeUpdate();
            }
        }
    }

    public void updateSchedule(Schedule schedule) throws SQLException {
        String updateScheduleSQL = """
            UPDATE schedules SET name = ?, start_date = ?, end_date = ?, 
            slots_per_day = ?, min_slot_number = ?, max_slot_number = ?, 
            max_exams_per_day = ?, status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE schedule_id = ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(updateScheduleSQL)) {
            pstmt.setString(1, schedule.getName());
            pstmt.setDate(2, java.sql.Date.valueOf(schedule.getStartDate()));
            pstmt.setDate(3, java.sql.Date.valueOf(schedule.getEndDate()));
            pstmt.setInt(4, (schedule.getMaxSlot() - schedule.getMinSlot() + 1));
            pstmt.setInt(5, schedule.getMinSlot());
            pstmt.setInt(6, schedule.getMaxSlot());
            pstmt.setInt(7, schedule.getMaxExamsPerDay());
            pstmt.setString(8, schedule.getStatus());
            pstmt.setInt(9, schedule.getScheduleId());
            pstmt.executeUpdate();
        }
    }
    
    public void deleteSchedule(int scheduleId) throws SQLException {
         String[] deleteSqls = {
            "DELETE FROM exams WHERE schedule_id = ?",
            "DELETE FROM enrollments WHERE schedule_id = ?",
            "DELETE FROM classrooms WHERE schedule_id = ?",
            "DELETE FROM courses WHERE schedule_id = ?",
            "DELETE FROM schedules WHERE schedule_id = ?"
        };
        try {
            connection.setAutoCommit(false);
            
            for (String sql : deleteSqls) {
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setInt(1, scheduleId);
                    pstmt.executeUpdate();
                }
            }
            
            connection.commit();
            
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    public Schedule loadSchedule(int scheduleId) throws SQLException {
        String scheduleSQL = "SELECT * FROM schedules WHERE schedule_id = ?";
        String examsSQL = """
            SELECT e.*, cr.capacity
            FROM exams e
            JOIN classrooms cr ON e.classroom_id = cr.classroom_id AND e.schedule_id = cr.schedule_id
            WHERE e.schedule_id = ?
            ORDER BY e.exam_date, e.slot
        """;
        
        try (PreparedStatement scheduleStmt = connection.prepareStatement(scheduleSQL)) {
            scheduleStmt.setInt(1, scheduleId);
            ResultSet rs = scheduleStmt.executeQuery();
            
            if (!rs.next()) {
                return null;
            }
            
            Schedule schedule = new Schedule();
            schedule.setScheduleId(rs.getInt("schedule_id"));
            schedule.setName(rs.getString("name"));
            
            java.sql.Date startDate = rs.getDate("start_date");
            if (startDate != null) {
                schedule.setStartDate(startDate.toLocalDate());
            }
            
            java.sql.Date endDate = rs.getDate("end_date");
            if (endDate != null) {
                schedule.setEndDate(endDate.toLocalDate());
            }
            
            schedule.setSlotsPerDay(rs.getInt("slots_per_day"));
            schedule.setMinSlot(rs.getInt("min_slot_number"));
            schedule.setMaxSlot(rs.getInt("max_slot_number"));
            schedule.setMaxExamsPerDay(rs.getInt("max_exams_per_day"));
            schedule.setStatus(rs.getString("status"));
            
            java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                schedule.setCreatedAt(createdAt.toLocalDateTime());
            }
            
            java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                schedule.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            
            try (PreparedStatement examStmt = connection.prepareStatement(examsSQL)) {
                examStmt.setInt(1, scheduleId);
                ResultSet examRs = examStmt.executeQuery();
                
                List<Exam> exams = new ArrayList<>();
                while (examRs.next()) {
                    Exam exam = new Exam();
                    exam.setExamId(examRs.getInt("exam_id"));
                    exam.setScheduleId(scheduleId);
                    exam.setCourseCode(examRs.getString("course_code"));
                    exam.setClassroomId(examRs.getString("classroom_id"));
                    
                    java.sql.Date examDate = examRs.getDate("exam_date");
                    if (examDate != null) {
                        exam.setExamDate(examDate.toLocalDate());
                    }
                    
                    exam.setSlot(examRs.getInt("slot"));
                    exam.setDuration(examRs.getInt("duration"));
                    exam.setCapacity(examRs.getInt("capacity"));
                    exams.add(exam);
                }
                schedule.setExams(exams);
            }
            
            return schedule;
        }
    }
    
    public List<Schedule> loadAllSchedules() throws SQLException {
        String schedulesSQL = "SELECT * FROM schedules ORDER BY created_at DESC";
        String examsSQL = """
            SELECT e.*, cr.capacity
            FROM exams e
            JOIN classrooms cr ON e.classroom_id = cr.classroom_id AND e.schedule_id = cr.schedule_id
            WHERE e.schedule_id IN (
        """ +
        "?) ORDER BY e.schedule_id, e.exam_date, e.slot";

        Map<Integer, Schedule> scheduleMap = new LinkedHashMap<>(); 

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(schedulesSQL)) {

            while (rs.next()) {
                Schedule schedule = new Schedule();
                schedule.setScheduleId(rs.getInt("schedule_id"));
                schedule.setName(rs.getString("name"));
                
                java.sql.Date startDate = rs.getDate("start_date");
                if (startDate != null) {
                    schedule.setStartDate(startDate.toLocalDate());
                }
                
                java.sql.Date endDate = rs.getDate("end_date");
                if (endDate != null) {
                    schedule.setEndDate(endDate.toLocalDate());
                }
                
                schedule.setSlotsPerDay(rs.getInt("slots_per_day"));
                schedule.setMinSlot(rs.getInt("min_slot_number"));
                schedule.setMaxSlot(rs.getInt("max_slot_number"));
                schedule.setMaxExamsPerDay(rs.getInt("max_exams_per_day"));
                schedule.setStatus(rs.getString("status"));
                
                java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    schedule.setCreatedAt(createdAt.toLocalDateTime());
                }
                
                java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    schedule.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                
                schedule.setExams(new ArrayList<>()); 
                scheduleMap.put(schedule.getScheduleId(), schedule);
            }
        }

        if (scheduleMap.isEmpty()) {
            return new ArrayList<>();
        }

        StringJoiner inClause = new StringJoiner(",");
        for (Integer scheduleId : scheduleMap.keySet()) {
            inClause.add("?");
        }
        String finalExamsSQL = examsSQL.replace("?", inClause.toString());

        try (PreparedStatement examStmt = connection.prepareStatement(finalExamsSQL)) {
            int i = 1;
            for (Integer scheduleId : scheduleMap.keySet()) {
                examStmt.setInt(i++, scheduleId);
            }

            ResultSet examRs = examStmt.executeQuery();
            while (examRs.next()) {
                int scheduleId = examRs.getInt("schedule_id");
                Schedule schedule = scheduleMap.get(scheduleId);

                if (schedule != null) {
                    Exam exam = new Exam();
                    exam.setExamId(examRs.getInt("exam_id"));
                    exam.setScheduleId(scheduleId);
                    exam.setCourseCode(examRs.getString("course_code"));
                    exam.setClassroomId(examRs.getString("classroom_id"));
                    
                    java.sql.Date examDate = examRs.getDate("exam_date");
                    if (examDate != null) {
                        exam.setExamDate(examDate.toLocalDate());
                    }
                    
                    exam.setSlot(examRs.getInt("slot"));
                    exam.setDuration(examRs.getInt("duration"));
                    exam.setCapacity(examRs.getInt("capacity"));
                    
                    schedule.getExams().add(exam);
                }
            }
        }

        return new ArrayList<>(scheduleMap.values());
    }

    public Course loadCourse(String courseCode , int scheduleId) throws SQLException {
        String courseSql = "SELECT * FROM courses WHERE course_code = ? AND schedule_id = ?" ;
        Course course = null;

        try (PreparedStatement stmt = connection.prepareStatement(courseSql)) {
            stmt.setString(1, courseCode);
            stmt.setInt(2, scheduleId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                course = new Course(rs.getString("course_code"));
            }
        }

        if (course != null) {
            String enrollmentSql = "SELECT student_id FROM enrollments WHERE course_code = ? AND schedule_id = ?";
            Set<String> enrolledStudents = new HashSet<>();
            try (PreparedStatement stmt = connection.prepareStatement(enrollmentSql)) {
                stmt.setString(1, courseCode);
                stmt.setInt(2, scheduleId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    enrolledStudents.add(rs.getString("student_id"));
                }
            }
            course.setEnrolledStudents(enrolledStudents);
        }

        return course;
    }
    
    public List<Course> loadAllCourses(int scheduleId) throws SQLException {
        String enrollmentSql = "SELECT course_code, student_id FROM enrollments WHERE schedule_id = ?";
        Map<String, Set<String>> enrollmentsByCourse = new HashMap<>();
         try (PreparedStatement stmt = connection.prepareStatement(enrollmentSql)) {
            stmt.setInt(1, scheduleId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String courseCode = rs.getString("course_code");
                String studentId = rs.getString("student_id");
                enrollmentsByCourse.computeIfAbsent(courseCode, k -> new HashSet<>()).add(studentId);
            }
        }

        String courseSql = "SELECT course_code FROM courses WHERE schedule_id = ? ORDER BY course_code";
        List<Course> courses = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(courseSql)) {
            stmt.setInt(1, scheduleId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String courseCode = rs.getString("course_code");
                Course course = new Course(courseCode);
                Set<String> enrolledStudents = enrollmentsByCourse.getOrDefault(courseCode, new HashSet<>());
                course.setEnrolledStudents(enrolledStudents);
                courses.add(course);
            }
        }
        
        return courses;
    }

    public List<Course> loadCourses(List<String> courseCodes) throws SQLException {
        if (courseCodes == null || courseCodes.isEmpty()) {
            return new ArrayList<>();
        }

        String placeholders = String.join(",", Collections.nCopies(courseCodes.size(), "?"));
        String courseSql = "SELECT * FROM courses WHERE course_code IN (" + placeholders + ")";
        Map<String, Course> coursesMap = new HashMap<>();

        try (PreparedStatement stmt = connection.prepareStatement(courseSql)) {
            for (int i = 0; i < courseCodes.size(); i++) {
                stmt.setString(i + 1, courseCodes.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String courseCode = rs.getString("course_code");
                Course course = new Course(courseCode);
                coursesMap.put(courseCode, course);
            }
        }

        String enrollmentSql = "SELECT course_code, student_id FROM enrollments WHERE course_code IN (" + placeholders + ")";
        try (PreparedStatement stmt = connection.prepareStatement(enrollmentSql)) {
            for (int i = 0; i < courseCodes.size(); i++) {
                stmt.setString(i + 1, courseCodes.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String courseCode = rs.getString("course_code");
                Course course = coursesMap.get(courseCode);
                if (course != null) {
                    if (course.getEnrolledStudents() == null) {
                        course.setEnrolledStudents(new HashSet<>());
                    }
                    course.getEnrolledStudents().add(rs.getString("student_id"));
                }
            }
        }

        return new ArrayList<>(coursesMap.values());
    }
    
    
    public List<Classroom> loadAllClassrooms(int scheduleId) throws SQLException {
        String sql = "SELECT * FROM classrooms WHERE schedule_id = ? ORDER BY classroom_id";
        List<Classroom> classrooms = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, scheduleId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Classroom classroom = new Classroom(
                    rs.getString("classroom_id"),
                    rs.getInt("capacity")
                );
                classrooms.add(classroom);
            }
        }
        
        return classrooms;
    }

    public List<Classroom> loadClassrooms(List<String> classroomIds) throws SQLException {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return new ArrayList<>();
        }
        String placeholders = String.join(",", Collections.nCopies(classroomIds.size(), "?"));
        String sql = "SELECT * FROM classrooms WHERE classroom_id IN (" + placeholders + ")";
        List<Classroom> classrooms = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < classroomIds.size(); i++) {
                stmt.setString(i + 1, classroomIds.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classrooms.add(new Classroom(
                    rs.getString("classroom_id"),
                    rs.getInt("capacity")
                ));
            }
        }
        return classrooms;
    }
    
    
    public List<Exam> searchExams(SearchCriteria criteria) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT e.*, cr.capacity
            FROM exams e
            JOIN classrooms cr ON e.classroom_id = cr.classroom_id AND e.schedule_id = cr.schedule_id
            WHERE 1=1
        """);
        
        List<Object> params = new ArrayList<>();
        
        if (criteria.getStudentId() != null) {
            sql = new StringBuilder("""
                SELECT e.*, cr.capacity
                FROM exams e
                JOIN classrooms cr ON e.classroom_id = cr.classroom_id AND e.schedule_id = cr.schedule_id
                JOIN enrollments en ON e.course_code = en.course_code AND e.schedule_id = en.schedule_id
                WHERE en.student_id = ?
            """);
            params.add(criteria.getStudentId());
        }
        
        if (criteria.getCourseCode() != null) {
            sql.append(" AND e.course_code = ?");
            params.add(criteria.getCourseCode());
        }
        
        if (criteria.getClassroomId() != null) {
            sql.append(" AND e.classroom_id = ?");
            params.add(criteria.getClassroomId());
        }
        
        if (criteria.getExamDate() != null) {
            sql.append(" AND e.exam_date = ?");
            params.add(java.sql.Date.valueOf(criteria.getExamDate()));
        }
        
        if (criteria.getScheduleId() != null) {
            sql.append(" AND e.schedule_id = ?");
            params.add(criteria.getScheduleId());
        }
        
        sql.append(" ORDER BY e.exam_date, e.slot");
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            List<Exam> exams = new ArrayList<>();
            
            while (rs.next()) {
                Exam exam = new Exam();
                exam.setExamId(rs.getInt("exam_id"));
                exam.setScheduleId(rs.getInt("schedule_id"));
                exam.setCourseCode(rs.getString("course_code"));
                exam.setClassroomId(rs.getString("classroom_id"));
                
                java.sql.Date examDate = rs.getDate("exam_date");
                if (examDate != null) {
                    exam.setExamDate(examDate.toLocalDate());
                }
                
                exam.setSlot(rs.getInt("slot"));
                exam.setDuration(rs.getInt("duration"));
                exam.setCapacity(rs.getInt("capacity"));
                exams.add(exam);
            }
            
            return exams;
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.warning("Error closing database connection: " + e.getMessage());
        }
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}