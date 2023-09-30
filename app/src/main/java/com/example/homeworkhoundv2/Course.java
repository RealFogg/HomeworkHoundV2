package com.example.homeworkhoundv2;

public class Course {
    private String courseID;
    private String courseColor;

    public Course(String courseID, String courseColor) {
        this.courseID = courseID;
        this.courseColor = courseColor;
    }

    public String getCourseID() {
        return courseID;
    }
    public void setCourseID(String courseID) {
        this.courseID = courseID;
    }

    public String getCourseColor() {
        return courseColor;
    }
    public void setCourseColor(String courseColor) {
        this.courseColor = courseColor;
    }

    @Override
    public String toString() {
        return courseID;
    }
}

