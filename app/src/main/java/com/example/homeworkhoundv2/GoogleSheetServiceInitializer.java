package com.example.homeworkhoundv2;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleSheetServiceInitializer {

    public static Sheets initialize(Context context) throws IOException {
        // Define the Google Sheets API scope
        List<String> scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);

        // Load the credentials JSON file (replace 'YOUR_CREDENTIALS_FILE.json' with your file)
        //InputStream credentialsStream = GoogleSheetServiceInitializer.class.getResourceAsStream("/my_credentials.json");
        InputStream credentialsStream = context.getResources().openRawResource(R.raw.my_credentials);

        // Create a GoogleCredentials object
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(scopes);

        if (credentials == null) {
            Log.e("Error Log", "Credentials are null");
        }
        else {
            Log.d("Error Log", "Credentials are not null");
        }

        // Create an HTTP request initializer using the credentials
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        // Initialize the HTTP transport
        HttpTransport httpTransport = new com.google.api.client.http.javanet.NetHttpTransport();

        // Build and return the Sheets service
        return new Sheets.Builder(
                httpTransport,
                JacksonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("HomeworkHoundV2")
                .build();
    }
}


