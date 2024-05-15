package com.example.compassapp;

import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    private ImageView _compassImage;
    private ImageView _northCompassImage;
    private TextView _topText;
    private TextView _distText;
    private TextView _locationText;


    private SensorManager _sensorManager;
    private Sensor _orientation;

    private FusedLocationProviderClient _fusedLocationClient;
    private LocationRequest _locationRequest;
    private LocationCallback _locationCallback;

    private Location _myLoc;
    private Location _targetLoc;

    private Geocoder geocoder;

    private float _angleFromNorth = 0f;

    private static final float ALPHA = 0.15f; //lower alpha should equal smoother movement
    private static final float EARTH_RADIUS = 6371;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        _topText = findViewById(R.id.textView);
        _distText = findViewById(R.id.distText);
        _locationText = findViewById(R.id.locNameText);

        _compassImage = findViewById(R.id.compass);
        _northCompassImage = findViewById(R.id.compass1);

        _sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        _orientation = _sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        if (!isLocationPermissionGranted()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }

        initLocationCheck();

        _locationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSearchDialog();
            }
        });

        geocoder = new Geocoder(this, Locale.getDefault());
    }

    private void showSearchDialog(){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.search_dialog_layout);

        ImageView btnClose = dialog.findViewById(R.id.btn_close);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        SearchView searchView = dialog.findViewById(R.id.search_bar);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            // Override onQueryTextSubmit method which is call when submit query is searched
            @Override
            public boolean onQueryTextSubmit(String query) {
                LinearLayout resultsLayout = dialog.findViewById(R.id.results_layout);
                resultsLayout.removeAllViews();

                try {
                    List<Address> addresses = geocoder.getFromLocationName(query, 10);
                    Log.i("TAG", ""+addresses.size());
                    for (Address address : addresses){
                        TextView textView = new TextView(dialog.getContext());
                        textView.setText(address.getAddressLine(0));
                        textView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                setLocationByAddress(address);
                            }
                        });
                        resultsLayout.addView(textView);
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        dialog.show();
    }

    private void setLocationByAddress(Address address){
        try {
            Integer.parseInt(address.getFeatureName());
            _locationText.setText(address.getAddressLine(0));
        }
        catch (Exception e){
            _locationText.setText(address.getFeatureName());
        }

        _targetLoc = new Location("");
        _targetLoc.setLatitude(address.getLatitude());
        _targetLoc.setLongitude(address.getLongitude());

        Log.d("MY LOCATION", String.format("lat: %f, long: %f", _myLoc.getLatitude(), _myLoc.getLongitude()));
        Log.d("TARGET LOCATION", String.format("lat: %f, long: %f", address.getLatitude(), address.getLongitude()));
        updateLocation();
    }

    private void updateLocation(){
        if (_myLoc == null || _targetLoc == null) return;
        _angleFromNorth = calcAngleFromNorthToTarget(_myLoc.getLatitude(), _myLoc.getLongitude(), _targetLoc.getLatitude(), _targetLoc.getLongitude());
        _topText.setText(String.format("Angle: %s", _angleFromNorth));
        _distText.setText(String.format("%.2f km away", distanceBetween(_myLoc.getLatitude(), _myLoc.getLongitude(), _targetLoc.getLatitude(), _targetLoc.getLongitude())));
    }

    @SuppressLint("MissingPermission")
    private void initLocationCheck(){
        if (!isLocationPermissionGranted()) return;

        _fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        _locationRequest = LocationRequest.create();
        _locationRequest.setInterval(100);
        _locationRequest.setFastestInterval(50);
        _locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //instantiating the LocationCallBack
        _locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    _myLoc = location;
                    updateLocation();
                }
            }
        };
        _fusedLocationClient.requestLocationUpdates(_locationRequest, _locationCallback, Looper.getMainLooper());
    }

    private boolean isLocationPermissionGranted() {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        _sensorManager.registerListener(this, _orientation, SensorManager.SENSOR_DELAY_GAME);

    }

    @Override
    protected void onPause() {
        super.onPause();
        _sensorManager.unregisterListener(this, _orientation);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == _orientation) {
            float northAngle = event.values[0];
            _northCompassImage.setRotation(-northAngle);
            _compassImage.setRotation(-northAngle-_angleFromNorth);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {    }

    private float[] applyLowPassFilter(float[] input, float[] output) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    private float calcAngleFromNorthToTarget(double userLat, double userLong, double targetLat, double targetLong){
        Vector3 pos = Vector3.FromSpherical(userLat, userLong);
        Vector3 target = Vector3.FromSpherical(targetLat, targetLong);

        Vector3 toNorth = Vector3.Cross(pos, new Vector3(0, 0, 1)).Normalized();
        Vector3 toTarget = Vector3.Cross(target, pos).Normalized();

        return 180f - (float)Math.toDegrees(Vector3.SignedRadiansBetween(toTarget, toNorth, pos));
    }

    private float distanceBetween(double lat1, double long1, double lat2, double long2){
        return EARTH_RADIUS * (float)Vector3.RadiansBetween(Vector3.FromSpherical(lat1, long1), Vector3.FromSpherical(lat2, long2));
    }
}