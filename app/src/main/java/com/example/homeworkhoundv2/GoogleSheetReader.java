package com.example.homeworkhoundv2;

import android.os.AsyncTask;
import android.util.Log;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.List;

public class GoogleSheetReader {

    //private static final String SPREADSHEET_ID = "1fy9jX-jukiP6qxl4NEmKUoXMG1ZHj5oeWeICndMoAAA";

    // Interface for handling data read callbacks
    public interface DataReadListener {
        void onDataRead(List<List<Object>> values);

        void onError(String errorMessage);
    }

    // Method to read data from the Google Sheet
    public static void readDataFromSheet(Sheets sheetsService, String range, DataReadListener listener) {
        new ReadSheetDataTask(sheetsService, range, listener).execute();
    }

    // AsyncTask for reading data from the Google Sheet
    private static class ReadSheetDataTask extends AsyncTask<Void, Void, List<List<Object>>> {
        private Sheets sheetsService;
        private String range;
        private DataReadListener listener;

        public ReadSheetDataTask(Sheets sheetsService, String range, DataReadListener listener) {
            this.sheetsService = sheetsService;
            this.range = range;
            this.listener = listener;
        }

        // This gets data from the Google sheet
        @Override
        protected List<List<Object>> doInBackground(Void... voids) {
            try {
                // Make the API request to read data
                ValueRange response = sheetsService.spreadsheets().values()
                        .get(AppConfig.SPREADSHEET_ID, range)
                        .execute();

                return response.getValues();
            } catch (IOException e) {
                Log.e("ReadSheetDataTask", "Error: " + e.getMessage());
                return null;
            }
        }

        // onPostExecute is being used to pass the values retrieved from the sheet to other methods through the DataReadListener interface
        @Override
        protected void onPostExecute(List<List<Object>> values) {
            if (values != null) {
                listener.onDataRead(values);
            } else {
                listener.onError("Error reading data from Google Sheet.");
            }
        }
    }
}

