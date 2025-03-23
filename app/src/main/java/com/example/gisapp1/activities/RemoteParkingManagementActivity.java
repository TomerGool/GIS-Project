package com.example.gisapp1.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gisapp1.R;
import com.example.gisapp1.adapters.RemoteParkingAdapter;
import com.example.gisapp1.models.ParkingSpot;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class RemoteParkingManagementActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private RemoteParkingAdapter adapter;
    private List<ParkingSpot> parkingSpots = new ArrayList<>();
    private FloatingActionButton fabAddSpot;

    // Map related variables
    private GoogleMap mMap;
    private LatLng selectedLocation;
    private Marker currentMarker;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_parking_management);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_remote_spots);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RemoteParkingAdapter(parkingSpots, this::updateParkingSpotStatus);
        recyclerView.setAdapter(adapter);

        // Initialize add spot button
        fabAddSpot = findViewById(R.id.fab_add_spot);
        fabAddSpot.setOnClickListener(v -> showAddSpotDialog());

        // Load user's parking spots
        loadUserParkingSpots();
    }

    private void loadUserParkingSpots() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("parkingSpots")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Clear existing list
                    parkingSpots.clear();

                    // Populate list with parking spots
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String ownerId = doc.getString("ownerId");
                        String address = doc.getString("address");
                        double latitude = doc.getDouble("latitude") != null ? doc.getDouble("latitude") : 0.0;
                        double longitude = doc.getDouble("longitude") != null ? doc.getDouble("longitude") : 0.0;
                        String status = doc.getString("status");
                        String reservedBy = doc.getString("reservedBy");
                        String availableFrom = doc.getString("availableFrom");
                        String availableUntil = doc.getString("availableUntil");

                        ParkingSpot spot = new ParkingSpot(id, ownerId, address, latitude, longitude,
                                status, reservedBy, availableFrom, availableUntil);
                        parkingSpots.add(spot);
                    }

                    // Update adapter
                    adapter.notifyDataSetChanged();

                    // Show empty view if no spots
                    findViewById(R.id.text_no_spots).setVisibility(
                            parkingSpots.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading spots: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    public void updateParkingSpotStatus(String parkingSpotId, String status) {
        db.collection("parkingSpots")
                .document(parkingSpotId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Parking spot status updated to " + status,
                            Toast.LENGTH_SHORT).show();

                    // Refresh spot list
                    loadUserParkingSpots();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddSpotDialog() {
        // Create a custom dialog with a map view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_parking_with_map, null);

        // Get UI elements
        EditText etFrom = dialogView.findViewById(R.id.et_available_from);
        EditText etUntil = dialogView.findViewById(R.id.et_available_until);
        TextView tvSelectedAddress = dialogView.findViewById(R.id.tv_selected_address);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);

        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Parking Spot")
                .setView(dialogView)
                .create();

        dialog.show();

        // Getting the map fragment
        FragmentManager fm = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map_add_parking);

        if (mapFragment == null) {
            // Create a new instance if not found
            mapFragment = SupportMapFragment.newInstance();
            fm.beginTransaction()
                    .add(R.id.map_add_parking, mapFragment)
                    .commit();
            fm.executePendingTransactions();
        }

        // Setup the map
        final SupportMapFragment finalMapFragment = mapFragment;
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;

                // Set default location (Tel Aviv)
                LatLng defaultLocation = new LatLng(32.0853, 34.7818);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 14));

                // Enable user's location if permission is granted
                if (ActivityCompat.checkSelfPermission(RemoteParkingManagementActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }

                // Allow user to place a marker by tapping on the map
                mMap.setOnMapClickListener(latLng -> {
                    // Remove previous marker if exists
                    if (currentMarker != null) {
                        currentMarker.remove();
                    }

                    // Add new marker
                    currentMarker = mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("Selected Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                    // Store the selected location
                    selectedLocation = latLng;

                    // Get address from coordinates (reverse geocoding)
                    getAddressFromLocation(latLng, tvSelectedAddress);
                });
            }
        });

        SupportMapFragment finalMapFragment2 = mapFragment;
        btnCancel.setOnClickListener(v -> {
            // Cleanup if needed
            if (finalMapFragment2 != null && finalMapFragment2.isAdded()) {
                fm.beginTransaction().remove(finalMapFragment2).commit();
            }
            dialog.dismiss();
        });

        SupportMapFragment finalMapFragment1 = mapFragment;
        btnAdd.setOnClickListener(v -> {
            String availableFrom = etFrom.getText().toString().trim();
            String availableUntil = etUntil.getText().toString().trim();

            // Validate inputs
            if (selectedLocation == null) {
                Toast.makeText(RemoteParkingManagementActivity.this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!validateTimeInputs(etFrom, etUntil)) {
                return;
            }

            // Get the selected address or use coordinates description
            String addressText = tvSelectedAddress.getText().toString();
            if (addressText.isEmpty() || addressText.equals("Select a location on the map")) {
                addressText = "Location at " + selectedLocation.latitude + ", " + selectedLocation.longitude;
            }

            // Add the parking spot
            addRemoteParkingSpot(addressText, availableFrom, availableUntil,
                    selectedLocation.latitude, selectedLocation.longitude);

            // Cleanup if needed
            if (finalMapFragment1 != null && finalMapFragment1.isAdded()) {
                fm.beginTransaction().remove(finalMapFragment1).commit();
            }
            dialog.dismiss();
        });
    }

    // Method to get address from coordinates
    private void getAddressFromLocation(LatLng latLng, TextView addressDisplay) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                addressDisplay.setText(addressText);
            } else {
                addressDisplay.setText("Location selected (No address found)");
            }
        } catch (IOException e) {
            addressDisplay.setText("Location selected (Coordinates: " +
                    latLng.latitude + ", " + latLng.longitude + ")");
            Log.e("GeoCodingError", "Error getting address", e);
        }
    }

    // Validate time inputs
    private boolean validateTimeInputs(EditText etFrom, EditText etUntil) {
        String availableFrom = etFrom.getText().toString().trim();
        String availableUntil = etUntil.getText().toString().trim();

        if (availableFrom.isEmpty()) {
            etFrom.setError("Start time is required");
            return false;
        } else if (!isValidTimeFormat(availableFrom)) {
            etFrom.setError("Invalid time format. Use HH:mm");
            return false;
        }

        if (availableUntil.isEmpty()) {
            etUntil.setError("End time is required");
            return false;
        } else if (!isValidTimeFormat(availableUntil)) {
            etUntil.setError("Invalid time format. Use HH:mm");
            return false;
        }

        // Additional time validation
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date startTime = timeFormat.parse(availableFrom);
            Date endTime = timeFormat.parse(availableUntil);

            if (startTime != null && endTime != null && !endTime.after(startTime)) {
                etUntil.setError("End time must be after start time");
                return false;
            }
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    private boolean isValidTimeFormat(String time) {
        Pattern timePattern = Pattern.compile("^([01]?[0-9]|2[0-3]):([0-5][0-9])$");
        return timePattern.matcher(time).matches();
    }

    private void addRemoteParkingSpot(String address, String availableFrom, String availableUntil,
                                      double latitude, double longitude) {
        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> spotData = new HashMap<>();
        spotData.put("ownerId", userId);
        spotData.put("address", address);
        spotData.put("availableFrom", availableFrom);
        spotData.put("availableUntil", availableUntil);
        spotData.put("status", "available");
        spotData.put("reservedBy", "");
        spotData.put("latitude", latitude);
        spotData.put("longitude", longitude);

        db.collection("parkingSpots")
                .add(spotData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Parking spot added successfully",
                            Toast.LENGTH_SHORT).show();
                    loadUserParkingSpots();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding spot: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try to enable location on map
                if (mMap != null && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Location permission is required for better experience",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}