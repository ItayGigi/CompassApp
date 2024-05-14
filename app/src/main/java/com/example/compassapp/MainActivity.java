package com.example.compassapp;

import androidx.core.app.ActivityCompat;

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

import java.text.MessageFormat;

public class MainActivity extends Activity implements SensorEventListener {

    private ImageView compassImage;
    private ImageView northCompassImage;

    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor accelerometer;
    private Sensor orientation;
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private float[] lastOrientation = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;
    private float[] rotationMatrix = new float[9];
    private float[] orientationVec = new float[3];

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
//    private LocationCallback locationCallback;
    private float angleFromNorth = 0f;

    private static final float ALPHA = 0.15f; //lower alpha should equal smoother movement

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        textView = findViewById(R.id.textView);
        compassImage = findViewById(R.id.compass);
        northCompassImage = findViewById(R.id.compass1);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        while (!isLocationPermissionGranted()) ;

        initLocationCheck();
    }

    private void initLocationCheck(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = locationRequest.create();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(50);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //instantiating the LocationCallBack
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                //Showing the latitude, longitude and accuracy on the home screen.
                for (Location location : locationResult.getLocations()) {
                    textView.setText(MessageFormat.format("Lat: {0} Long: {1} Accuracy: {2}", location.getLatitude(), location.getLongitude(), location.getAccuracy()));
                    angleFromNorth = calcAngleFromNorthToTarget(location.getLatitude(), location.getLongitude(), 32.0553642, 34.8637358);
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private boolean isLocationPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_GAME);

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, orientation);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == orientation) {
            float northAngle = event.values[0];
            northCompassImage.setRotation(-northAngle);
            compassImage.setRotation(-northAngle-angleFromNorth);
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