package com.example.homeworkhoundv2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.api.services.sheets.v4.Sheets;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Sheets sheetsService = null;
    //TODO: Will Probs remove the rowStart and rowEnd as global variables and make them local in the onCreate or something
    private int rowStart = AppConfig.INTERVAL_START;              // The row the assignments start on in the interval
    private int rowEnd = rowStart + AppConfig.LOAD_INTERVAL - 1; // The row the assignments end on int the given interval (-1 because rowStart is inclusive)
    private int nextRowStart;                      // The next starting row of assignments to load
    private int nextRowEnd;                        // The next ending row of assignments to load
    private boolean loadingMore = false;                         // Is the program currently loading more assignments
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
    * 3.1) Loading more assignments:
    *       When I want to load more resources get the current number of assignments loaded and % it by
    *       the LOAD_INTERVAL if the remainder is 0 then all intervals are full so load the next interval.
    * 3.2) Removing discordantly loaded assignments:
    *       Implement some code to remove intervals of assignments from the currently loaded assignment list.
    *       For example if the current interval is 5 then unload the all intervals except intervals 4,5,6.
    * */

    //TODO: Make these modifications if possible
    /** Can prob remove the AppConfig.totalAssignmentsCount++ from every where and simply put it in the
     * Google Sheet writer class and the -- in the deleter **/

    /** When I return work on adding the load more button, then the above green text, then UI beautification **/

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

        // Initialize the Assignment adapter and recycler view
        assignmentList = new ArrayList<>();
        assignmentAdapter = new AssignmentAdapter(this, assignmentList, sheetsService);
        assignmentRecyclerView.setAdapter(assignmentAdapter);

        // Set the initial range to load
        String initialRange = "A" + rowStart + ":C" + rowEnd;

        /** The bellow code is probably not needed **/
        // Increment the range for the next load interval (this is more of a formality in this method but doing it to set a precedence)
        //nextRowStart = AppConfig.LOAD_INTERVAL + 1;  // Example: 14 += 20 + 1 | equals 35 because 34 is the end of previous interval
        //nextRowEnd += rowStart + AppConfig.LOAD_INTERVAL;

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

        /** IDEA - I can probs find the last visiable item position and if it is less than the start interval
         * of the next highest interval after the current interval then I can remove 20 assignments from
         * the end of the assignment list **/
        // Set a scroll listener to detect when the user reaches the end of the RecyclerView
        assignmentRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Check if it's not already loading more and the user is at the end of the list
                if (AppConfig.intervalAtCapacity && !loadingMore && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount) {
                    loadingMore = true;

                    // Load more assignments here
                    loadMoreAssignments();
                    //Log.d("Debug Log", "Inside loading more area ************************" + totalItemCount);
                }
            }
        });
    }

    private void loadMoreAssignments() {
        // You can implement your logic to load more assignments here.
        // For example, update the 'rowStart' and 'rowEnd' variables to load the next batch of assignments
        // and then fetch data from the Google Sheet as you did in the initial load.

        // Calculate the current interval based on the number of assignments loaded
        int assignmentsLoaded = assignmentList.size();
        int currentInterval = (assignmentsLoaded / (AppConfig.LOAD_INTERVAL + 1)) + 1;

        //Log.d("Debug Log", "**** AssignmentsLoaded: " + assignmentsLoaded + " - CurrentInterval: " + currentInterval);

        // Calculate the start and end positions for the next interval
        int intervalStart = AppConfig.INTERVAL_START + currentInterval * AppConfig.LOAD_INTERVAL;
        int intervalEnd = intervalStart + AppConfig.LOAD_INTERVAL - 1;
        String intervalRange = "A" + intervalStart + ":C" + intervalEnd;

        //Log.d("Debug Log", "****** start: " + intervalStart + " - End: " + intervalEnd);

        // Load the new assignment data from the Google Sheet
        if (sheetsService != null) {
            GoogleSheetReader.readDataFromSheet(sheetsService, intervalRange, assignmentDataReadListener);
        }
        else {
            Log.d("Debug Log", "Error loading additional assignments in Main Activity");
        }
    }

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

                // Increment the most recently loaded interval
                AppConfig.latestIntervalLoaded++;

                // Notify the assignment adapter of the read data
                assignmentAdapter.notifyDataSetChanged();

                Log.d("Debug Log", "Num assignments loaded: " + assignmentList.size());

                // Check if the interval is currently at max capacity
                if (assignmentList.size() == AppConfig.latestIntervalLoaded * AppConfig.LOAD_INTERVAL) {
                    AppConfig.intervalAtCapacity = true;
                }

                // Set loadingMore to false after loading is complete
                loadingMore = false;
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