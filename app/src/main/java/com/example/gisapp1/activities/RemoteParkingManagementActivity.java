package com.example.gisapp1.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gisapp1.R;
import com.example.gisapp1.adapters.RemoteParkingAdapter;
import com.example.gisapp1.models.ParkingSpot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteParkingManagementActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private RemoteParkingAdapter adapter;
    private List<ParkingSpot> parkingSpots = new ArrayList<>();
    private FloatingActionButton fabAddSpot;

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
                    Toast.makeText(this, "Parking spot status updated",
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_remote_spot, null);
        EditText etAddress = dialogView.findViewById(R.id.et_address);
        EditText etFrom = dialogView.findViewById(R.id.et_available_from);
        EditText etUntil = dialogView.findViewById(R.id.et_available_until);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Remote Parking Spot")
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String address = etAddress.getText().toString().trim();
            String availableFrom = etFrom.getText().toString().trim();
            String availableUntil = etUntil.getText().toString().trim();

            if (address.isEmpty()) {
                etAddress.setError("Address is required");
                return;
            }

            if (availableFrom.isEmpty()) {
                etFrom.setError("Start time is required");
                return;
            }

            if (availableUntil.isEmpty()) {
                etUntil.setError("End time is required");
                return;
            }

            addRemoteParkingSpot(address, availableFrom, availableUntil);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addRemoteParkingSpot(String address, String availableFrom, String availableUntil) {
        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> spotData = new HashMap<>();
        spotData.put("ownerId", userId);
        spotData.put("address", address);
        spotData.put("availableFrom", availableFrom);
        spotData.put("availableUntil", availableUntil);
        spotData.put("status", "available");
        spotData.put("reservedBy", "");

        // Default location - could be enhanced with geocoding
        spotData.put("latitude", 0.0);
        spotData.put("longitude", 0.0);

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
}