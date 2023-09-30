package com.example.homeworkhoundv2;

import java.util.ArrayList;
import java.util.List;

public class CourseListSingleton {
    private static CourseListSingleton instance;
    private List<Course> courseList = new ArrayList<>();

    private CourseListSingleton() {
        // Private constructor to prevent instantiation
    }

    public static CourseListSingleton getInstance() {
        if (instance == null) {
            instance = new CourseListSingleton();
        }
        return instance;
    }

    public List<Course> getCourseList() {
        return courseList;
    }

    public void setCourseList(List<Course> courseList) {
        this.courseList = courseList;
    }
}
