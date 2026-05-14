package com.example.flybaitau.model;

public class Waypoint {
    public double latitude;
    public double longitude;
    public double altitude;
    public double speed;
    public double yaw; // азимут съёмки в градусах

    public Waypoint(double latitude, double longitude,
                    double altitude, double speed, double yaw) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.yaw = yaw;
    }

    // Конструктор без yaw для обратной совместимости
    public Waypoint(double latitude, double longitude,
                    double altitude, double speed) {
        this(latitude, longitude, altitude, speed, 0);
    }
}
