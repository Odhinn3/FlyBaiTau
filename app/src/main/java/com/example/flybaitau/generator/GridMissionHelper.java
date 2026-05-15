package com.example.flybaitau.generator;

import com.google.android.gms.maps.model.LatLng;
import com.example.flybaitau.model.CameraProfile;
import com.example.flybaitau.model.Waypoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GridMissionHelper {

    public static List<Waypoint> generate(
            List<LatLng> polygon,
            double altitude,
            double frontlap,
            double sidelap,
            double speed,
            double azimuthDeg,
            CameraProfile camera) {

        double fovHorizRad = Math.toRadians(camera.fovHorizontalDeg);
        double footprintWidth = 2 * altitude * Math.tan(fovHorizRad / 2);
        double footprintHeight = footprintWidth / camera.aspectRatio;

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

        // Азимут всегда 0-179° (восточные направления)
        // Первая точка всегда в SW углу полигона
        // goingUp=false (первая полоса): maxY → minY → shootingYaw = azimuthDeg
        // goingUp=true  (вторая полоса): minY → maxY → shootingYaw = (azimuthDeg+180)%360
        // goingUp переключается только для непустых полос
        boolean goingUp = false;

        double currentX = minX + stripDistance / 2;
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

            if (!stripPoints.isEmpty()) {
                if (!goingUp) Collections.reverse(stripPoints);

                double shootingYaw = goingUp
                        ? (azimuthDeg + 180) % 360
                        : azimuthDeg;

                for (double[] pt : stripPoints) {
                    double rx = pt[0] * Math.cos(angleRad) - pt[1] * Math.sin(angleRad);
                    double ry = pt[0] * Math.sin(angleRad) + pt[1] * Math.cos(angleRad);
                    double lat = centerLat + ry / metersPerDegLat;
                    double lon = centerLon + rx / metersPerDegLon;
                    waypoints.add(new Waypoint(lat, lon, altitude, speed, shootingYaw));
                }

                goingUp = !goingUp; // только для непустых полос
            }

            currentX += stripDistance;
        }

        return waypoints;
    }

    private static boolean isInsideRotatedPolygon(
            double[] point, List<double[]> polygon) {
        int n = polygon.size();
        boolean inside = false;
        double px = point[0], py = point[1];
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i)[0], yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0], yj = polygon.get(j)[1];
            if (((yi > py) != (yj > py))
                    && (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        return inside;
    }
}