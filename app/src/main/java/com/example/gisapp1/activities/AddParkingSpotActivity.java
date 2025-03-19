package com.example.gisapp1.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.gisapp1.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import android.location.Geocoder;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class AddParkingSpotActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private EditText etAddress, etAvailability, etPrice;
    private Button btnGetLocation, btnSaveParking;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0.0, longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_parking_spot);

        initializeViews();
        setupFirebase();
        setupListeners();
    }

    private void initializeViews() {
        etAddress = findViewById(R.id.et_address);
        etAvailability = findViewById(R.id.et_availability);
        etPrice = findViewById(R.id.et_price);
        btnGetLocation = findViewById(R.id.btn_get_location);
        btnSaveParking = findViewById(R.id.btn_save_parking);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupListeners() {
        btnGetLocation.setOnClickListener(v -> requestCurrentLocation());
        btnSaveParking.setOnClickListener(v -> validateAndSaveParkingSpot());
    }

    private void requestCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, this::handleLocationSuccess)
                .addOnFailureListener(this::handleLocationFailure);
    }

    private void handleLocationSuccess(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            // Use Geocoder with modern Android location handling
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            // Use geocoding with a callback for newer Android versions
            geocoder.getFromLocation(latitude, longitude, 1, addresses -> {
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String fullAddress = address.getAddressLine(0);
                        etAddress.setText(fullAddress);
                        Toast.makeText(this, "Location successfully saved!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Could not retrieve address", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLocationFailure(Exception e) {
        Toast.makeText(this, "Location retrieval failed: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
    }

    private void validateAndSaveParkingSpot() {
        String address = etAddress.getText().toString().trim();
        String availability = etAvailability.getText().toString().trim();
        String price = etPrice.getText().toString().trim();

        // Comprehensive validation
        if (!validateInputs(address, availability, price)) return;

        Map<String, Object> parkingSpot = prepareParkingSpotData(address, availability, price);

        saveParkingSpotToFirestore(parkingSpot);
    }

    private boolean validateInputs(String address, String availability, String price) {
        boolean isValid = true;

        if (TextUtils.isEmpty(address)) {
            etAddress.setError("Address is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(availability)) {
            etAvailability.setError("Availability hours are required");
            isValid = false;
        } else if (!isValidTimeFormat(availability)) {
            etAvailability.setError("Invalid time format. Use HH:mm-HH:mm");
            isValid = false;
        }

        if (TextUtils.isEmpty(price)) {
            etPrice.setError("Price is required");
            isValid = false;
        }

        if (latitude == 0 || longitude == 0) {
            Toast.makeText(this, "Please get your location first", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidTimeFormat(String timeRange) {
        Pattern timePattern = Pattern.compile("^([01]?[0-9]|2[0-3]):([0-5][0-9])-([01]?[0-9]|2[0-3]):([0-5][0-9])$");
        return timePattern.matcher(timeRange).matches();
    }

    private Map<String, Object> prepareParkingSpotData(String address, String availability, String price) {
        String[] times = availability.split("-");
        String availableFrom = times[0].trim();
        String availableUntil = times[1].trim();

        Map<String, Object> parkingSpot = new HashMap<>();
        parkingSpot.put("address", address);
        parkingSpot.put("latitude", latitude);
        parkingSpot.put("longitude", longitude);
        parkingSpot.put("availability", availability);
        parkingSpot.put("availableFrom", availableFrom);
        parkingSpot.put("availableUntil", availableUntil);
        parkingSpot.put("price", Double.parseDouble(price));
        parkingSpot.put("status", "available");
        parkingSpot.put("ownerId", mAuth.getCurrentUser().getUid());
        parkingSpot.put("reservedBy", "");

        return parkingSpot;
    }

    private void saveParkingSpotToFirestore(Map<String, Object> parkingSpot) {
        db.collection("parkingSpots")
                .add(parkingSpot)
                .addOnSuccessListener(documentReference -> showSuccessDialog())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error adding parking spot: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Parking Spot Added")
                .setMessage("Your parking spot has been saved successfully!")
                .setPositiveButton("Dashboard", (dialog, which) -> navigateToDashboard())
                .setNegativeButton("Add Another", (dialog, which) -> resetForm())
                .setNeutralButton("Home", (dialog, which) -> finish())
                .show();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("navigateTo", "dashboard");
        startActivity(intent);
        finish();
    }

    private void resetForm() {
        etAddress.setText("");
        etAvailability.setText("");
        etPrice.setText("");
        latitude = 0;
        longitude = 0;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}