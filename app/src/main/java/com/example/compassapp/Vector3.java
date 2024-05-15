package com.example.compassapp;

public class Vector3 {
    private final double _x, _y, _z;

    public Vector3(double x, double y, double z){
        _x = x;
        _y = y;
        _z = z;
    }

    public Vector3 Normalized(){
        double mag = Math.sqrt(Dot(this, this));
        return new Vector3(_x/mag, _y/mag, _z/mag);
    }

    public static Vector3 Cross(Vector3 a, Vector3 b){
        return new Vector3(a._y*b._z - a._z*b._y,
                a._z*b._x - a._x*b._z,
                a._x*b._y - a._y*b._x);
    }

    public static double Dot(Vector3 a, Vector3 b){
        return a._x*b._x + a._y*b._y + a._z*b._z;
    }

    public static Vector3 FromSpherical(double lat, double lon){
        double latRad = Math.toRadians(lat);
        double longRad = Math.toRadians(lon);
        return new Vector3(Math.cos(longRad) * Math.cos(latRad),
                           Math.sin(longRad) * Math.cos(latRad),
                              Math.sin(latRad));
    }

    public static double RadiansBetween(Vector3 a, Vector3 b) {
        return Math.acos(Dot(a.Normalized(), b.Normalized()));
    }

    public static double SignedRadiansBetween(Vector3 a, Vector3 b, Vector3 up) {
        //atan2((Va x Vb) . Vn, Va . Vb)
        return Math.atan2(Dot(Cross(a, b), up.Normalized()), Dot(a, b));
    }
}
