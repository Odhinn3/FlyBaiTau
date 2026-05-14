package com.example.flybaitau;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.flybaitau.generator.GridMissionHelper;
import com.example.flybaitau.generator.KmzGenerator;
import com.example.flybaitau.model.Waypoint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final LatLng DEFAULT_LOCATION = new LatLng(43.2258, 76.9630);

    private GoogleMap mMap;
    private final List<LatLng> waypoints = new ArrayList<>();
    private final List<LatLng> polygon = new ArrayList<>();
    private boolean isGridMode = false;
    private TextView tvWaypointCount;
    private FusedLocationProviderClient fusedLocationClient;

    private int selectedVertexIndex = -1; // индекс выбранной вершины (-1 = не выбрана)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvWaypointCount = findViewById(R.id.tvWaypointCount);

        Button btnToggleMode = findViewById(R.id.btnToggleMode);
        btnToggleMode.setOnClickListener(v -> {
            isGridMode = !isGridMode;
            polygon.clear();
            waypoints.clear();
            if (mMap != null) mMap.clear();
            btnToggleMode.setText(isGridMode ? "Grid" : "Waypoint");
            tvWaypointCount.setText(isGridMode ? "Вершин: 0" : "Точек: 0");
        });

        Button btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> clearWaypoints());

        Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> exportMission());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        moveToDefault();
        requestLocationAndMove();

        mMap.setOnMapClickListener(latLng -> {
            if (isGridMode) {
                if (selectedVertexIndex >= 0) {
                    // Перемещаем выбранную вершину
                    polygon.set(selectedVertexIndex, latLng);
                    selectedVertexIndex = -1;
                    updateMap();
                } else {
                    // Добавляем новую вершину
                    polygon.add(latLng);
                    updateMap();
                }
            } else {
                waypoints.add(latLng);
                updateMap();
            }
        });

        mMap.setOnMarkerClickListener(marker -> {
            if (isGridMode) {
                Object tag = marker.getTag();
                if (tag instanceof Integer) {
                    int index = (Integer) tag;
                    if (selectedVertexIndex == index) {
                        // Повторный тап — снимаем выделение
                        selectedVertexIndex = -1;
                    } else {
                        selectedVertexIndex = index;
                    }
                    updateMap();
                    return true;
                }
            }
            return false;
        });

        mMap.setOnMapLongClickListener(latLng -> {
            if (isGridMode) {
                if (selectedVertexIndex >= 0) {
                    // Удаляем выбранную вершину
                    polygon.remove(selectedVertexIndex);
                    selectedVertexIndex = -1;
                    updateMap();
                } else if (!polygon.isEmpty()) {
                    polygon.remove(polygon.size() - 1);
                    updateMap();
                }
            } else {
                if (!waypoints.isEmpty()) {
                    waypoints.remove(waypoints.size() - 1);
                    updateMap();
                    Toast.makeText(this, "Последняя точка удалена",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void requestLocationAndMove() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            getLocationAndMove();
        }
    }

    private void getLocationAndMove() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            moveToDefault();
            return;
        }

        mMap.setMyLocationEnabled(true);

        if (isEmulator()) {
            moveToDefault();
            return;
        }

        fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null)
                .addOnSuccessListener(this, location -> {
                    if (location != null && isValidLocation(location)) {
                        LatLng current = new LatLng(
                                location.getLatitude(),
                                location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16));
                    } else {
                        moveToDefault();
                    }
                })
                .addOnFailureListener(e -> moveToDefault());
    }

    private boolean isEmulator() {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.BRAND.startsWith("generic");
    }

    private boolean isValidLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        return lat != 0.0 && lon != 0.0
                && lat >= -90 && lat <= 90
                && lon >= -180 && lon <= 180
                && location.getAccuracy() < 200;
    }

    private void moveToDefault() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 16));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndMove();
            } else {
                moveToDefault();
            }
        }
    }

    private void updateMap() {
        mMap.clear();

        if (isGridMode) {
            if (polygon.size() >= 3) {
                com.google.android.gms.maps.model.PolygonOptions opts =
                        new com.google.android.gms.maps.model.PolygonOptions()
                                .addAll(polygon)
                                .strokeColor(0xFF4CAF50)
                                .fillColor(0x334CAF50)
                                .strokeWidth(3f);
                mMap.addPolygon(opts);
            }
            for (int i = 0; i < polygon.size(); i++) {
                com.google.android.gms.maps.model.MarkerOptions markerOptions =
                        new com.google.android.gms.maps.model.MarkerOptions()
                                .position(polygon.get(i))
                                .title("Вершина " + (i + 1));

                // Выбранная вершина — красный маркер
                if (i == selectedVertexIndex) {
                    markerOptions.icon(
                            com.google.android.gms.maps.model.BitmapDescriptorFactory
                                    .defaultMarker(com.google.android.gms.maps.model
                                            .BitmapDescriptorFactory.HUE_RED));
                } else {
                    markerOptions.icon(
                            com.google.android.gms.maps.model.BitmapDescriptorFactory
                                    .defaultMarker(com.google.android.gms.maps.model
                                            .BitmapDescriptorFactory.HUE_GREEN));
                }

                com.google.android.gms.maps.model.Marker marker =
                        mMap.addMarker(markerOptions);
                if (marker != null) marker.setTag(i);
            }
            tvWaypointCount.setText("Вершин: " + polygon.size() +
                    (selectedVertexIndex >= 0 ? " | Выбрана: " + (selectedVertexIndex + 1) : ""));
        } else {
            for (int i = 0; i < waypoints.size(); i++) {
                mMap.addMarker(new MarkerOptions()
                        .position(waypoints.get(i))
                        .title("Точка " + (i + 1)));
            }
            if (waypoints.size() > 1) {
                PolylineOptions polyline = new PolylineOptions()
                        .addAll(waypoints)
                        .color(0xFF2196F3)
                        .width(5f);
                mMap.addPolyline(polyline);
            }
            tvWaypointCount.setText("Точек: " + waypoints.size());
        }
    }

    private void clearWaypoints() {
        waypoints.clear();
        polygon.clear();
        if (mMap != null) mMap.clear();
        tvWaypointCount.setText(isGridMode ? "Вершин: 0" : "Точек: 0");
    }

    private void exportMission() {
        if (isGridMode) {
            if (polygon.size() < 3) {
                Toast.makeText(this, "Нарисуйте полигон (минимум 3 точки)",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            showGridSettingsDialog();
        } else {
            if (waypoints.size() < 2) {
                Toast.makeText(this, "Добавьте минимум 2 точки",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            exportWaypointMission();
        }
    }

    private void showGridSettingsDialog() {
        android.view.View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_grid_settings, null);

        android.widget.EditText etAltitude = dialogView.findViewById(R.id.etAltitude);
        android.widget.EditText etSpeed = dialogView.findViewById(R.id.etSpeed);
        android.widget.SeekBar seekFrontlap = dialogView.findViewById(R.id.seekFrontlap);
        android.widget.SeekBar seekSidelap = dialogView.findViewById(R.id.seekSidelap);
        android.widget.SeekBar seekAzimuth = dialogView.findViewById(R.id.seekAzimuth);
        android.widget.TextView tvFrontlap = dialogView.findViewById(R.id.tvFrontlap);
        android.widget.TextView tvSidelap = dialogView.findViewById(R.id.tvSidelap);
        android.widget.TextView tvAzimuth = dialogView.findViewById(R.id.tvAzimuth);

        seekFrontlap.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(android.widget.SeekBar s,
                                                  int p, boolean f) {
                        tvFrontlap.setText((p + 10) + "%");
                    }
                    public void onStartTrackingTouch(android.widget.SeekBar s) {}
                    public void onStopTrackingTouch(android.widget.SeekBar s) {}
                });

        seekSidelap.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(android.widget.SeekBar s,
                                                  int p, boolean f) {
                        tvSidelap.setText((p + 10) + "%");
                    }
                    public void onStartTrackingTouch(android.widget.SeekBar s) {}
                    public void onStopTrackingTouch(android.widget.SeekBar s) {}
                });

        seekAzimuth.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(android.widget.SeekBar s,
                                                  int p, boolean f) {
                        tvAzimuth.setText(p + "°");
                    }
                    public void onStartTrackingTouch(android.widget.SeekBar s) {}
                    public void onStopTrackingTouch(android.widget.SeekBar s) {}
                });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Настройки Grid миссии")
                .setView(dialogView)
                .setPositiveButton("Сгенерировать", (dialog, which) -> {
                    double altitude = Double.parseDouble(
                            etAltitude.getText().toString());
                    double speed = Double.parseDouble(
                            etSpeed.getText().toString());
                    double frontlap = (seekFrontlap.getProgress() + 10) / 100.0;
                    double sidelap = (seekSidelap.getProgress() + 10) / 100.0;
                    double azimuth = seekAzimuth.getProgress();
                    exportGridMission(altitude, speed, frontlap, sidelap, azimuth);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private String buildMissionInfo(List<Waypoint> wpList,
                                    double altitude, double speed,
                                    double frontlap, double sidelap) {
        // Площадь полигона в кв. метрах (формула Гаусса)
        double area = 0;
        double metersPerDegLat = 111320.0;
        double centerLat = 0;
        for (LatLng p : polygon) centerLat += p.latitude;
        centerLat /= polygon.size();
        double metersPerDegLon = 111320.0 * Math.cos(Math.toRadians(centerLat));

        for (int i = 0; i < polygon.size(); i++) {
            int j = (i + 1) % polygon.size();
            double xi = polygon.get(i).longitude * metersPerDegLon;
            double yi = polygon.get(i).latitude * metersPerDegLat;
            double xj = polygon.get(j).longitude * metersPerDegLon;
            double yj = polygon.get(j).latitude * metersPerDegLat;
            area += xi * yj - xj * yi;
        }
        area = Math.abs(area) / 2.0;

        // Размер footprint
        double fovRad = Math.toRadians(84.0);
        double footprintW = 2 * altitude * Math.tan(fovRad / 2);
        double footprintH = footprintW / (4.0 / 3.0);

        // Количество снимков
        int photoCount = wpList.size();

        // Расстояние маршрута
        double distance = 0;
        for (int i = 1; i < wpList.size(); i++) {
            double dlat = (wpList.get(i).latitude - wpList.get(i-1).latitude)
                    * metersPerDegLat;
            double dlon = (wpList.get(i).longitude - wpList.get(i-1).longitude)
                    * metersPerDegLon;
            distance += Math.sqrt(dlat * dlat + dlon * dlon);
        }

        // Время полёта
        double timeSeconds = distance / speed;
        int minutes = (int) timeSeconds / 60;
        int seconds = (int) timeSeconds % 60;

        return String.format(
                "Площадь: %.1f га\n" +
                        "Точек маршрута: %d\n" +
                        "Размер кадра: %.0f × %.0f м\n" +
                        "Длина маршрута: %.0f м\n" +
                        "Время полёта: ~%d мин %d сек",
                area / 10000.0,
                photoCount,
                footprintW, footprintH,
                distance,
                minutes, seconds);
    }
    private void exportGridMission(double altitude, double speed,
                                   double frontlap, double sidelap,
                                   double azimuth) {
        List<Waypoint> wpList = GridMissionHelper.generate(
                polygon, altitude, frontlap, sidelap, speed, azimuth);

        if (wpList.isEmpty()) {
            Toast.makeText(this, "Не удалось сгенерировать маршрут",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Показываем информацию о миссии
        String info = buildMissionInfo(wpList, altitude, speed, frontlap, sidelap);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Информация о миссии")
                .setMessage(info)
                .setPositiveButton("Сохранить KMZ", (dialog, which) -> {
                    saveMission(wpList);
                })
                .setNegativeButton("Отмена", null)
                .show();

        // Показываем маршрут на карте
        mMap.clear();
        com.google.android.gms.maps.model.PolygonOptions opts =
                new com.google.android.gms.maps.model.PolygonOptions()
                        .addAll(polygon)
                        .strokeColor(0xFF4CAF50)
                        .fillColor(0x334CAF50)
                        .strokeWidth(3f);
        mMap.addPolygon(opts);

        List<LatLng> routePoints = new ArrayList<>();
        for (Waypoint wp : wpList) {
            routePoints.add(new LatLng(wp.latitude, wp.longitude));
        }
        mMap.addPolyline(new PolylineOptions()
                .addAll(routePoints)
                .color(0xFFFF5722)
                .width(3f));
    }

    private void saveMission(List<Waypoint> wpList) {
        try {
            File outputDir = getExternalFilesDir(null);
            KmzGenerator generator = new KmzGenerator();
            File kmzFile = generator.generate(wpList, outputDir, "grid_mission");
            Toast.makeText(this,
                    "Сохранено " + wpList.size() + " точек: " + kmzFile.getName(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void exportWaypointMission() {
        List<Waypoint> wpList = new ArrayList<>();
        for (LatLng latLng : waypoints) {
            wpList.add(new Waypoint(
                    latLng.latitude, latLng.longitude, 50.0, 10.0));
        }
        try {
            File outputDir = getExternalFilesDir(null);
            KmzGenerator generator = new KmzGenerator();
            File kmzFile = generator.generate(wpList, outputDir, "mission");
            Toast.makeText(this, "Сохранено: " + kmzFile.getName(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}