package com.examify.model.entities;

public class Classroom {

    private String id;
    private int capacity;

    public Classroom(){}
    public Classroom(String id, String name, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public void setCapacity(int capacity){
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return id + " (Capacity: " + capacity + ")";
    }
}
