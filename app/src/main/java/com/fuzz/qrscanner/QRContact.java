package com.fuzz.qrscanner;

import android.util.Log;

import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class QRContact {

    private static String getFirst(String[] values) {
        if (values != null && values.length >= 1) {
            return values[0];
        }
        return null;
    }

    public final String name;
    public final String email;
    public final String org;
    public final String title;

    public final String rawQRCode;

    public QRContact(ParsedResult parseResult, String rawQRCode) {
        if (parseResult.getType() != ParsedResultType.ADDRESSBOOK) {
            throw new IllegalArgumentException("Parse result must be ADDRESSBOOK. Actual TypeL: " + parseResult.getType().name());
        }
        AddressBookParsedResult addressResult = (AddressBookParsedResult) parseResult;
        this.name = getFirst(addressResult.getNames());
        this.email = getFirst(addressResult.getEmails());
        this.org = addressResult.getOrg();
        this.title = addressResult.getTitle();
        this.rawQRCode = rawQRCode;
    }

    public QRContact(String name, String email, String org, String title, String rawQRCode) {
        this.name = name;
        this.email = email;
        this.org = org;
        this.title = title;
        this.rawQRCode = rawQRCode;
    }

    public boolean isValid() {
        return this.name != null || this.email != null || this.org != null || this.title != null || this.rawQRCode != null;
    }

}
