package com.example.homeworkhoundv2;

import android.os.AsyncTask;
import android.util.Log;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GoogleSheetRowDeleter {

    // Interface for handling data deletion callbacks (In this case am using to help write Logs)
    public interface DataDeleteListener {
        void onDataDeleted();

        void onError(String errorMessage);
    }

    /** Method to delete a row from the google sheet
     * Parameters:
     *      Sheets sheetService,
     *      int currentRow - the row the item to delete is on,
     *      int sizeOfRange - the number of items currently active,
     *      DataDeleteListener listener - interface to support data deletion**/
    public static void deleteRowFromSheet(Sheets sheetService, int currentRow, int sizeOfRange, DataDeleteListener listener) {
        new DeleteRowFromSheetTask(sheetService, currentRow, sizeOfRange, listener).execute();
    }

    /** Method to delete a row from the google sheet where the size of the range does not matter
     * Parameters:
     *      Sheets sheetService,
     *      int currentRow - the row the item to delete is on,
     *      DataDeleteListener listener - interface to support data deletion**/
    public static void deleteRowFromSheet(Sheets sheetService, int currentRow, DataDeleteListener listener) {
        new DeleteRowFromSheetTask(sheetService, currentRow, listener).execute();
    }

    // AsyncTask for deleting data from the Google sheet
    private static class DeleteRowFromSheetTask extends AsyncTask<Void, Void, Void> {
        private Sheets sheetService;
        private int currentRow;
        private int sizeOfRange;
        private String uniqueID;
        private String range;
        private DataDeleteListener listener;

        /** Method to delete a row from the google sheet
         * Parameters:
         *      Sheets sheetService,
         *      int currentRow - the row the item to delete is on,
         *      int sizeOfRange - the number of items currently active,
         *      DataDeleteListener listener - interface to support data deletion**/
        public DeleteRowFromSheetTask(Sheets sheetService, int currentRow, int sizeOfRange, DataDeleteListener listener) {
            this.sheetService = sheetService;
            this.currentRow = currentRow;
            this.sizeOfRange = sizeOfRange;
            this.listener = listener;
        }

        /** Method to delete a row from the google sheet where the size of the range does not matter
         * Parameters:
         *      Sheets sheetService,
         *      int currentRow - the row the item to delete is on,
         *      DataDeleteListener listener - interface to support data deletion**/
        public DeleteRowFromSheetTask(Sheets sheetService, int currentRow, DataDeleteListener listener) {
            this.sheetService = sheetService;
            this.currentRow = currentRow;
            this.sizeOfRange = -1;
            this.listener = listener;
        }

        // This one should work well with the courses
        private void deleteRow() {
            try {
                // Create a delete request for the entire row
                DeleteDimensionRequest deleteRequest = new DeleteDimensionRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(0) // The first sheet
                                .setDimension("ROWS")
                                .setStartIndex(currentRow - 1) // the row the current item is on
                                .setEndIndex(currentRow)); // start of next row (Deletes the entire current row)

                // Create a request to batch delete
                Request deleteRequestObject = new Request().setDeleteDimension(deleteRequest);

                // Add the delete requests to a list
                List<Request> requests = new ArrayList<>();
                requests.add(deleteRequestObject);

                // If size of range matters insert the a new row at the end of the last item in the range
                if (sizeOfRange > 0) {
                    // Create an insert request to add a new row
                    InsertDimensionRequest insertRequest = new InsertDimensionRequest()
                            .setRange(new DimensionRange()
                                    .setSheetId(0) // Assuming the first sheet
                                    .setDimension("ROWS")
                                    .setStartIndex(sizeOfRange) // Insert after the last row in the range
                                    .setEndIndex(sizeOfRange + 1)); // Insert a single row

                    // Create a request to batch delete and insert
                    Request insertRequestObject = new Request().setInsertDimension(insertRequest);

                    // Add the insert requests to the request list
                    requests.add(insertRequestObject);
                }

                // Execute the batch update to delete the row and insert a new row
                BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                        .setRequests(requests);

                // Execute the batch update to delete the row and insert a new row
                sheetService.spreadsheets().batchUpdate(AppConfig.SPREADSHEET_ID, batchUpdateRequest)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("Debug Log", "Inside doInBackground for GoogleSheetRowDeleter");

            deleteRow();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (listener != null) {
                listener.onDataDeleted();
            }
        }
    }
}
