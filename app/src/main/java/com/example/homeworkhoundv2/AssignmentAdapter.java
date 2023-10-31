package com.example.homeworkhoundv2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeworkhoundv2.Assignment;
import com.google.api.services.sheets.v4.Sheets;

import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {
    private AVLTree assignmentTree;
    //private List<Assignment> assignmentList;
    private Context context;
    private Sheets sheetService;
    //private GoogleSheetWriter.DataWriteListener assignmentDataWriteListener;

    private DialogManager dialogManager;

    // Constructor to initialize the adapter with data
    public AssignmentAdapter(Context context, AVLTree assignmentTree/*List<Assignment> assignmentList*/, Sheets sheetService) {
        this.context = context;
        this.assignmentTree = assignmentTree;
        //this.assignmentList = assignmentList;
        this.sheetService = sheetService;

        // Initialize the DialogManager
        dialogManager = new DialogManager(context, assignmentTree, sheetService,
                new DialogManager.AssignmentUpdateListener() {
                    @Override
                    public void onAssignmentUpdated(Assignment targetAssignment, Assignment newAssignment, int position) {
                        // Handle the update of an existing assignment
                        assignmentTree.updateAssignment(targetAssignment, newAssignment);
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onAssignmentDeleted(Assignment assignment, int position) {
                        // Remove the assignment from the local assignmentList
                        assignmentTree.delete(assignment);

                        // Notify the adapter about the data change
                        notifyItemRemoved(position);

                        // Optionally, could update the Google Sheet here
                    }
                });
    }

    // ViewHolder class to hold the UI elements
    public static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        TextView assignmentNameTextView;
        TextView dueDateTextView;
        TextView courseIdTextView;
        View colorSquareView;

        public AssignmentViewHolder(View itemView) {
            super(itemView);
            assignmentNameTextView = itemView.findViewById(R.id.assignmentNameTextView);
            dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
            courseIdTextView = itemView.findViewById(R.id.courseIdTextView);
            colorSquareView = itemView.findViewById(R.id.colorSquare);
        }
    }

    @Override
    public AssignmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the item layout and create a new ViewHolder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AssignmentViewHolder holder, int position) {
        // Bind data to the UI elements of the ViewHolder

        // This works but may be inefficient (might look for new method in future versions).
        Assignment assignment = assignmentTree.getAssignmentAtPosition(position);

        holder.assignmentNameTextView.setText(assignment.getAssignmentName());
        holder.courseIdTextView.setText(assignment.getCourseId());

        // Get the Current course list
        List<Course> courseList = CourseListSingleton.getInstance().getCourseList();

        if (!courseList.isEmpty()) {
            for (Course course : courseList) {
                if (course.getCourseID().equals(assignment.getCourseId())) {
                    // Set the background color of the color square view
                    int colorResource = getColorPosition(course.getCourseColor());
                    int colorValue = ContextCompat.getColor(context, colorResource);
                    holder.colorSquareView.setBackgroundColor(colorValue);
                }
            }
        }

        // Format the Date to a human-readable string
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        String dueDateString = dateFormat.format(assignment.getDueDate());

        holder.dueDateTextView.setText(dueDateString);

        // Debug statements: can remove later
        //Log.d("AssignmentAdapter", "Binding assignment at position " + position);
        //Log.d("AssignmentAdapter", "Assignment name: " + assignment.getAssignmentName());
        //Log.d("AssignmentAdapter", "Course ID: " + assignment.getCourseId());
        //Log.d("AssignmentAdapter", "Due Date: " + assignment.getDueDate());

        /** OnClickListener - Allow users to click on assignment items from the MainActivity Recycler view **/
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Debug - Show the course details
                //Log.d("AssignmentAdapterOC", "Binding assignment at position " + holder.getAdapterPosition());
                //Log.d("AssignmentAdapterOC", "Assignment name: " + assignment.getAssignmentName());
                //Log.d("AssignmentAdapterOC", "Course ID: " + assignment.getCourseId());
                //Log.d("AssignmentAdapterOC", "Due Date: " + assignment.getDueDate());

                // Debug THIS WORKS But doesn't fix the issue at the start of the onBindViewHolder ********************
                //String assignmentNameStr = holder.assignmentNameTextView.getText().toString();
                //String dueDateStr = holder.dueDateTextView.getText().toString();
                //String courseIDStr = holder.courseIdTextView.getText().toString();
                //Date holderDueDate = AppConfig.convertStringToDateFormat(dueDateStr);
                //Assignment holderAssignment = new Assignment(assignmentNameStr, holderDueDate, courseIDStr);
                //Log.d("AssAdap Debug", "Holder assignmentNameStr: " + assignmentNameStr);
                //Log.d("AssAdap Debug", "Holder dueDateStr: " + dueDateStr);
                //Log.d("AssAdap Debug", "Holder courseIDStr: " + courseIDStr);

                // Set the background to a darker color to show the click
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.darkBackground));

                // Start a delayed method that will change the background color back to white
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Set background color back to white
                        holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
                    }
                }, 150);

                // Show the edit assignment dialog using the DialogManager
                dialogManager.AssignmentModificationDialog(assignment, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return assignmentTree.getTotalAssignments();
    }

    private int getColorPosition(String colorCode) {
        switch (colorCode) {
            case "red":
                return R.color.red;
            case "blue":
                return R.color.blue;
            case "green":
                return R.color.green;
            case "orange":
                return R.color.orange;
            case "pink":
                return R.color.pink;
            default:        // default is the case for purple
                return R.color.purple;
        }
    }
}
