package com.example.homeworkhoundv2;

import androidx.annotation.NonNull;

import java.util.Date;

public class Assignment {
    private String assignmentName;
    private Date dueDate;
    private String courseId;
    private int colorCode;

    Assignment(String assignmentName, Date dueDate, String courseId) {
        this.assignmentName = assignmentName;
        this.dueDate = dueDate;
        this.courseId = courseId;
    }

    Assignment(String assignmentName, Date dueDate, String courseId, int colorCode) {
        this.assignmentName = assignmentName;
        this.dueDate = dueDate;
        this.courseId = courseId;
        this.colorCode = colorCode;
    }

    public String getAssignmentName() {
        return assignmentName;
    }
    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
    }

    public Date getDueDate() {
        return dueDate;
    }
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getCourseId() {
        return courseId;
    }
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public int getColorCode() {
        return colorCode;
    }
    public void setColorCode(int colorCode) {
        this.colorCode = colorCode;
    }

    @NonNull
    @Override
    public String toString() {
        return "Name: " + assignmentName + "\nDue Date: " + dueDate + "\nCourse ID: " + courseId;
    }
}
