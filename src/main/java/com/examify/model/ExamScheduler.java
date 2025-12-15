package com.examify.model;

import com.examify.model.entities.Exam;
import com.examify.model.entities.Schedule;

import java.util.ArrayList;
import java.util.List;

public class ExamScheduler {

    private List<Exam> scheduledExams;

    private List<String> errorLog;

    public ExamScheduler() {
        this.scheduledExams = new ArrayList<>();
        this.errorLog = new ArrayList<>();
    }

    public List<String> getErrorLog() {
        return errorLog;
    }

    public List<Exam> generateSchedule() {
        ScheduleManager manager = ScheduleManager.getInstance();
    }
}
