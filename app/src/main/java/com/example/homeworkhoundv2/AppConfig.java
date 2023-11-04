package com.example.homeworkhoundv2;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppConfig {
    // Google Sheet ID
    public static final String SPREADSHEET_ID = "1fy9jX-jukiP6qxl4NEmKUoXMG1ZHj5oeWeICndMoAAA";

    // DateFormats:
    public static final SimpleDateFormat customGoogleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public static final SimpleDateFormat googleDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    public static final SimpleDateFormat longDate = new SimpleDateFormat("MM/dd/yy", Locale.US);
    public static final SimpleDateFormat shortMonthDate = new SimpleDateFormat("M/dd/yy", Locale.US);
    public static final SimpleDateFormat shortDayDate = new SimpleDateFormat("MM/d/yy", Locale.US);
    public static final SimpleDateFormat shortDate = new SimpleDateFormat("M/d/yy", Locale.US);

    // Static non-final variables
    public static int totalAssignmentsCount = 0;

    public static final int ASSIGNMENTS_RANGE_START = 14;         // The starting row for the assignments table

    // Static methods for global use
    /** Method to convert a string to a date format
     * Parameter: stringToDate - the string you want to convert to a date
     * Returns:   A date object that matches the string or null if parsing failed*/
    public static Date convertStringToDateFormat(String stringToDate) {
        // String Format: MM/dd/YYYY (Google Sheet view of date)
        try {
            return googleDateFormat.parse(stringToDate);
        }catch (ParseException pe) {
            try {
                return shortDate.parse(stringToDate);
            } catch (ParseException e) {
                try {
                    return shortMonthDate.parse(stringToDate);
                } catch (ParseException ex) {
                    try {
                        return shortDayDate.parse(stringToDate);
                    } catch (ParseException exc) {
                        try {
                            return longDate.parse(stringToDate);
                        } catch (ParseException exception) {
                            Log.e("Date Error", "Error occurred converting string to date");
                            exception.printStackTrace();
                            return null;
                        }
                    }
                }
            }
        }
    }

    /** Method to convert a string to a date format
     * Parameter: stringToDate - the string you want to convert to a date
     * Returns:   A date object that matches the string*/
    public static Date convertStringToGoogleDate(String stringToDate) {
        try {
            return googleDateFormat.parse(stringToDate);
        } catch (ParseException e) {
            Date temp = convertStringToGoogleDate(stringToDate);
            if (temp == null) {
                Log.e("AppConfig Error", "Could not parse date from string in convertStringToGoogleDate method");
                return null;
            }

            String tempStr = googleDateFormat.format(temp);
            try {
                return googleDateFormat.parse(tempStr);
            } catch (ParseException ex) {
                Log.e("AppConfig Error", "Could not parse date from string in convertStringToGoogleDate method");
                return null;
            }
        }
    }

    /** Method to convert a due date to the customGoogleDateFormat and return it as a string
     * Parameter: dateToString - a Date type variable
     * Returns:   A string format of the date*/
    public static String convertDateToStringFormat(Date dateToString) {
        return googleDateFormat.format(dateToString);
    }
}

