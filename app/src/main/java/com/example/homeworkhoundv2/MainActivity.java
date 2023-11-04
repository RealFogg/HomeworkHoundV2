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
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private Sheets sheetsService = null;
    private AVLTree assignmentTree;
    private AssignmentAdapter assignmentAdapter;
    private List<Course> courseList;
    private final String courseRange = "A2:B11";

    private RecyclerView assignmentRecyclerView;
    private Button addCourseButton;
    private Button addAssignmentButton;
    private DialogManager dialogManager;

    private CourseListSingleton courseListSingleton;

    // TODO: When I left off I fixed up how the assignment data was sorted so now the data should be properly
    // synced up with the google sheet. I also removed a few old methods and comments that where no longer needed.
    // I think the next step is to continue removing old and redundant code, then move on to cleaning up and
    // finalizing the UI.

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
        assignmentTree = new AVLTree();
        assignmentAdapter = new AssignmentAdapter(this, assignmentTree, sheetsService);
        assignmentRecyclerView.setAdapter(assignmentAdapter);

        // Initialize the course list and assignment list using data from the Google Sheet
        if (sheetsService != null) {
            // The total number of assignments cell
            String totalAssignmentsCell = "B12:B12";
            // Read in the total number of assignments ( In this implementation I also read in the assignment data
            // during the this method). It is a bit of a strange way to do it but it means I don't have to pause the UI thread
            GoogleSheetReader.readDataFromSheet(sheetsService, totalAssignmentsCell, totalAssignmentDataReadListener);

            GoogleSheetReader.readDataFromSheet(sheetsService, courseRange, courseDataReadListener);
        }

        // Initialize the dialogManager (for adding assignments)
        dialogManager = new DialogManager(this, assignmentTree, sheetsService, assignmentUpdateListener);

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
                dialogManager.AssignmentModificationDialog(null, AppConfig.totalAssignmentsCount);
            }
        });
    }

    // Debug method for testing heavy loads of assignments
    public void generateRandomAssignments(int count) {
        //List<Assignment> assignments = new ArrayList<>();
        Random random = new Random();

        for (int i = 1; i <= count; i++) {
            String assignmentName = "Assignment " + i;
            Date dueDate = generateRandomDueDate();
            String courseId = "Course " + (random.nextInt(5) + 1); // Random course ID

            Assignment assignment = new Assignment(assignmentName, dueDate, courseId);

            assignmentTree.insert(assignment);

            //assignments.add(assignment);
        }

        //return assignments;
    }
    // Helper method for generateRandomAssignments
    private Date generateRandomDueDate() {
        long now = System.currentTimeMillis();
        long maxDate = now + (365 * 24 * 60 * 60 * 1000); // Up to one year in the future
        long randomTime = now + (long) (Math.random() * (maxDate - now));
        return new Date(randomTime);
    }

    private GoogleSheetReader.DataReadListener courseDataReadListener = new GoogleSheetReader.DataReadListener() {

        @Override
        public void onDataRead(List<List<Object>> values) {
            Log.d("Data Reader Debug", "Course Data read successfully in MainActivity");

            // Handle the received data here (update the local assignmentTree)
            if (values != null) {
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
                        Date dueDate = AppConfig.convertStringToDateFormat(row.get(1).toString());
                        String courseID = row.get(2).toString();
                        Assignment assignment = new Assignment(assignmentName, dueDate, courseID);

                        // Add the assignment to the AVLTree
                        assignmentTree.insert(assignment);
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
                Log.d("Main Activity Debug", "Total Assignment Count: " + AppConfig.totalAssignmentsCount);

                // Retrieve all the existing assignments from the Google Sheet
                int rowStart = AppConfig.ASSIGNMENTS_RANGE_START;              // The row the assignments start on
                int rowEnd = rowStart + AppConfig.totalAssignmentsCount;  // The row the assignments end on
                int endRow = rowStart + AppConfig.totalAssignmentsCount;
                String initialRange = "A" + rowStart + ":C" + endRow;

                GoogleSheetReader.readDataFromSheet(sheetsService, initialRange, assignmentDataReadListener);
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
            // Update the local assignmentTree
            assignmentTree.insert(assignment);

            // Refresh the UI
            assignmentAdapter.notifyDataSetChanged();
        }
    };
}