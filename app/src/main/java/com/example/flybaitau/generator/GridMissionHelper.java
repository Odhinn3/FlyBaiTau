package com.example.flybaitau.generator;

import com.google.android.gms.maps.model.LatLng;
import com.example.flybaitau.model.Waypoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GridMissionHelper {

    private static final double FOV_HORIZONTAL = 84.0;
    private static final double ASPECT_RATIO = 4.0 / 3.0;

    public static List<Waypoint> generate(
            List<LatLng> polygon,
            double altitude,
            double frontlap,
            double sidelap,
            double speed,
            double azimuthDeg) {

        double fovHorizRad = Math.toRadians(FOV_HORIZONTAL);
        double footprintWidth = 2 * altitude * Math.tan(fovHorizRad / 2);
        double footprintHeight = footprintWidth / ASPECT_RATIO;

        double stripDistance = footprintWidth * (1 - sidelap);
        double pointDistance = footprintHeight * (1 - frontlap);

        // Центр полигона
        double centerLat = 0, centerLon = 0;
        for (LatLng p : polygon) {
            centerLat += p.latitude;
            centerLon += p.longitude;
        }
        centerLat /= polygon.size();
        centerLon /= polygon.size();

        double metersPerDegLat = 111320.0;
        double metersPerDegLon = 111320.0 * Math.cos(Math.toRadians(centerLat));

        double angleRad = Math.toRadians(azimuthDeg);

        // Поворачиваем полигон
        List<double[]> rotated = new ArrayList<>();
        for (LatLng p : polygon) {
            double dx = (p.longitude - centerLon) * metersPerDegLon;
            double dy = (p.latitude - centerLat) * metersPerDegLat;
            double rx = dx * Math.cos(-angleRad) - dy * Math.sin(-angleRad);
            double ry = dx * Math.sin(-angleRad) + dy * Math.cos(-angleRad);
            rotated.add(new double[]{rx, ry});
        }

        // Bounding box
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] p : rotated) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
        }

        List<Waypoint> waypoints = new ArrayList<>();
        double currentX = minX + stripDistance / 2;
        boolean goingUp = true;

        while (currentX <= maxX) {
            List<double[]> stripPoints = new ArrayList<>();
            double currentY = minY + pointDistance / 2;

            while (currentY <= maxY) {
                double[] pt = new double[]{currentX, currentY};
                if (isInsideRotatedPolygon(pt, rotated)) {
                    stripPoints.add(pt);
                }
                currentY += pointDistance;
            }

            if (!goingUp) Collections.reverse(stripPoints);

            // Азимут съёмки зависит от направления движения в полосе
            // goingUp = движение в одну сторону = azimuthDeg
            // !goingUp = обратное направление = azimuthDeg + 180°
            double shootingYaw = goingUp
                    ? azimuthDeg
                    : (azimuthDeg + 180) % 360;

            for (double[] pt : stripPoints) {
                double rx = pt[0] * Math.cos(angleRad) - pt[1] * Math.sin(angleRad);
                double ry = pt[0] * Math.sin(angleRad) + pt[1] * Math.cos(angleRad);

                double lat = centerLat + ry / metersPerDegLat;
                double lon = centerLon + rx / metersPerDegLon;
                waypoints.add(new Waypoint(lat, lon, altitude, speed, shootingYaw));
            }

            currentX += stripDistance;
            goingUp = !goingUp;
        }

        return waypoints;
    }

    private static boolean isInsideRotatedPolygon(double[] point,
                                                  List<double[]> polygon) {
        int n = polygon.size();
        boolean inside = false;
        double px = point[0], py = point[1];

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i)[0], yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0], yj = polygon.get(j)[1];

            boolean intersect = ((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}