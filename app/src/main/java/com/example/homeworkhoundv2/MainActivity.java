package com.example.homeworkhoundv2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.api.services.sheets.v4.Sheets;

import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Sheets sheetsService = null;
    private int rowStart = AppConfig.intervalStart;          // The row the assignments start on in the interval
    private int rowEnd = rowStart + AppConfig.LOAD_INTERVAL; // The row the assignments end on int the given interval
    private int nextRowStart;                      // The next starting row of assignments to load
    private int nextRowEnd;                        // The next ending row of assignments to load
    private List<Assignment> assignmentList;
    private AssignmentAdapter assignmentAdapter;
    private List<Course> courseList;
    private CourseAdapter courseAdapter;
    private final String courseRange = "A2:B11";

    private RecyclerView assignmentRecyclerView;
    private Button addCourseButton;
    private Button addAssignmentButton;

    private AlertDialog alertDialog;
    private DialogManager dialogManager;

    private CourseListSingleton courseListSingleton;

    /* Developer Thoughts:
    * 1) This application may use methods that are not well suited for large scale applications. For example,
    *    the Google Sheets implementation uses frequent API requests which could cause major slow downs on
    *    larger scale applications. I designed this application with the idea of creating a personal
    *    homework tracking app that I could use to aid me in remembering and keeping track of when my
    *    assignments are due. That being said A way to make this more commercially viable would be to
    *    use a local database along side the cloud database and only make API requests when opening and closing
    *    the app. For example, when opening the app the local database would be updated with the cloud
    *    databases information. And right before the app is closed the app will update the cloud database.
    *    This allows the assignment information to continue to be cross platform and reduces the load times.
    *    By reducing the number of API requests. This may cause a long initial and final load time though.
    *
    * 2) The Facade:
    *       I am going to use a technique I call the facade to update the assignment list without reading data from the sheet.
    *       Ok so I am going to read data from the Google sheet but only once in the onCreate method.
    *       Then I whenever I delete or update a row I will do the required delete or update requests but,
    *       instead of reading back the change I will simply update the local assignment list to visualize the
    *       change without actually getting the changed data.
    *
    * 3) Loading more assignments:
    *       When I want to load more resources get the current number of assignments loaded and % it by
    *       the LOAD_INTERVAL if the remainder is 0 then all intervals are full so load the next interval.
    * */

    /** IMPORTANT - Just finished adding the delete feature to the assignments next up I need
     * to figure out how I want to do my sorting by date and color **/

    /** Can prob remove the AppConfig.totalAssignmentsCount++ from every where and simply put it in the
     * Google Sheet writer class and the -- in the deleter **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        assignmentRecyclerView = findViewById(R.id.assignmentRecyclerView);
        addCourseButton = findViewById(R.id.addCourseButton);
        addAssignmentButton = findViewById(R.id.addAssignmentButton);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        assignmentRecyclerView.setLayoutManager(layoutManager);

        /******************** Initializing Data ******************************/
        // Initialize the Google Sheets service
        try {
            sheetsService = GoogleSheetServiceInitializer.initialize(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize the CourseListSingleton (Used to globally access course list data)
        courseListSingleton = CourseListSingleton.getInstance();

        // Get the current courseList (If list is empty it is initialized as a new ArrayList<>())
        courseList = courseListSingleton.getCourseList();

        // Initialize the course list
        //courseList = new ArrayList<>();  // If using course list singleton this is not necessary

        // Initialize the Assignment adapter and recycler view
        assignmentList = new ArrayList<>();
        assignmentAdapter = new AssignmentAdapter(this, assignmentList, sheetsService);
        assignmentRecyclerView.setAdapter(assignmentAdapter);

        // Set the initial range to load
        String initialRange = "A" + rowStart + ":C" + rowEnd;

        // Increment the range for the next load interval (this is more of a formality in this method but doing it to set a precedence)
        nextRowStart += AppConfig.LOAD_INTERVAL + 1;  // Example: 14 += 20 + 1 | equals 35 because 34 is the end of previous interval
        nextRowEnd += rowStart + AppConfig.LOAD_INTERVAL;

        // Initialize the course list and assignment list using data from the Google Sheet
        if (sheetsService != null) {
            GoogleSheetReader.readDataFromSheet(sheetsService, courseRange, courseDataReadListener);
            GoogleSheetReader.readDataFromSheet(sheetsService, initialRange, assignmentDataReadListener);

            // The total number of assignments cell
            String totalAssignmentsCell = "B12:B12";
            // Read in the total number of assignments
            GoogleSheetReader.readDataFromSheet(sheetsService, totalAssignmentsCell, totalAssignmentDataReadListener);
        }

        // Initialize the dialogManager (for adding assignments)
        //dialogManager = new DialogManager(this, assignmentList, courseAdapter, sheetsService, assignmentUpdateListener);
        dialogManager = new DialogManager(this, assignmentList, sheetsService, assignmentUpdateListener);

        /******************* On Click Listeners for Buttons *************************/
        // Set a click listener for the Add Course editCourseButton
        addCourseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // When the editCourseButton is clicked, navigate to the AddEditCourseActivity
                Intent intent = new Intent(MainActivity.this, AddEditCourseActivity.class);
                startActivity(intent);
            }
        });

        addAssignmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open an alert dialog box for adding assignments
                //ShowAddAssignmentDialog(null, -1);

                dialogManager.AssignmentModificationDialog(null, AppConfig.totalAssignmentsCount);
            }
        });
    }

    /** IMPORTANT - Add an onActivityResult method to catch when the AddEditCourseActivity ends  *
     *  This will be used to update the local course list                                       **/

    /** POTENTIAL METHODS / Issues to Resolve
    *  SortCourses - Method to Sort the courses by date and color
    *       - This may require me to read the Google sheet first then create a list of assignments
    *       - then sort the list of assignments by date and color (This would refresh for every change of Google Sheet)
    * */

    private Date convertStringToDateFormat(String stringToDate) {
        // String Format: MM/dd/YYYY (Google Sheet view of date)
        try {
            return AppConfig.shortDate.parse(stringToDate);
        }
        catch (ParseException e) {
            try {
                return AppConfig.shortMonthDate.parse(stringToDate);
            }
            catch (ParseException ex) {
                try {
                    return AppConfig.shortDayDate.parse(stringToDate);
                }
                catch (ParseException exc) {
                    try {
                        return AppConfig.longDate.parse(stringToDate);
                    }
                    catch (ParseException exception) {
                        Log.e("Date Error", "Error occurred converting string to date");
                        exception.printStackTrace();
                        return null;
                    }
                }
            }
        }
    }

    private GoogleSheetWriter.DataWriteListener assignmentDataWriteListener = new GoogleSheetWriter.DataWriteListener() {
        @Override
        public void onDataWritten() {
            Log.d("Data writer Debug", "Assignment Data write successful in MainActivity");

            // Close the Add Course dialog
            alertDialog.dismiss();
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("Data writer Error", "Error writing assignment data in MainActivity: \n" + errorMessage);
        }
    };

    private GoogleSheetReader.DataReadListener courseDataReadListener = new GoogleSheetReader.DataReadListener() {

        @Override
        public void onDataRead(List<List<Object>> values) {
            Log.d("Data Reader Debug", "Course Data read successfully in MainActivity");

            // Handle the received data here (update the local courseList)
            if (values != null) {
                //List<Course> updatedCourseList = new ArrayList<>();

                for (List<Object> row : values) {
                    if (!row.isEmpty()) {
                        String courseID = row.get(0).toString();
                        String courseColor = (row.size() > 1) ? row.get(1).toString() : "";
                        Course course = new Course(courseID, courseColor);
                        courseList.add(course);
                    }
                    else {
                        break;
                    }
                }
                courseListSingleton.setCourseList(courseList);

                // Update the local course list
                //courseList.clear();
                //courseList.addAll(updatedCourseList);
            }
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("Data Reader Error", "Error reading course data in MainActivity: \n" + errorMessage);
        }
    };

    private GoogleSheetReader.DataReadListener assignmentDataReadListener = new GoogleSheetReader.DataReadListener() {

        @Override
        public void onDataRead(List<List<Object>> values) {
            Log.d("Data Reader Debug", "Assignment Data read successfully in MainActivity");

            // Use the data received in the Google sheet read to compile an assignment list
            if (values != null) {
                for (List<Object> row : values) {
                    if (!row.isEmpty()) {
                        String assignmentName = row.get(0).toString();
                        Date dueDate = convertStringToDateFormat(row.get(1).toString());
                        String courseID = row.get(2).toString();
                        Assignment assignment = new Assignment(assignmentName, dueDate, courseID);
                        assignmentList.add(assignment);
                    }
                    else {
                        break;
                    }
                }

                // Notify the assignment adapter of the read data
                assignmentAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("Data Reader Error", "Error reading assignment data in MainActivity: \n" + errorMessage);
        }
    };

    private GoogleSheetReader.DataReadListener totalAssignmentDataReadListener = new GoogleSheetReader.DataReadListener() {
        @Override
        public void onDataRead(List<List<Object>> values) {
            Log.d("Data Reader Debug", "Total Assignment Data read successfully in MainActivity");

            if (values != null) {
                List<Object> row = values.get(0);
                AppConfig.totalAssignmentsCount = Integer.parseInt(row.get(0).toString());
            }
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("Data Reader Error", "Error reading total assignments data in MainActivity: \n" + errorMessage);
        }
    };

    private DialogManager.AssignmentUpdateListener assignmentUpdateListener = new DialogManager.AssignmentUpdateListener() {
        @Override
        public void onAssignmentAdded(Assignment assignment, int position) {
            // Update the local assignmentList
            assignmentList.add(assignment);

            // Sort the the list
            sortAssignmentsByDueDate();

            // Refresh the UI
            assignmentAdapter.notifyDataSetChanged();
        }
    };

    public void sortAssignmentsByDueDate() {
        assignmentList.sort(new Comparator<Assignment>() {
            @Override
            public int compare(Assignment assignment1, Assignment assignment2) {
                // Compare assignments by due date
                return assignment1.getDueDate().compareTo(assignment2.getDueDate());
            }
        });
    }
}