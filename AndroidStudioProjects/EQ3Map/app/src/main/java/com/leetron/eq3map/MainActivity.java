package com.leetron.eq3map;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    EditText startYearET;
    EditText startMonthET;
    EditText startDateET;

    EditText endYearET;
    EditText endMonthET;
    EditText endDateET;

    EditText minMagnitudeET;
    EditText maxMagnitudeET;
    EditText theLocationET;
    EditText theRadiusET;

    RadioGroup radioOutGroup;
    RadioButton radioOutButton;

    String startYear;
    String startMonth;
    String startDate ;


    String endYear;
    String endMonth;
    String endDate;

    String minMagnitude;
    String maxMagnitude;

    String theLocation;
    String searchLocLat;
    String searchLocLong;
    String radius;
    String outputFormat;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startYearET = (EditText) findViewById(R.id.startYear);
        startMonthET = (EditText) findViewById(R.id.startMonth);
        startDateET = (EditText) findViewById(R.id.startDate);

        endYearET = (EditText) findViewById(R.id.endYear);
        endMonthET = (EditText) findViewById(R.id.endMonth);
        endDateET = (EditText) findViewById(R.id.endDate);

        minMagnitudeET = (EditText) findViewById(R.id.minMagnitude);
        maxMagnitudeET = (EditText) findViewById(R.id.maxMagnitude);

        theLocationET = (EditText) findViewById(R.id.location);
        theRadiusET = (EditText) findViewById(R.id.radius);

        radioOutGroup = (RadioGroup) findViewById(R.id.radioOutputFormat);

    }

    public void startSearch(View v) {


        startYear = startYearET.getText().toString();
        startMonth = startMonthET.getText().toString();
        startDate = startDateET.getText().toString();

        endYear = endYearET.getText().toString();
        endMonth = endMonthET.getText().toString();
        endDate = endDateET.getText().toString();

        minMagnitude = minMagnitudeET.getText().toString();
        maxMagnitude = maxMagnitudeET.getText().toString();

        theLocation = theLocationET.getText().toString();
        radius = theRadiusET.getText().toString();

        searchLocLat = "";
        searchLocLong = "";

        // get selected radio button from radioGroup
        int selectedId = radioOutGroup.getCheckedRadioButtonId();

        // find the radiobutton by returned id
        radioOutButton = (RadioButton) findViewById(selectedId);

        outputFormat = radioOutButton.getText().toString();



        StrictMode
                .setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork() // or .detectAll() for all detectable problems
                        .penaltyLog()
                        .build());
        StrictMode
                .setVmPolicy(new StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
//4.22以下版本不加此行  .detectLeakedClosableObjects()
                        .penaltyLog()
                        .penaltyDeath()
                        .build());


        // if there is location, find the location information from Google Geocode

        if ((theLocation != null) && (theLocation.trim().length() != 0)) {

            StringBuilder jsonStr = new StringBuilder();
            String loca = theLocation.trim();
            loca = loca.replaceAll("\\s+", "%20");

            String geocode = "https://maps.googleapis.com/maps/api/geocode/json?address="+loca;

            try {
                URL url = new URL(geocode);
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader in = new BufferedReader(isr);

                String line;
                while ((line = in.readLine()) != null) {
                    jsonStr.append(line);
                }

                JSONObject jsonObj = null;

                try {
                    jsonObj = new JSONObject(jsonStr.toString());
                    // Getting JSON Array node
                    JSONArray results = jsonObj.getJSONArray("results");
                    JSONObject c = results.getJSONObject(0);
                    JSONObject loc = c.getJSONObject("geometry").getJSONObject("location");
                    searchLocLat = (String) loc.getString("lat");
                    searchLocLong = (String) loc.getString("lng");

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            }

            if (searchLocLat.equals("")) {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("No such location")
                        .setMessage(
                                "Google Geocode cannot find the location '" + theLocation.trim() + "'."
                        )
                        .setPositiveButton("確定", null)
                        .show();

                return;
            }

        }


        Intent intent = new Intent();

        if (outputFormat.equals("Google Map")) {
            intent.setClass(MainActivity.this, MapsActivity.class);
        } else {
            intent.setClass(MainActivity.this, ListActivity.class);
        }

        /* new一個Bundle物件，並將要傳遞的資料傳入 */
        Bundle bundle = new Bundle();
        bundle.putString("quakeServerURL", buildReqURL());

/*
        bundle.putString("startYear", startYear);
        bundle.putString("starMonth", startMonth);
        bundle.putString("startDate", startDate);
        bundle.putString("endYear", endYear);
        bundle.putString("endMonth", endMonth);
        bundle.putString("endDate", endDate);
        bundle.putString("minMagnitude", minMagnitude);
        bundle.putString("maxMagnitude", maxMagnitude);
        bundle.putString("searchLocLat", searchLocLat);
        bundle.putString("searchLocLong", searchLocLong);
        bundle.putString("radius", radius);
*/
        intent.putExtras(bundle);

        startActivity(intent);
    }

    private String buildReqURL () {

         String url = "http://192.168.1.109:8080/EarthquakeServer2/GetFromForm3?";
        //String url = "http://172.17.48.112:8080/EarthquakeServer3/GetFromForm3?";
        //String url = "http://140.137.218.59:8080/EarthquakeServer3/GetFromForm3?";

        url += "startYear=" + startYear;
        url += "&" + "startMonth=" + startMonth;
        url += "&" + "startDate=" + startDate;
        url += "&" + "endYear=" + endYear;
        url += "&" + "endMonth=" + endMonth;
        url += "&" + "endDate=" + endDate;
        url += "&" + "minMagnitude=" + minMagnitude;
        url += "&" + "maxMagnitude=" + maxMagnitude;
        url += "&" + "searchLocLat=" + searchLocLat;
        url += "&" + "searchLocLong=" + searchLocLong;
        url += "&" + "radius=" + radius;

        return url;

    }

}