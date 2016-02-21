package com.desem.hack.busgpssending;

import android.location.Location;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.security.spec.ECField;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    @Bind(R.id.start_gps)
    Button startGPS;
    @Bind(R.id.heloo)
    TextView helo;

    private GoogleApiClient apiClient;
    private LocationRequest locationRequest;

    private long UPDATE_INTERVAL = 3000;
    private long FATEST_INTERVAL = 6000;
    private float DISPLACEMENT = 0.001f;

    private HandlerThread handlerThread;
    private Handler handler;
    private boolean locationReady = false;

    private RequestQueue requestQueue;

    private String url = "http://138.251.207.124:4000/api/locations";

    private double delta = 0.00001;
    private double prevLong = 0;
    private double prevLat = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        ButterKnife.bind(this);

        if (apiClient == null) {
            apiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        }


    }

    void startHandler() {
        requestQueue = Volley.newRequestQueue(this);

        handlerThread = new HandlerThread("desem");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (locationReady) {
                    Location location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
                    clickSendRequest(location.getLatitude() + "", location.getLongitude()+"");
                    Log.d("thread", "test " + location);
                }

                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(runnable, 5000);


    }

    @OnClick(R.id.start_gps)
    public void click() {
        String start = getApplication().getString(R.string.start);
        String stop = getApplication().getString(R.string.stop);
        if (startGPS.getText() != null && startGPS.getText().equals(start)) {
            startGPS.setText(stop);
            startGPSTracking();
            startHandler();
        } else if (startGPS.getText() != null) {
            startGPS.setText(start);
            stophandler();
            stopGPSTracking();

        }
    }

    public void clickSendRequest(String langitude, String longitude) {
        Log.d("request", "goolge");
        helo.setText("lat: " + langitude+", long: "+longitude);
        JSONObject locationJson = null;
        try {
            JSONObject obj = new JSONObject();
            obj.put("lat", langitude);
            obj.put("lng", longitude);
            obj.put("timestamp", (int)(System.currentTimeMillis()/1000));
            locationJson = new JSONObject();
            locationJson.put("location", obj);
        } catch (Exception exc) {

        }


        //int method, String url, Listener<JSONObject> listener, ErrorListener errorListener
        Request request = new JsonObjectRequest(Request.Method.POST, url, locationJson,
                new Response.Listener() {
                    @Override
                    public void onResponse(Object response) {
                        Log.d("onResponse", response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("onErrorResponse", error.toString());
            }
        });
        if (requestQueue!=null) {
            Log.d("onErrorResponse", "non null queue");
            requestQueue.add(request);
        } else {
            Log.d("onErrorResponse", "null queue");
            requestQueue = Volley.newRequestQueue(this);
        }

    }

    private void stopGPSTracking() {

        apiClient.disconnect();
    }

    private void startGPSTracking() {
        apiClient.connect();
    }

    private void stophandler() {
        handlerThread.quit();
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.d("COnnected", "yohooo");
        Location location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
        if (location != null & Math.abs(prevLong-location.getLongitude())+Math.abs(prevLat-location.getLatitude())>delta) {

            clickSendRequest(location.getLatitude() + "", location.getLongitude() + "");

            prevLong = location.getLongitude();
            prevLat = location.getLatitude();
        }

        locationReady = true;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onStart() {
        startGPSTracking();
        super.onStart();
    }

    @Override
    protected void onStop() {
        stopGPSTracking();
        handlerThread.quit();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("onConnectionFailed", "yohooo");
    }

}
