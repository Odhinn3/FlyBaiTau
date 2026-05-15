package com.example.flybaitau.model;

public class CameraProfile {

    public final String name;
    public final double fovHorizontalDeg;
    public final double imageWidthPx;
    public final double imageHeightPx;
    public final double aspectRatio;

    public CameraProfile(String name,
                         double fovHorizontalDeg,
                         double imageWidthPx,
                         double imageHeightPx) {
        this.name = name;
        this.fovHorizontalDeg = fovHorizontalDeg;
        this.imageWidthPx = imageWidthPx;
        this.imageHeightPx = imageHeightPx;
        this.aspectRatio = imageWidthPx / imageHeightPx;
    }

    // Все поддерживаемые камеры
    public static final CameraProfile MAVIC_4_PRO = new CameraProfile(
            "DJI Mavic 4 Pro (Hasselblad)",
            72.0,
            12288.0,
            8192.0
    );

    // Сюда можно добавлять другие камеры
    // public static final CameraProfile MAVIC_3_PRO = new CameraProfile(
    //         "DJI Mavic 3 Pro",
    //         84.0,
    //         5280.0,
    //         3956.0
    // );
}