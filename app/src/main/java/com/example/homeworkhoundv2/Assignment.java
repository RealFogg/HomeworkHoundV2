package com.example.homeworkhoundv2;

import androidx.annotation.NonNull;

import java.util.Date;

public class Assignment {
    private String assignmentName;
    private Date dueDate;
    private String courseId;
    //private String uniqueID;

    // TODO: Make all implementations of assignment also include a uniqueID (this will help with querying data)
    // TODO: Generate the uniqueIDs based on number of assignments rather than user input
    Assignment(String assignmentName, Date dueDate, String courseId) {
        this.assignmentName = assignmentName;
        this.dueDate = dueDate;
        this.courseId = courseId;
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

    /*public String getUniqueID() {
        return uniqueID;
    }
    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }*/

    @NonNull
    @Override
    public String toString() {
        return "Name: " + assignmentName + "\nDue Date: " + dueDate + "\nCourse ID: " + courseId;
    }
}
