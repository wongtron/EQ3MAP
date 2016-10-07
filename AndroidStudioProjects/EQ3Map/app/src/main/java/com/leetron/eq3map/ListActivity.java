package com.leetron.eq3map;


import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

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

public class ListActivity extends AppCompatActivity {

    ListView list;
    TextView countTV;
    String quakeServerURL;
    String resultCount;
    String [] searchResultHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list);

        list = (ListView) findViewById(R.id.listView);

        countTV = (TextView) findViewById(R.id.resultCount);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();

        // get the server HTTP GET request URL
        quakeServerURL = bundle.getString("quakeServerURL");

        resultCount = "0";


        new TransTask().execute(quakeServerURL);
    }

    class TransTask extends AsyncTask<String, Void, List<Map<String, String>>> {

        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(ListActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected List<Map<String, String>> doInBackground(String... params) {
            // this is the list for the collection of earthquake events from the query
            List<Map<String, String>> data = new ArrayList<>();

            // this is the map for the "name":"value pair of the earthquake data
            Map<String, String> row;

            String[] eq = null;
            String[] individual = null;

            try {
                URL url = new URL(params[0]);
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader in = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }

                eq = sb.toString().split("<br />");

                searchResultHeader = eq[0].split(",");

                // eq[0] has the search result count

                resultCount = searchResultHeader [0];

                if (eq == null || eq.length <= 1) {
                    // there is no quake data returned from server
                    resultCount = "0";
                    return null;
                }





                // build the quake data from eq[1]
                for (int i = 1; i <= eq.length - 1; i++) {
                    individual = eq[i].split(",");
                    row = new HashMap<>();
                    row.put("event_date", individual[1]);

                    row.put("event_time", individual[2]);

                    row.put("latitude", individual[3]);

                    row.put("longitude", individual[4]);

                    row.put("depth", individual[5]);

                    row.put("magnitude", individual[6]);


                    if (getPlace(individual[16]) != null) {
                        row.put("place", individual[16]);
                        row.put("place_short", getPlace(individual[16]));
                    }
                    else {
                        row.put("place", "Unknown");
                        row.put("place_short", "Unknown");
                    }

                    data.add(row);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                resultCount = "Check if the server is up!";
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                resultCount = "Network error. Please check the network.";
                return null;
            }
            return data;
        }

        @Override
        protected void onPostExecute(final List data) {

            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

            if (data == null) {
                countTV.setText(resultCount);
                return;
            }

            SimpleAdapter adapter = new SimpleAdapter(
                    ListActivity.this,
                    data,
                    R.layout.simple_list_item,
                    new String[]{"magnitude", "event_date", "event_time", "place_short"},
                    new int[]{R.id.magnitude, R.id.dateView, R.id.timeView, R.id.placeView}
            ) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    Map<String, String> row;
                    View view = super.getView(position, convertView, parent);
                    TextView text = (TextView) view.findViewById(R.id.magnitude);
                    row = new HashMap<>();
                    row = (Map<String, String>) data.get(position);

                    String mag = row.get("magnitude");
                    Double magnitude = Double.parseDouble(mag);

                    if (magnitude <= 2.0) {
                        text.setTextColor(Color.GREEN);
                    } else
                    if (magnitude > 2.0 && magnitude <= 4.0 ) {
                        text.setTextColor(Color.BLUE);
                    } else
                    if (magnitude > 4.0 && magnitude <= 6.0 ) {
                        text.setTextColor(Color.MAGENTA);
                    } else
                    if (magnitude > 6.0 && magnitude <= 8.0 ) {
                        text.setTextColor(Color.CYAN);
                    } else {

                        text.setTextColor(Color.RED);
                    }

                    return view;
                }

            };

            countTV.setText(resultCount);

            list.setAdapter(adapter);

            list.setOnItemClickListener(new ListView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                        long arg3) {

                    Map<String, String> row = new HashMap<>();


                    row = (Map<String, String>) list.getItemAtPosition(arg2);

                    new AlertDialog.Builder(ListActivity.this)
                            .setTitle("Detail Information：")
                            .setMessage(
                                    "Event Date: " + row.get("event_date") + "\n" +
                                            "Event Time: " + row.get("event_time") + "\n" +
                                            "Latitude: " + row.get("latitude") + "\n" +
                                            "Longitude: " + row.get("longitude") + "\n" +
                                            "Magnitude: " + row.get("magnitude") + "\n" +
                                            "Depth: " + row.get("depth") + "\n" +
                                            "Place: " + row.get("place")
                            )
                            .setPositiveButton("確定", null)
                            .show();
                }
            });

        }

    }


    public void showListResultInfo (View view) {
        String message = "Search Result Count: " + searchResultHeader[0] + "\n" +
                "Time Range: " + searchResultHeader[1] + "-"
                + searchResultHeader[2] + "-" + searchResultHeader[3] + "\n" +
                "to " + searchResultHeader[4] + "-"
                + searchResultHeader[5] + "-" + searchResultHeader[6] + "\n" +
                "Magnitude Range: " + searchResultHeader[7] + "-"
                + searchResultHeader[8];
        if (!searchResultHeader[9].equals("")) {
            message += "\n" + "Latitude: " + searchResultHeader[9] + "\n" +
                    "Longitude: " + searchResultHeader[10] + "\n" +
                    "Radius: " + searchResultHeader[11] ;
        }

        new AlertDialog.Builder(ListActivity.this)
                .setTitle("Search Result Information：")
                .setMessage(
                        message
                )
                .setPositiveButton("確定", null)
                .show();


    }
    private static String getPlace(String str) {

        // str is the description of the place which could be pretty long.
        // extract only the last word for now as the last word is usually the
        // state name or the country name
        if (str != null) {
            // get the last 2 words in str as the place
            // if there is only 1 word, then return the 1 word
            String s[] = str.split("\\s+");
            if (s.length >= 2)
                return s[s.length - 2] + " " + s[s.length - 1];
            else
                return str;
        }
        return null;
    }

}


