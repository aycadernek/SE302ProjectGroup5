package com.examify.model.entities;

public class Exam {

    private Course course;
    private Classroom classroom;
    private int day;
    private int timeSlot;

    public Exam(Course course, Classroom classroom, int day, int timeSlot) {
        this.course = course;
        this.classroom = classroom;
        this.day = day;
        this.timeSlot = timeSlot;
    }

    public Course getCourse() {
        return course;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public int getDay() {
        return day;
    }

    public int getTimeSlot() {
        return timeSlot;
    }

    @Override
    public String toString() {
        return String.format("Day: %d, Time Slot: %d | %s -> %s",
                day, timeSlot, course.getCode(), classroom.getId());
    }
}
