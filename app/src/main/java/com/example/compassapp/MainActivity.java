package com.example.compassapp;

import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends Activity implements SensorEventListener {

    private ImageView _compassImage;
    private ImageView _northCompassImage;
    private TextView _topText;

    private SensorManager _sensorManager;
    private Sensor _orientation;

    private FusedLocationProviderClient _fusedLocationClient;
    private LocationRequest _locationRequest;
    private LocationCallback _locationCallback;

    private float _angleFromNorth = 0f;

    private static final float ALPHA = 0.15f; //lower alpha should equal smoother movement

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        _topText = findViewById(R.id.textView);
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
                    _angleFromNorth = calcAngleFromNorthToTarget(location.getLatitude(), location.getLongitude(), 32.0553642, 34.8637358);
                    _topText.setText(String.format("Angle: %s", _angleFromNorth));
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

    private float calcAngleFromNorthToTarget(double fromLat, double fromLong, double toLat, double toLong){
        Vector3 from = Vector3.FromSpherical(fromLat, fromLong);
        Vector3 to = Vector3.FromSpherical(toLat, toLong);
        Vector3 up = new Vector3(0, 0, 1);

        double dot = Vector3.Dot(Vector3.Cross(to, from).Normalized(), Vector3.Cross(from, up).Normalized());
        float angle = 180f - (float)Math.toDegrees(Math.acos(dot));

        return angle;
    }
}