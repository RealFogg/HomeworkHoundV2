package com.example.homeworkhoundv2;

import android.os.AsyncTask;
import android.util.Log;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SortRangeRequest;
import com.google.api.services.sheets.v4.model.SortSpec;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GoogleSheetWriter {

    // Interface for handling data write callbacks
    public interface DataWriteListener {
        void onDataWritten();

        void onError(String errorMessage);
    }

    // Method to write data to the Google Sheet
    public static void writeDataToSheet(Sheets sheetsService, String range, List<List<Object>> data, DataWriteListener listener) {
        new WriteSheetDataTask(sheetsService, range, data, listener).execute();
    }

    // AsyncTask for writing data to the Google Sheet
    private static class WriteSheetDataTask extends AsyncTask<Void, Void, Void> {
        private Sheets sheetsService;
        private String range;
        private List<List<Object>> data;
        private DataWriteListener listener;

        public WriteSheetDataTask(Sheets sheetsService, String range, List<List<Object>> data, DataWriteListener listener) {
            this.sheetsService = sheetsService;
            this.range = range;
            this.data = data;
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Create a ValueRange object to hold the data to be written
                ValueRange valueRange = new ValueRange();
                valueRange.setValues(data);

                // Make the API request to write data
                sheetsService.spreadsheets().values()
                        .update(AppConfig.SPREADSHEET_ID, range, valueRange)
                        .setValueInputOption("RAW")
                        .execute();

                // Sort the sheet by due date
                sortSheetByDueDate();

            } catch (IOException e) {
                Log.e("WriteSheetDataTask", "Error: " + e.getMessage());
            }
            return null;
        }

        private void sortSheetByDueDate() {
            try {
                // Define the sorting criteria
                SortSpec sortSpec = new SortSpec()
                        .setDimensionIndex(1) // Sort by the second column
                        .setSortOrder("ASCENDING"); // Sort in ascending order

                // Create a GridRange for the range you want to sort
                GridRange gridRange = new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(13)
                        .setEndRowIndex(13 + AppConfig.totalAssignmentsCount)
                        .setStartColumnIndex(0)    // Start column (0 based)
                        .setEndColumnIndex(3);     // End column +1

                Log.d("SortSheetByDueDate", "Sorting range: " + gridRange);

                // Create a Request to sort the GridRange
                Request sortRequest = new Request()
                        .setSortRange(new SortRangeRequest()
                                .setRange(gridRange)
                                .setSortSpecs(Collections.singletonList(sortSpec)));

                // Create a batchUpdate request to apply the sort
                BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                        .setRequests(Collections.singletonList(sortRequest));

                // Execute the batchUpdate request to sort the sheet
                sheetsService.spreadsheets().batchUpdate(AppConfig.SPREADSHEET_ID, batchUpdateRequest).execute();
            } catch (IOException e) {
                Log.e("SortSheetByDueDate", "Error: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (listener != null) {
                listener.onDataWritten();
            }
        }
    }
}

