package com.fuzz.qrscanner;


import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {
    public static final String KEY_PREF_TOKEN = "pref_smartsheet_token";
    public static final String KEY_PREF_SHEET_ID = "pref_smartsheet_sheet_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}

