package com.example.flybaitau;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.example.flybaitau.generator.GridMissionHelper;
import com.example.flybaitau.generator.KmzGenerator;
import com.example.flybaitau.model.CameraProfile;
import com.example.flybaitau.model.Waypoint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"NullableProblems", "MapsUtilityLibraryWarning"})
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final LatLng DEFAULT_LOCATION = new LatLng(43.2258, 76.9630);

    private GoogleMap mMap;
    private final List<LatLng> waypoints = new ArrayList<>();
    private final List<LatLng> polygon = new ArrayList<>();
    private boolean isGridMode = false;
    private TextView tvWaypointCount;
    private FusedLocationProviderClient fusedLocationClient;

    private int selectedVertexIndex = -1;

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
            tvWaypointCount.setText(isGridMode ? R.string.vertices_zero : R.string.points_zero);
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

    @SuppressWarnings("MapsUtilityLibraryWarning")
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
                    polygon.set(selectedVertexIndex, latLng);
                    selectedVertexIndex = -1;
                } else {
                    polygon.add(latLng);
                }
            } else {
                waypoints.add(latLng);
            }
            updateMap();
        });

        mMap.setOnMarkerClickListener(marker -> {
            if (isGridMode) {
                Object tag = marker.getTag();
                if (tag instanceof Integer) {
                    int index = (Integer) tag;
                    selectedVertexIndex = (selectedVertexIndex == index) ? -1 : index;
                    updateMap();
                    return true;
                }
            }
            return false;
        });

        mMap.setOnMapLongClickListener(latLng -> {
            if (isGridMode) {
                if (selectedVertexIndex >= 0) {
                    polygon.remove(selectedVertexIndex);
                    selectedVertexIndex = -1;
                } else if (!polygon.isEmpty()) {
                    polygon.remove(polygon.size() - 1);
                }
            } else if (!waypoints.isEmpty()) {
                waypoints.remove(waypoints.size() - 1);
                Toast.makeText(this, "Последняя точка удалена",
                        Toast.LENGTH_SHORT).show();
            }
            updateMap();
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

    @SuppressWarnings("MapsUtilityLibraryWarning")
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
                mMap.addPolygon(new PolygonOptions()
                        .addAll(polygon)
                        .strokeColor(0xFF4CAF50)
                        .fillColor(0x334CAF50)
                        .strokeWidth(3f));
            }
            for (int i = 0; i < polygon.size(); i++) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(polygon.get(i))
                        .title(String.format(Locale.getDefault(), "Вершина %d", i + 1))
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                i == selectedVertexIndex
                                        ? BitmapDescriptorFactory.HUE_RED
                                        : BitmapDescriptorFactory.HUE_GREEN));

                Marker marker = mMap.addMarker(markerOptions);
                if (marker != null) marker.setTag(i);
            }

            if (selectedVertexIndex >= 0) {
                tvWaypointCount.setText(String.format(Locale.getDefault(),
                        "Вершин: %d | Выбрана: %d",
                        polygon.size(), selectedVertexIndex + 1));
            } else {
                tvWaypointCount.setText(String.format(Locale.getDefault(),
                        "Вершин: %d", polygon.size()));
            }
        } else {
            for (int i = 0; i < waypoints.size(); i++) {
                mMap.addMarker(new MarkerOptions()
                        .position(waypoints.get(i))
                        .title(String.format(Locale.getDefault(), "Точка %d", i + 1)));
            }
            if (waypoints.size() > 1) {
                mMap.addPolyline(new PolylineOptions()
                        .addAll(waypoints)
                        .color(0xFF2196F3)
                        .width(5f));
            }
            tvWaypointCount.setText(String.format(Locale.getDefault(),
                    "Точек: %d", waypoints.size()));
        }
    }

    private void clearWaypoints() {
        waypoints.clear();
        polygon.clear();
        selectedVertexIndex = -1;
        if (mMap != null) mMap.clear();
        tvWaypointCount.setText(isGridMode ? R.string.vertices_zero : R.string.points_zero);
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
        android.widget.EditText etSpeed    = dialogView.findViewById(R.id.etSpeed);
        android.widget.SeekBar seekFrontlap = dialogView.findViewById(R.id.seekFrontlap);
        android.widget.SeekBar seekSidelap  = dialogView.findViewById(R.id.seekSidelap);
        android.widget.SeekBar seekAzimuth  = dialogView.findViewById(R.id.seekAzimuth);
        android.widget.TextView tvFrontlap  = dialogView.findViewById(R.id.tvFrontlap);
        android.widget.TextView tvSidelap   = dialogView.findViewById(R.id.tvSidelap);
        android.widget.TextView tvAzimuth   = dialogView.findViewById(R.id.tvAzimuth);

        seekFrontlap.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(android.widget.SeekBar s, int p, boolean f) {
                        tvFrontlap.setText(String.format(Locale.getDefault(), "%d%%", p + 10));
                    }
                    public void onStartTrackingTouch(android.widget.SeekBar s) {}
                    public void onStopTrackingTouch(android.widget.SeekBar s) {}
                });

        seekSidelap.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(android.widget.SeekBar s, int p, boolean f) {
                        tvSidelap.setText(String.format(Locale.getDefault(), "%d%%", p + 10));
                    }
                    public void onStartTrackingTouch(android.widget.SeekBar s) {}
                    public void onStopTrackingTouch(android.widget.SeekBar s) {}
                });

        seekAzimuth.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(android.widget.SeekBar s, int p, boolean f) {
                        tvAzimuth.setText(String.format(Locale.getDefault(), "%d°", p));
                    }
                    public void onStartTrackingTouch(android.widget.SeekBar s) {}
                    public void onStopTrackingTouch(android.widget.SeekBar s) {}
                });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Настройки Grid миссии")
                .setView(dialogView)
                .setPositiveButton("Сгенерировать", (dialog, which) -> {
                    double altitude = Double.parseDouble(etAltitude.getText().toString());
                    double speed    = Double.parseDouble(etSpeed.getText().toString());
                    double frontlap = (seekFrontlap.getProgress() + 10) / 100.0;
                    double sidelap  = (seekSidelap.getProgress() + 10) / 100.0;
                    double azimuth  = seekAzimuth.getProgress();
                    exportGridMission(altitude, speed, frontlap, sidelap, azimuth);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private String buildMissionInfo(List<Waypoint> wpList,
                                    double altitude, double speed) {
        double metersPerDegLat = 111320.0;
        double centerLat = 0;
        for (LatLng p : polygon) centerLat += p.latitude;
        centerLat /= polygon.size();
        double metersPerDegLon = 111320.0 * Math.cos(Math.toRadians(centerLat));

        // Площадь полигона (формула Гаусса)
        double area = 0;
        for (int i = 0; i < polygon.size(); i++) {
            int j = (i + 1) % polygon.size();
            double xi = polygon.get(i).longitude * metersPerDegLon;
            double yi = polygon.get(i).latitude  * metersPerDegLat;
            double xj = polygon.get(j).longitude * metersPerDegLon;
            double yj = polygon.get(j).latitude  * metersPerDegLat;
            area += xi * yj - xj * yi;
        }
        area = Math.abs(area) / 2.0;

        // Размер footprint (параметры Mavic 4 Pro)
        double fovRad    = Math.toRadians(CameraProfile.MAVIC_4_PRO.fovHorizontalDeg);
        double footprintW = 2 * altitude * Math.tan(fovRad / 2);
        double footprintH = footprintW / CameraProfile.MAVIC_4_PRO.aspectRatio;

        // Длина маршрута
        double distance = 0;
        for (int i = 1; i < wpList.size(); i++) {
            double dlat = (wpList.get(i).latitude  - wpList.get(i - 1).latitude)  * metersPerDegLat;
            double dlon = (wpList.get(i).longitude - wpList.get(i - 1).longitude) * metersPerDegLon;
            distance += Math.sqrt(dlat * dlat + dlon * dlon);
        }

        // Время полёта
        int minutes = (int)(distance / speed) / 60;
        int seconds = (int)(distance / speed) % 60;

        return String.format(Locale.getDefault(),
                "Площадь: %.1f га\n"
                        + "Точек маршрута: %d\n"
                        + "Размер кадра: %.0f × %.0f м\n"
                        + "Длина маршрута: %.0f м\n"
                        + "Время полёта: ~%d мин %d сек",
                area / 10000.0,
                wpList.size(),
                footprintW, footprintH,
                distance,
                minutes, seconds);
    }

    private void exportGridMission(double altitude, double speed,
                                   double frontlap, double sidelap,
                                   double azimuth) {
        List<Waypoint> wpList = GridMissionHelper.generate(
                polygon, altitude, frontlap, sidelap, speed, azimuth,
                CameraProfile.MAVIC_4_PRO);

        if (wpList.isEmpty()) {
            Toast.makeText(this, "Не удалось сгенерировать маршрут",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String info = buildMissionInfo(wpList, altitude, speed);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Информация о миссии")
                .setMessage(info)
                .setPositiveButton("Сохранить KMZ", (dialog, which) -> saveMission(wpList))
                .setNegativeButton("Отмена", null)
                .show();

        // Показываем маршрут на карте
        mMap.clear();
        mMap.addPolygon(new PolygonOptions()
                .addAll(polygon)
                .strokeColor(0xFF4CAF50)
                .fillColor(0x334CAF50)
                .strokeWidth(3f));

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
            if (outputDir == null) outputDir = getFilesDir();

            File kmzFile = new KmzGenerator().generate(wpList, outputDir, "grid_mission");

            Uri fileUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", kmzFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent,
                    String.format(Locale.getDefault(),
                            "Сохранить KMZ (%d точек)", wpList.size())));

        } catch (IOException e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void exportWaypointMission() {
        List<Waypoint> wpList = new ArrayList<>();
        for (LatLng latLng : waypoints) {
            wpList.add(new Waypoint(latLng.latitude, latLng.longitude, 50.0, 10.0));
        }
        try {
            File outputDir = getExternalFilesDir(null);
            File kmzFile = new KmzGenerator().generate(wpList, outputDir, "mission");
            Toast.makeText(this, "Сохранено: " + kmzFile.getName(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}