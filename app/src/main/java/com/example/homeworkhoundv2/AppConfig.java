package com.example.homeworkhoundv2;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AppConfig {
    // Google Sheet ID
    public static final String SPREADSHEET_ID = "1fy9jX-jukiP6qxl4NEmKUoXMG1ZHj5oeWeICndMoAAA";

    // DateFormats:
    public static final SimpleDateFormat googleDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    public static final SimpleDateFormat longDate = new SimpleDateFormat("MM/dd/yy", Locale.US);
    public static final SimpleDateFormat shortMonthDate = new SimpleDateFormat("M/dd/yy", Locale.US);
    public static final SimpleDateFormat shortDayDate = new SimpleDateFormat("MM/d/yy", Locale.US);
    public static final SimpleDateFormat shortDate = new SimpleDateFormat("M/d/yy", Locale.US);

    // Static non-final variables
    public static int totalAssignmentsCount = 0;

    // Variables for keeping track of loaded items / intervals
    public static final int LOAD_INTERVAL = 20;          // The number of items loaded in each interval
    public static int latestIntervalLoaded = 1;          // The most recently loaded interval
    public static final int intervalStart = 14;          // The starting row for the assignments table
}

