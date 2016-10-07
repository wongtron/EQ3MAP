package com.leetron.eq3map;

import android.*;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final int REQUEST_LOCATION = 2;
    private GoogleMap mMap = null;
    GoogleApiClient mGoogleApiClient;
    LocationRequest locationRequest;
    List<Map<String, String>> eqData = new ArrayList<>();
    TextView countTV;
    String resultCount;
    String [] searchResultHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        countTV = (TextView) findViewById(R.id.resultCountMap);

        createLocationRequest();

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();

        // get the server HTTP GET request URL
        String quakeServerURL = bundle.getString("quakeServerURL");

        new TransTask().execute(quakeServerURL);
    }

    class TransTask extends AsyncTask<String, Void, List<Map<String, String>>> {

        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MapsActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected List<Map<String, String>> doInBackground(String... params) {

            if (eqData != null)
                eqData.clear();
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

                // eq[0] has the search result information

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
                    } else {
                        row.put("place", "Unknown");
                        row.put("place_short", "Unknown");
                    }

                    eqData.add(row);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                resultCount = "Check if the server is up!" ;
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                resultCount = "Network error. Please check the network.";
                return null;
            }
            return eqData;
        }

        protected void onPostExecute(final List data) {
            LatLngBounds.Builder builder = null;
            LatLngBounds bounds = null;
            LatLng eqLatLng = new LatLng(25.033408, 121.564099);



            if (mMap == null)
                return;

            countTV.setText(resultCount);

            // display all EQ locations with marker
            // List<Map<String, String>> eqData = new ArrayList<>();
            if (eqData != null && eqData.size() != 0) {

                builder = new LatLngBounds.Builder();

                for (int i = 0; i < eqData.size(); i++) {
                    float color = BitmapDescriptorFactory.HUE_RED;
                    Double magnitude =  Double.parseDouble(eqData.get(i).get("magnitude"));
                    if (magnitude <=2)
                        color = BitmapDescriptorFactory.HUE_GREEN;
                    else if (magnitude > 2.0 && magnitude <=4.0)
                        color = BitmapDescriptorFactory.HUE_BLUE;
                    else if (magnitude > 4.0 && magnitude <=6.0)
                        color = BitmapDescriptorFactory.HUE_YELLOW;
                    else if (magnitude > 6.0 && magnitude <=8.0)
                        color = BitmapDescriptorFactory.HUE_ORANGE;
                    else
                        color = BitmapDescriptorFactory.HUE_RED;

                    eqLatLng = new LatLng(Double.parseDouble(eqData.get(i).get("latitude")), Double.parseDouble(eqData.get(i).get("longitude")));

                    try {
                        mMap.addMarker(new MarkerOptions().position(eqLatLng).title("Magnitude: " + eqData.get(i).get("magnitude"))
                                .snippet(
                                        "Date: " + eqData.get(i).get("event_date") + "\n" +
                                                "Time: " + eqData.get(i).get("event_time") + "\n" +
                                                "Place: " + eqData.get(i).get("place") + "\n" +
                                                "Latitude: " + eqData.get(i).get("latitude") + "\n" +
                                                "Longitude: " + eqData.get(i).get("longitude") + "\n" +
                                                "Depth: " + eqData.get(i).get("depth"))
                                .icon(BitmapDescriptorFactory.defaultMarker(color)));
                    } catch (java.lang.NullPointerException e) {
                        e.printStackTrace();
                        return;
                    }

                    builder.include(eqLatLng);
                }
                // http://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers
                bounds = builder.build();
            }

            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            int padding = 0; // offset from edges of the map in pixels

            if (bounds != null) {
                //CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 1920, 1080, padding);
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                mMap.moveCamera(cu);
            }
            else

                mMap.moveCamera(CameraUpdateFactory.newLatLng(eqLatLng));
        }
    }

    private static String getPlace(String str) {

        // str is the description of the place which could be pretty long.
        // extract only the last word for now as the last word is usually the
        // state name or the country name
        if (str != null) {
            // get the last 2 words in str as the place
            // if there is only 1 word, then return the 1 word
            String s[]  = str.split("\\s+");
            if (s.length >=2)
                return s[s.length-2] + " " + s[s.length-1];
            else
                return str;
        }
        return null;
    }

    public void showMapResultInfo (View view) {
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

        new AlertDialog.Builder(MapsActivity.this)
                .setTitle("Search Result Information：")
                .setMessage(
                        message
                )
                .setPositiveButton("確定", null)
                .show();


    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLngBounds.Builder builder = null;
        LatLngBounds bounds = null;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {
            setupMyLocation();
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle(marker.getTitle())
                        .setMessage(marker.getSnippet())
                        .setPositiveButton("OK", null)
                        .show();
                return true;
            }
        });

        mMap.setInfoWindowAdapter(
                new GoogleMap.InfoWindowAdapter() {
                    @Override
                    public View getInfoWindow(Marker marker) {
                        View view = getLayoutInflater().inflate(
                                R.layout.info_window, null);
                        TextView title =
                                (TextView) view.findViewById(R.id.info_title);
                        title.setText("Title: "+marker.getTitle());
                        TextView snippet =
                                (TextView) view.findViewById(R.id.info_snippet);
                        snippet.setText(marker.getTitle());
                        return view;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        return null;
                    }
                }
        );

        LatLng eqLatLng = new LatLng(25.033408, 121.564099);

        // display all EQ locations with marker
        // List<Map<String, String>> eqData = new ArrayList<>();
        if (eqData != null && eqData.size() != 0) {

            builder = new LatLngBounds.Builder();

            for (int i = 0; i < eqData.size(); i++) {
                float color = BitmapDescriptorFactory.HUE_RED;
                Double magnitude =  Double.parseDouble(eqData.get(i).get("magnitude"));
                if (magnitude <=2)
                    color = BitmapDescriptorFactory.HUE_GREEN;
                else if (magnitude > 2.0 && magnitude <=4.0)
                    color = BitmapDescriptorFactory.HUE_BLUE;
                else if (magnitude > 4.0 && magnitude <=6.0)
                    color = BitmapDescriptorFactory.HUE_YELLOW;
                else if (magnitude > 6.0 && magnitude <=8.0)
                    color = BitmapDescriptorFactory.HUE_ORANGE;
                else
                    color = BitmapDescriptorFactory.HUE_RED;

                eqLatLng = new LatLng(Double.parseDouble(eqData.get(i).get("latitude")), Double.parseDouble(eqData.get(i).get("longitude")));

                mMap.addMarker(new MarkerOptions().position(eqLatLng).title("Magnitude: " + eqData.get(i).get("magnitude") )
                        .snippet(
                                "Date: " + eqData.get(i).get("event_date")  + "\n" +
                                        "Time: " + eqData.get(i).get("event_time")  + "\n" +
                                        "Place: " + eqData.get(i).get("place")  + "\n" +
                                        "Latitude: " + eqData.get(i).get("latitude")  + "\n" +
                                        "Longitude: " + eqData.get(i).get("longitude") + "\n" +
                                        "Depth: " + eqData.get(i).get("depth"))
                        .icon(BitmapDescriptorFactory.defaultMarker(color)));

                builder.include(eqLatLng);
            }
            // http://stackoverflow.com/questions/14828217/android-map-v2-zoom-to-show-all-the-markers
            bounds = builder.build();
        }


        int padding = 0; // offset from edges of the map in pixels
        /*
        if (bounds != null) {
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 1920, 1080, padding);

            mMap.moveCamera(cu);
        }
        else
        */
        mMap.moveCamera(CameraUpdateFactory.newLatLng(eqLatLng));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 使用者允許權限
                    //noinspection MissingPermission
                    setupMyLocation();
                } else {
                    // 使用者拒絕授權 , 停用 MyLocation 功能
                }
                break;
        }
    }

    private void setupMyLocation() {
        //noinspection MissingPermission
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(
                new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {
                        // 透過位置服務，取得目前裝置所在
                        LocationManager locationManager =
                                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        Criteria criteria = new Criteria();
                        // 設定標準為存取精確
                        criteria.setAccuracy(Criteria.ACCURACY_FINE);
                        // 向系統查詢最合適的服務提供者名稱 ( 通常也是 "gps")
                        String provider = locationManager.getBestProvider(criteria, true);
                        //noinspection MissingPermission
                        Location location = locationManager.getLastKnownLocation(provider);
                        if (location != null) {
                            Log.i("LOCATION", location.getLatitude() + "/" +
                                    location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude())
                                    , 15));
                        }
                        return false;
                    }
                }
        );
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //noinspection MissingPermission
        /*
        Location location = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        if (location != null){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude())
                    , 15));
        }
        //noinspection MissingPermission
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locationRequest, this);
                */
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.d("LOCATION", location.getLatitude() + "," +
                    location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude())
                    , 11));
        }
    }
}
