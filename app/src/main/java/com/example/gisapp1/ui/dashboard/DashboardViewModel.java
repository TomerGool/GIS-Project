package com.example.gisapp1.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gisapp1.models.ParkingSpot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<List<ParkingSpot>> myReservations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ParkingSpot>> myParkingSpots = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public DashboardViewModel() {
        loadMyData();
    }

    private void loadMyData() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            statusMessage.setValue("Please log in to view your dashboard");
            return;
        }

        // Load my reservations
        db.collection("parkingSpots")
                .whereEqualTo("reservedBy", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        statusMessage.setValue("Error loading reservations: " + error.getMessage());
                        return;
                    }

                    List<ParkingSpot> spots = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            String id = document.getId();
                            String ownerId = document.getString("ownerId");
                            String address = document.getString("address");
                            double latitude = document.getDouble("latitude");
                            double longitude = document.getDouble("longitude");
                            String status = document.getString("status");
                            String reservedBy = document.getString("reservedBy");
                            String availableFrom = document.getString("availableFrom");
                            String availableUntil = document.getString("availableUntil");

                            ParkingSpot spot = new ParkingSpot(id, ownerId, address, latitude, longitude,
                                    status, reservedBy, availableFrom, availableUntil);
                            spots.add(spot);
                        }
                    }
                    myReservations.setValue(spots);
                });

        // Load parking spots I own
        db.collection("parkingSpots")
                .whereEqualTo("ownerId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        statusMessage.setValue("Error loading your parking spots: " + error.getMessage());
                        return;
                    }

                    List<ParkingSpot> spots = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            String id = document.getId();
                            String ownerId = document.getString("ownerId");
                            String address = document.getString("address");
                            double latitude = document.getDouble("latitude");
                            double longitude = document.getDouble("longitude");
                            String status = document.getString("status");
                            String reservedBy = document.getString("reservedBy");
                            String availableFrom = document.getString("availableFrom");
                            String availableUntil = document.getString("availableUntil");

                            ParkingSpot spot = new ParkingSpot(id, ownerId, address, latitude, longitude,
                                    status, reservedBy, availableFrom, availableUntil);
                            spots.add(spot);
                        }
                    }
                    myParkingSpots.setValue(spots);
                });
    }

    public LiveData<List<ParkingSpot>> getMyReservations() {
        return myReservations;
    }

    public LiveData<List<ParkingSpot>> getMyParkingSpots() {
        return myParkingSpots;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }
}