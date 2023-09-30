package com.example.homeworkhoundv2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.example.homeworkhoundv2.GoogleSheetReader;
import com.example.homeworkhoundv2.GoogleSheetWriter;

public class AddEditCourseActivity extends AppCompatActivity {
    private EditText courseIDEditText;
    private Spinner colorCodeSpinner;
    private Button addCourseButton;
    private ListView courseListView;
    private List<Course> courseList;
    private ArrayAdapter<CharSequence> colorSpinnerAdapter;
    private CourseAdapter adapter;

    private CourseListSingleton courseListSingleton;

    private String courseRange  = "A2:B11";                  // Range of acceptable cells to add a course (10 course max)
    private Sheets sheetsService = null;

    /*
    *  Ideas:
    * 1) Add a refresh courseDeleteButton in top right of the title bar thing to refresh the course list
    * */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_course);

        courseIDEditText = findViewById(R.id.courseIDEditText);
        colorCodeSpinner = findViewById(R.id.colorCodeSpinner);
        addCourseButton = findViewById(R.id.addCourseButton);
        courseListView = findViewById(R.id.courseListView);

        /** Initializations **/
        // Initialize the Google Sheets service
        try {
            sheetsService = GoogleSheetServiceInitializer.initialize(this);
        } catch (IOException e) {
            e.printStackTrace();
            // Handle initialization error
        }

        // Initialize the CourseListSingleton (Used to globally access course list data)
        courseListSingleton = CourseListSingleton.getInstance();

        // Initialize the course list and custom adapter
        courseList = new ArrayList<>();
        adapter = new CourseAdapter(this, R.layout.item_course, courseList, sheetsService); // Use the custom adapter
        courseListView.setAdapter(adapter);

        // Initialize the color courseIDSpinner
        colorSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.Colors, android.R.layout.simple_spinner_item);
        colorSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorCodeSpinner.setAdapter(colorSpinnerAdapter);

        // Read the course list from the Google sheet
        UpdateCourseList();

        /** Listeners **/
        // Add a course to the list
        Sheets finalSheetsService = sheetsService;
        findViewById(R.id.addCourseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String courseID = courseIDEditText.getText().toString();
                String courseColor = getColorNameFromSelection(colorCodeSpinner.getSelectedItemPosition());
                Course course = new Course(courseID, courseColor);

                // Add the course to the local course list
                courseList.add(course);
                adapter.notifyDataSetChanged(); // Notify the custom adapter
                clearEditTextFields();

                // Update the CourseListSingleton to make the local course list globally accessible
                courseListSingleton.setCourseList(courseList);

                // Prep data to be written to the Google sheet
                List<List<Object>> dataToWrite = new ArrayList<>();
                List<Object> rowData = new ArrayList<>();
                rowData.add(courseID);
                rowData.add(courseColor);
                dataToWrite.add(rowData);

                // Find the next empty row and create the write range for the row
                int nextEmptyRow = courseList.size() + 1;
                String writeRange = "A" + nextEmptyRow + ":B" + nextEmptyRow;

                // Write the data to the Google sheet
                if (finalSheetsService != null && nextEmptyRow <= 11) {
                    GoogleSheetWriter.writeDataToSheet(finalSheetsService, writeRange, dataToWrite, dataWriteListener);
                }
                else if (nextEmptyRow > 11) {
                    Toast.makeText(AddEditCourseActivity.this, "You can only have 10 courses saved at time.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void UpdateCourseList() {
        if (sheetsService != null) {
            GoogleSheetReader.readDataFromSheet(sheetsService, courseRange, dataReadListener);
        }
    }

    private void clearEditTextFields() {
        courseIDEditText.setText("");
        colorCodeSpinner.setSelection(0); // Reset the courseIDSpinner to the first item
    }

    private String getColorNameFromSelection(int position) {
        // Get the color name from the selected position
        String[] colorNames = getResources().getStringArray(R.array.Colors);
        if (position >= 0 && position < colorNames.length) {
            return colorNames[position];
        }
        return "No Color Found";
    }

    /* An instance of my GoogleSheetReader
    * This allows me to customize the Google sheet reader for the courses (and eventually assignments) without creating redundant code
    * */
    private GoogleSheetReader.DataReadListener dataReadListener = new GoogleSheetReader.DataReadListener() {
        @Override
        public void onDataRead(List<List<Object>> values) {
            // Handle the received data here (update the local courseList)
            if (values != null) {
                for (List<Object> row : values) {
                    if (!row.isEmpty()) {
                        String courseID = row.get(0).toString();
                        String courseColor = (row.size() > 1) ? row.get(1).toString() : "";
                        Course course = new Course(courseID, courseColor);
                        courseList.add(course);
                    }
                }
                // Notify the adapter that the data has changed
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onError(String errorMessage) {
            // Handle the error (e.g., show an error message)
            Log.e("DataReadListener", errorMessage);
        }
    };

    private GoogleSheetWriter.DataWriteListener dataWriteListener = new GoogleSheetWriter.DataWriteListener() {
        @Override
        public void onDataWritten() {
            // Handle the data write success
            Log.d("DataWriteListener", "Data written successfully");
        }

        @Override
        public void onError(String errorMessage) {
            // Handle the error
            Log.e("DataWriteListener", errorMessage);
        }
    };
}

