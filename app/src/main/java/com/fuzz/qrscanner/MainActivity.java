package com.fuzz.qrscanner;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.smartsheet.api.SmartsheetException;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Static vars
    public static final String LOG_TAG = "QRContact";

    // Member vars
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button scanButton = (Button) findViewById(R.id.btnScan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                integrator.setPrompt(getResources().getString(R.string.msg_scan_prompt));
                integrator.setOrientationLocked(false);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(true);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                integrator.initiateScan();
            }
        });

        final Button saveButton = (Button) findViewById(R.id.btnSave);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Make sure our settings are configured
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String token = sharedPref.getString(SettingsActivity.KEY_PREF_TOKEN, "");
                long sheetID = getLong(sharedPref.getString(SettingsActivity.KEY_PREF_SHEET_ID, ""));
                if (!isSet(token) || sheetID <= 0) {
                    Toast.makeText(MainActivity.this, R.string.msg_settings, Toast.LENGTH_LONG).show();
                } else {
                    // See if we have a valid contact
                    QRContact contact = getContactFromForm();
                    if (!contact.isValid()) {
                        Toast.makeText(MainActivity.this, R.string.msg_set_contact, Toast.LENGTH_LONG).show();
                    } else {
                        // Save our contact
                        MainActivity.this.dialog = ProgressDialog.show(MainActivity.this,
                                getResources().getString(R.string.title_saving),
                                getResources().getString(R.string.msg_saving), true, false);
                        SaveContactTask task = new SaveContactTask(token, sheetID);
                        task.execute(new QRContact[]{contact});
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_clear_cache:
                boolean success = SmartsheetAPI.clearCache(this);
                Toast.makeText(MainActivity.this,
                        getResources().getString(success ? R.string.msg_cache_clear_success : R.string.msg_cache_clear_failure),
                        Toast.LENGTH_LONG).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            try {
                // Get the content of the scan results
                Result result = new Result(scanResult.getContents(), scanResult.getRawBytes(), null, BarcodeFormat.QR_CODE);
                ParsedResult parsedResult = ResultParser.parseResult(result);

                // Try to use built in parsing logic in library
                QRContact contact;
                if (parsedResult != null && parsedResult.getType() == ParsedResultType.ADDRESSBOOK) {
                    contact = new QRContact(parsedResult, scanResult.getContents());
                } else {
                    // Fall back to just showing raw QR code text
                    contact = new QRContact(null, null, null, null, scanResult.getContents());
                }
                bindContactToForm(contact);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing QR contact.", e);
            }
        }
    }

    public boolean isSet(String value) {
        return value != null && value.trim().length() > 0;
    }

    public long getLong(String value) {
        if (value == null) {
            return Long.MIN_VALUE;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return Long.MIN_VALUE;
        }
    }

    public void bindContactToForm(QRContact contact) {
        ((EditText) findViewById(R.id.txtName)).setText(contact.name);
        ((EditText) findViewById(R.id.txtEmail)).setText(contact.email);
        ((EditText) findViewById(R.id.txtOrg)).setText(contact.org);
        ((EditText) findViewById(R.id.txtTitle)).setText(contact.title);
        ((EditText) findViewById(R.id.txtRaw)).setText(contact.rawQRCode);
    }

    public QRContact getContactFromForm() {
        String name = ((EditText) findViewById(R.id.txtName)).getText().toString();
        String email = ((EditText) findViewById(R.id.txtEmail)).getText().toString();
        String org = ((EditText) findViewById(R.id.txtOrg)).getText().toString();
        String title = ((EditText) findViewById(R.id.txtTitle)).getText().toString();
        String raw = ((EditText) findViewById(R.id.txtRaw)).getText().toString();
        return new QRContact(name, email, org, title, raw);
    }

    public void clearContactForm() {
        ((EditText) findViewById(R.id.txtName)).setText("");
        ((EditText) findViewById(R.id.txtEmail)).setText("");
        ((EditText) findViewById(R.id.txtOrg)).setText("");
        ((EditText) findViewById(R.id.txtTitle)).setText("");
        ((EditText) findViewById(R.id.txtRaw)).setText("");
    }

    private class SaveContactTask extends AsyncTask<QRContact, Void, Boolean> {
        private final String token;
        private final long sheetID;

        private SaveContactTask(String token, long sheetID) {
            this.token = token;
            this.sheetID = sheetID;
        }

        @Override
        protected Boolean doInBackground(QRContact... params) {
            Boolean success = Boolean.FALSE;
            try {
                SmartsheetAPI api = new SmartsheetAPI(MainActivity.this, this.token);
                api.saveContact(this.sheetID, params[0]);
                success = Boolean.TRUE;
            } catch (SmartsheetException ex) {
                Log.e(LOG_TAG, "Error saving contact.", ex);
            }
            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            int msg = R.string.msg_save_failure;
            if (success) {
                msg = R.string.msg_save_success;
                clearContactForm();
            }

            if (MainActivity.this.dialog != null) {
                MainActivity.this.dialog.dismiss();
                MainActivity.this.dialog = null;
            }
            Toast.makeText(MainActivity.this, getResources().getString(msg), Toast.LENGTH_LONG).show();
        }
    }
}
