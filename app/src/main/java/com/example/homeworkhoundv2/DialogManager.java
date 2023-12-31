package com.example.homeworkhoundv2;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
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

import androidx.appcompat.app.AlertDialog;

import com.google.api.services.sheets.v4.Sheets;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DialogManager {
    private Context context;
    private AlertDialog alertDialog;
    private AVLTree assignmentTree;
    private Sheets sheetService;
    private AssignmentUpdateListener assignmentUpdateListener;

    private boolean editMode = false;

    public interface AssignmentUpdateListener {
        // Initializing methods as default so classes choose which methods to incorporate)
        default void onAssignmentAdded(Assignment assignment, int position) {
            // Default implementation, no action needed
        }
        default void onAssignmentUpdated(Assignment targetAssignment, Assignment newAssignment, int position){
            // Default implementation, no action needed
        }
        default void onAssignmentDeleted(Assignment assignment, int position){
            // Default implementation, no action needed
        }
    }

    public DialogManager(Context context, AVLTree assignmentTree, Sheets sheetService,
                         AssignmentUpdateListener assignmentUpdateListener) {

        this.context = context;
        this.assignmentTree = assignmentTree;
        this.sheetService = sheetService;
        this.assignmentUpdateListener = assignmentUpdateListener;
    }

    /** Method to display a dialog box for adding, editing, or removing an assignment
     * Parameters:
     *      assignment - an assignment object or null for add and remove operations
     *      position - the assignment's position in the list or totalNumAssignments for adding assignments **/
    @SuppressLint("ClickableViewAccessibility")
    public void AssignmentModificationDialog(Assignment assignment, int position) {

        // Initialize the dialog box builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.add_edit_assignment_dialog, null);
        builder.setView(dialogView);

        // Initialize views
        Spinner courseIDSpinner = dialogView.findViewById(R.id.courseIDSpinner);
        EditText dueDateEditText = dialogView.findViewById(R.id.dueDateEditText);
        EditText assignmentNameEditText = dialogView.findViewById(R.id.assignmentNameEditText);
        Button saveButton = dialogView.findViewById(R.id.saveAssignmentButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteAssignmentButton);

        // If adding an assignment hide the delete button
        if (assignment == null) {
            deleteButton.setEnabled(false);
            deleteButton.setVisibility(View.GONE);
        }

        // Get the current course list from the course list singleton
        List<Course> courseList = CourseListSingleton.getInstance().getCourseList();

        // Initialize the Course ID Spinner
        ArrayAdapter<Course> courseAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, courseList);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseIDSpinner.setAdapter(courseAdapter);

        // Get data from the selected item
        if (assignment != null) {
            assignmentNameEditText.setText(assignment.getAssignmentName());
            dueDateEditText.setText(AppConfig.convertDateToStringFormat(assignment.getDueDate()));
            courseIDSpinner.setSelection(getCourseIDPosition(assignment.getCourseId(), courseList));
            editMode = true;
        }

        // Set onTouch listener for the due date edit text
        dueDateEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                showDatePickerDialog(dueDateEditText);
                return true;
            }
            return false;
        });

        /**** Handle Save Button Click ****/
        saveButton.setOnClickListener(v -> {
            String assignmentNameString = assignmentNameEditText.getText().toString();
            String dueDateString = dueDateEditText.getText().toString();
            String courseIDString = courseIDSpinner.getSelectedItem().toString();
            Assignment temp = new Assignment(assignmentNameString, AppConfig.convertStringToDateFormat(dueDateString), courseIDString);

            // Verify that all fields are filled and the assignmentTree does not already contain the assignment
            if (!dueDateString.equals("") && !assignmentNameString.equals("") && !assignmentTree.contains(temp)) {
                // Prep data to be written to the Google sheet
                List<List<Object>> dataToWrite = new ArrayList<>();
                List<Object> rowData = new ArrayList<>();
                rowData.add(assignmentNameString);
                rowData.add(dueDateString);
                rowData.add(courseIDString);
                dataToWrite.add(rowData);

                int writeOnRow = -1;
                if (editMode) {
                    // Find the row of the item to edit
                    writeOnRow = AppConfig.ASSIGNMENTS_RANGE_START + position;
                }
                else {
                    // Find the next empty row and create the write range for the row
                    writeOnRow = 14 + AppConfig.totalAssignmentsCount;
                }

                // The range I will be writing over
                String writeRange = "A" + writeOnRow + ":C" + writeOnRow;

                if (sheetService != null) {
                    // Write the new assignment to the Google Sheet
                    GoogleSheetWriter.writeDataToSheet(sheetService, writeRange, dataToWrite, assignmentDataWriteListener);

                    // Create the assignment and add it to the assignment list
                    Date dueDate = AppConfig.convertStringToDateFormat(dueDateString);
                    Assignment newAssignment = new Assignment(assignmentNameString, dueDate, courseIDString);

                    if (editMode) {
                        // Pass the current assignment and its updated version to the onAssignmentUpdated listener.
                        // This will update the assignment and notify the adapter of the update.
                        assignmentUpdateListener.onAssignmentUpdated(assignment, newAssignment, position);
                    }
                    else {
                        // Pass the assignment to delete to the onAssignmentDeleted listener.
                        // This will delete the assignment and notify the adapter of the change.
                        assignmentUpdateListener.onAssignmentAdded(newAssignment, position);
                    }

                    // Increment the total number of assignments
                    if (!editMode) {
                        AppConfig.totalAssignmentsCount++;
                    }
                }
                else {
                    Log.e("DialogManager Debug", "Error Writing to Google Sheet");
                    //Toast.makeText(context, "Error Writing to Google Sheet", Toast.LENGTH_SHORT).show();
                }
            }
            else if (dueDateString.equals("")){
                Toast.makeText(context, "Enter a date", Toast.LENGTH_SHORT).show();
            }
            else if (assignmentNameString.equals("")) {
                Toast.makeText(context, "Enter an assignment name", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context, "Assignment has already been added", Toast.LENGTH_SHORT).show();
            }

        }); // End of save button pressed

        // Delete button event handler
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sheetService != null) {
                    // Determine which row to delete
                    int rowToDelete = AppConfig.ASSIGNMENTS_RANGE_START + position;

                    // Perform the delete operation
                    GoogleSheetRowDeleter.deleteRowFromSheet(sheetService, rowToDelete, assignmentDeleteListener);

                    // Notify the adapter that the assignment has been deleted
                    assignmentUpdateListener.onAssignmentDeleted(assignment, position);

                    // Decrement the total number of assignments
                    AppConfig.totalAssignmentsCount--;

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

    // Method to create a mini calendar popup when selecting an edit text
    private void showDatePickerDialog(EditText dateEditText) {
        // Get the current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a DatePickerDialog with the current date as the default selection
        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        // When a date is selected in the calendar dialog, this callback is triggered

                        // Create a Calendar object for the selected date
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, day);

                        // Format the selected date as a string
                        String dateString = AppConfig.convertDateToStringFormat(selectedDate.getTime());

                        // Set the formatted date string in the target EditText
                        dateEditText.setText(dateString);
                    }
                }, year, month, day);

        // Show the dialog box
        datePickerDialog.show();
    }

    private int getCourseIDPosition(String courseID, List<Course> courseList) {
        int i = 0;
        for (Course course : courseList) {
            if (courseID.equals(course.getCourseID())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private GoogleSheetWriter.DataWriteListener assignmentDataWriteListener = new GoogleSheetWriter.DataWriteListener() {
        @Override
        public void onDataWritten() {
            Log.d("Data writer Debug", "Assignment Data write successful in DialogManager");

            // Close the Add Course dialog
            alertDialog.dismiss();
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("Data writer Error", "Error writing assignment data in DialogManager: \n" + errorMessage);
        }
    };

    private GoogleSheetRowDeleter.DataDeleteListener assignmentDeleteListener = new GoogleSheetRowDeleter.DataDeleteListener() {
        @Override
        public void onDataDeleted() {
            Log.d("Data deleter Debug", "Assignment Data deletion successful in DialogManager");

            // Close the Add Course dialog
            alertDialog.dismiss();
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("Data deleter Error", "Error deleting assignment data in DialogManager: \n" + errorMessage);
        }
    };
}
