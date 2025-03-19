package com.example.gisapp1.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.gisapp1.R;
import com.example.gisapp1.dialogs.ParkingSpotDetailsDialog;
import com.example.gisapp1.models.ParkingSpot;
import com.example.gisapp1.services.NotificationSchedulerService;
import com.example.gisapp1.services.ParkingAvailabilityNotificationService;
import com.example.gisapp1.ui.dashboard.DashboardFragment;
import com.example.gisapp1.ui.home.HomeFragment;
import com.example.gisapp1.ui.notifications.NotificationsFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<ParkingSpot> parkingSpots = new ArrayList<>();
    private Map<String, Marker> markersMap = new HashMap<>();
    private boolean mapReady = false;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private ParkingAvailabilityNotificationService notificationService;
    public BottomNavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize notification service
        notificationService = new ParkingAvailabilityNotificationService();

        // Set up bottom navigation
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView = findViewById(R.id.nav_view);

        // Initialize map fragment
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        // Navigation listener
        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                // Hide map when showing home fragment
                if (mapFragment.getView() != null) {
                    mapFragment.getView().setVisibility(View.GONE);
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, new HomeFragment())
                        .commit();
                return true;
            } else if (itemId == R.id.navigation_dashboard) {
                // Hide map when showing dashboard fragment
                if (mapFragment.getView() != null) {
                    mapFragment.getView().setVisibility(View.GONE);
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, new DashboardFragment())
                        .commit();
                return true;
            } else if (itemId == R.id.navigation_notifications) {
                // Hide map when showing notifications fragment
                if (mapFragment.getView() != null) {
                    mapFragment.getView().setVisibility(View.GONE);
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, new NotificationsFragment())
                        .commit();
                return true;
            }
            return false;
        });

        // Check if we need to navigate to a specific destination
        if (getIntent().hasExtra("navigateTo")) {
            String destination = getIntent().getStringExtra("navigateTo");
            if ("dashboard".equals(destination)) {
                navView.setSelectedItemId(R.id.navigation_dashboard);
            } else if ("notifications".equals(destination)) {
                navView.setSelectedItemId(R.id.navigation_notifications);
            }
        } else {
            // By default, start with home fragment
            navView.setSelectedItemId(R.id.navigation_home);
        }

        // Check if we should show a specific parking spot
        if (getIntent().hasExtra("parkingSpotId")) {
            String parkingSpotId = getIntent().getStringExtra("parkingSpotId");
            loadAndShowParkingSpot(parkingSpotId);
        }

        // Initialize map
        mapFragment.getMapAsync(this);

        // Start the notification scheduler service
        startService(new Intent(this, NotificationSchedulerService.class));

        // Immediately check for available parking spots
        notificationService.checkAvailableParkingSpots();
    }


    // Method to load and show a specific parking spot
    public void loadAndShowParkingSpot(String parkingSpotId) {
        // Show map view
        if (mapFragment.getView() != null) {
            mapFragment.getView().setVisibility(View.VISIBLE);
        }

        // Ensure map is initialized
        if (mMap == null) {
            mapFragment.getMapAsync(googleMap -> {
                mMap = googleMap;
                fetchAndShowParkingSpot(parkingSpotId);
            });
        } else {
            fetchAndShowParkingSpot(parkingSpotId);
        }
    }

    private void fetchAndShowParkingSpot(String parkingSpotId) {
        // Fetch parking spot data from Firestore
        db.collection("parkingSpots").document(parkingSpotId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Create parking spot object
                        Double lat = documentSnapshot.getDouble("latitude");
                        Double lon = documentSnapshot.getDouble("longitude");

                        // If lat/lon is not available, use default location
                        if (lat == null || lon == null) {
                            Toast.makeText(this, "Parking spot location not available", Toast.LENGTH_SHORT).show();
                            lat = 32.0853; // Default Tel Aviv latitude
                            lon = 34.7818; // Default Tel Aviv longitude
                        }

                        ParkingSpot spot = new ParkingSpot(
                                documentSnapshot.getId(),
                                documentSnapshot.getString("ownerId"),
                                documentSnapshot.getString("address"),
                                lat, lon,
                                documentSnapshot.getString("status"),
                                documentSnapshot.getString("reservedBy"),
                                documentSnapshot.getString("availableFrom"),
                                documentSnapshot.getString("availableUntil")
                        );

                        // Clear previous markers
                        mMap.clear();

                        // Center map on this spot
                        LatLng spotLocation = new LatLng(lat, lon);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(spotLocation, 16));

                        // Add marker for this spot
                        addParkingSpotMarker(spot);

                        // Show details dialog
                        showParkingSpotDetails(spot);
                    } else {
                        Toast.makeText(this, "Parking spot not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading parking spot: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Method to manually trigger parking spot availability check
    public void checkForAvailableSpots() {
        notificationService.checkAvailableParkingSpots();
        Toast.makeText(this, "Checking for available parking spots...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapReady = true;

        // Set default location (Tel Aviv)
        LatLng defaultLocation = new LatLng(32.0853, 34.7818);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14));

        // Set up click listener for markers
        mMap.setOnMarkerClickListener(marker -> {
            ParkingSpot spot = (ParkingSpot) marker.getTag();
            if (spot != null) {
                showParkingSpotDetails(spot);
            }
            return true;
        });
    }

    public void showMapWithCurrentLocation() {
        // Remove any current fragment from the container
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        getSupportFragmentManager().getFragments().forEach(fragment -> {
            if (fragment != mapFragment && fragment.getId() == R.id.nav_host_fragment_activity_main) {
                transaction.remove(fragment);
            }
        });
        transaction.commit();

        // Show the map fragment
        if (mapFragment.getView() != null) {
            mapFragment.getView().setVisibility(View.VISIBLE);
        }

        // Check and request location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Enable my location layer
        mMap.setMyLocationEnabled(true);

        // Get current location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        // Clear previous markers
                        mMap.clear();

                        // Move camera and zoom to current location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

                        // Setup parking spots near current location
                        setupNearbyParkingSpots(currentLocation);
                    } else {
                        Toast.makeText(this, "Could not retrieve location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupNearbyParkingSpots(LatLng currentLocation) {
        db.collection("parkingSpots")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error fetching parking spots", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            Double lat = document.getDouble("latitude");
                            Double lon = document.getDouble("longitude");

                            if (lat != null && lon != null) {
                                LatLng spotLocation = new LatLng(lat, lon);

                                // Calculate distance
                                float[] distance = new float[1];
                                android.location.Location.distanceBetween(
                                        currentLocation.latitude, currentLocation.longitude,
                                        spotLocation.latitude, spotLocation.longitude,
                                        distance
                                );

                                // If spot is within 2000 meters (2 km)
                                if (distance[0] <= 2000) {
                                    // Create parking spot and add marker
                                    ParkingSpot spot = new ParkingSpot(
                                            document.getId(),
                                            document.getString("ownerId"),
                                            document.getString("address"),
                                            lat, lon,
                                            document.getString("status"),
                                            document.getString("reservedBy"),
                                            document.getString("availableFrom"),
                                            document.getString("availableUntil")
                                    );

                                    addParkingSpotMarker(spot);
                                }
                            }
                        }
                    }
                });
    }

    private void addParkingSpotMarker(ParkingSpot spot) {
        if (mMap == null) return;

        LatLng location = new LatLng(spot.getLatitude(), spot.getLongitude());

        // Set marker color based on status (green for available, red for occupied)
        BitmapDescriptor markerIcon;
        if ("available".equals(spot.getStatus())) {
            markerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        } else {
            markerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        }

        // Create and add the marker
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(spot.getAddress())
                .snippet("Available: " + spot.getAvailableFrom() + " to " + spot.getAvailableUntil())
                .icon(markerIcon));

        // Store the parking spot object with the marker for later retrieval
        if (marker != null) {
            marker.setTag(spot);
            markersMap.put(spot.getId(), marker);
        }
    }

    private void showParkingSpotDetails(ParkingSpot spot) {
        // Create a dialog to show parking spot details
        ParkingSpotDetailsDialog dialog = new ParkingSpotDetailsDialog(spot);
        dialog.show(getSupportFragmentManager(), "ParkingSpotDetails");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try to show location again
                showMapWithCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void navigateToRemoteManagement() {
        Intent intent = new Intent(this, RemoteParkingManagementActivity.class);
        startActivity(intent);
    }
}