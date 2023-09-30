package com.example.homeworkhoundv2;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;


import com.google.api.services.sheets.v4.Sheets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CourseAdapter extends ArrayAdapter<Course> {
    private Context context;
    private int resource;
    private AlertDialog alertDialog;

    private Sheets sheetService;
    private String courseRange = "A2:B11";
    private List<Course> courseList;

    private CourseListSingleton courseListSingleton;

    public CourseAdapter(Context context, int resource, List<Course> courses, Sheets sheetService) {
        super(context, resource, courses);
        this.context = context;
        this.resource = resource;
        this.courseList = courses;
        this.sheetService = sheetService;
        this.courseListSingleton = CourseListSingleton.getInstance();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resource, parent, false);
        }

        Course course = getItem(position);

        if (course != null) {
            TextView courseIDTextView = convertView.findViewById(R.id.courseIDTextView);
            TextView colorTextView = convertView.findViewById(R.id.colorTextView);

            if (courseIDTextView != null) {
                courseIDTextView.setText(course.getCourseID());
            }

            if (colorTextView != null) {
                colorTextView.setText(course.getCourseColor());
            }
        }

        Button editCourseButton = convertView.findViewById(R.id.editCourseButton);
        editCourseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle the edit action here for the course at 'position'
                showEditDialog(position);
            }
        });

        return convertView;
    }

    private void showEditDialog(final int position) {
        // Create an AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.edit_course_dialog, null);
        builder.setView(dialogView);

        final EditText editCourseIDEditText = dialogView.findViewById(R.id.editCourseIDEditText);
        final Spinner editColorCodeSpinner = dialogView.findViewById(R.id.editColorCodeSpinner);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button deleteButton = dialogView.findViewById(R.id.courseDeleteButton);

        // Set the initial values from the selected course
        Course course = getItem(position);
        editCourseIDEditText.setText(course.getCourseID());

        // Populate the courseIDSpinner with color options
        ArrayAdapter<CharSequence> colorSpinnerAdapter = ArrayAdapter.createFromResource(context.getApplicationContext(), R.array.Colors, android.R.layout.simple_spinner_item);
        colorSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editColorCodeSpinner.setAdapter(colorSpinnerAdapter);

        // Set initial value for the color courseIDSpinner
        editColorCodeSpinner.setSelection(getColorPosition(course.getCourseColor()));

        // Handle the "Save" course saveAssignmentButton click
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Retrieve edited values
                String editedCourseID = editCourseIDEditText.getText().toString();
                String editedColor = editColorCodeSpinner.getSelectedItem().toString();

                // Update the course data in the list
                Course editedCourse = new Course(editedCourseID, editedColor);
                courseList.set(position, editedCourse);

                // Update the course list singleton of the new course list
                courseListSingleton.setCourseList(courseList);

                // Prep data to be written to the Google sheet
                List<List<Object>> dataToWrite = new ArrayList<>();
                List<Object> rowData = new ArrayList<>();
                rowData.add(editedCourseID);
                rowData.add(editedColor);
                dataToWrite.add(rowData);

                // Determine the row (range) in the sheet to update
                int rowToUpdate = position + 2;  // Position is the pos in the courseIDSpinner so +1 for being 0 based and +1 because row one is a header(excluded from range)
                String rangeToUpdate = "A" + rowToUpdate + ":B" + rowToUpdate;

                // Update the Google sheet with the new info
                if (sheetService != null) {
                    // Disable the save saveAssignmentButton to prevent multiple clicks
                    saveButton.setEnabled(false);

                    // Perform update operation
                    GoogleSheetWriter.writeDataToSheet(sheetService, rangeToUpdate, dataToWrite, dataWriteListener);
                }

                // Dismiss the dialog
                //alertDialog.dismiss();
            }
        });

        // Handle the "Delete" course saveAssignmentButton click
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
                if (sheetService != null) {
                    // Disable the delete saveAssignmentButton to prevent multiple clicks
                    deleteButton.setEnabled(false);

                    // Remove the course from the courseList
                    courseList.remove(course);

                    // Update the course list singleton of the new course list
                    courseListSingleton.setCourseList(courseList);

                    // Position of the course in the Google Sheet
                    int GSPosition = position + 2; // Position is the pos in the courseIDSpinner so +1 for being 0 based and +1 because row one is a header(excluded from range)
                    int GSSize = courseList.size() + 1;   // +1 for the header?

                    // Perform the delete operation
                    //GoogleSheetRowDeleter.deleteRowFromSheet(sheetService, course.getCourseID(), courseRange, dataDeleteListener);
                    GoogleSheetRowDeleter.deleteRowFromSheet(sheetService, GSPosition, GSSize, dataDeleteListener);
                }
                else {
                    Toast.makeText(context, "Not working", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Create and show the dialog
        alertDialog = builder.create();
        alertDialog.show();
    }

    private int getColorPosition(String colorCode) {
        switch (colorCode) {
            case "red":
                return 0;
            case "blue":
                return 1;
            case "green":
                return 2;
            case "orange":
                return 3;
            case "pink":
                return 4;
            default:        // default is the case for purple
                return 5;
        }
    }

    private void updateCourseList(List<Course> updatedCourseList) {
        // Update the course list and refresh the adapter
        this.courseList.clear();
        this.courseList.addAll(updatedCourseList);
        notifyDataSetChanged();
    }

    public List<Course> getCourseList() {
        return courseList;
    }

    private GoogleSheetRowDeleter.DataDeleteListener dataDeleteListener = new GoogleSheetRowDeleter.DataDeleteListener() {
        @Override
        public void onDataDeleted() {
            Log.d("DataDeleter", "Data deleted successfully");

            // Dismiss the dialog box
            alertDialog.dismiss();

            // Refresh the courseList with the new changes (Change this later when I change the onDataDeleted interface)
            //GoogleSheetReader.readDataFromSheet(sheetService, courseRange, dataReadListener);
            notifyDataSetChanged();
        }

        @Override
        public void onError(String errorMessage) {
            // Handle the error (e.g., show an error message)
            Log.e("DataDeleteListener", errorMessage);
        }
    };

    private GoogleSheetWriter.DataWriteListener dataWriteListener = new GoogleSheetWriter.DataWriteListener() {
        @Override
        public void onDataWritten() {
            Log.d("DataWriter", "Data Updated Successfully");

            //Dismiss the dialog
            alertDialog.dismiss();

            // Refresh the courseList with the new changes (Change this later when I change the onDataWritten interface)
            //GoogleSheetReader.readDataFromSheet(sheetService, courseRange, dataReadListener);
            notifyDataSetChanged();
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("DataWriter", "Data failed to Update: \n" + errorMessage);
        }
    };
}
