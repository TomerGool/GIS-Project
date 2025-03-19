package com.example.gisapp1.services;

import android.util.Log;

import com.example.gisapp1.models.AppNotification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ParkingAvailabilityNotificationService {
    private static final String TAG = "ParkingAvailabilityService";
    private FirebaseFirestore db;
    private NotificationRepository notificationRepository;
    private SimpleDateFormat timeFormat;

    public ParkingAvailabilityNotificationService() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    /**
     * Main method to check for available parking spots and notify interested renters
     * This should be called periodically (e.g., through a scheduled service)
     */
    public void checkAvailableParkingSpots() {
        // Find all active parking search criteria from renters
        db.collection("parkingSearches")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(searchCriteria -> {
                    for (QueryDocumentSnapshot searchDoc : searchCriteria) {
                        // Extract search parameters
                        String renterId = searchDoc.getString("userId");
                        String desiredLocation = searchDoc.getString("location");
                        String desiredStartTime = searchDoc.getString("startTime");
                        String desiredEndTime = searchDoc.getString("endTime");

                        // Find matching parking spots
                        findMatchingParkingSpots(renterId, desiredLocation,
                                desiredStartTime, desiredEndTime);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error fetching parking search criteria", e));
    }

    /**
     * Find parking spots that match a renter's search criteria
     */
    private void findMatchingParkingSpots(String renterId, String location,
                                          String startTime, String endTime) {
        db.collection("parkingSpots")
                .whereEqualTo("status", "available")
                .get()
                .addOnSuccessListener(parkingSpots -> {
                    for (QueryDocumentSnapshot spotDoc : parkingSpots) {
                        String spotAddress = spotDoc.getString("address");

                        // Check if location matches (with some flexibility for nearby locations)
                        if (isLocationMatching(spotAddress, location)) {
                            // Check if spot meets time requirements
                            if (isSpotTimeCompatible(spotDoc, startTime, endTime)) {
                                // Create and send notification
                                createAvailabilityNotification(renterId, spotDoc);
                            }
                        }
                    }
                });
    }

    /**
     * Check if the parking spot location matches the desired location
     * This could be enhanced with geocoding to find nearby spots
     */
    private boolean isLocationMatching(String spotAddress, String desiredLocation) {
        // Simple string matching for now
        // Could be improved with geocoding and radius search
        return spotAddress != null && desiredLocation != null &&
                spotAddress.toLowerCase().contains(desiredLocation.toLowerCase());
    }

    /**
     * Check if parking spot's availability matches user's desired time
     */
    private boolean isSpotTimeCompatible(QueryDocumentSnapshot spotDoc,
                                         String desiredStartTime,
                                         String desiredEndTime) {
        try {
            // Get spot's available times
            String spotAvailableFrom = spotDoc.getString("availableFrom");
            String spotAvailableUntil = spotDoc.getString("availableUntil");

            if (spotAvailableFrom == null || spotAvailableUntil == null ||
                    desiredStartTime == null || desiredEndTime == null) {
                return false;
            }

            // Parse times
            Date spotStart = timeFormat.parse(spotAvailableFrom);
            Date spotEnd = timeFormat.parse(spotAvailableUntil);
            Date desiredStart = timeFormat.parse(desiredStartTime);
            Date desiredEnd = timeFormat.parse(desiredEndTime);

            if (spotStart == null || spotEnd == null || desiredStart == null || desiredEnd == null) {
                return false;
            }

            // Check if desired time is within spot's available time
            return !desiredStart.before(spotStart) && !desiredEnd.after(spotEnd);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time", e);
            return false;
        }
    }

    /**
     * Create notification for an available parking spot
     */
    private void createAvailabilityNotification(String renterId,
                                                QueryDocumentSnapshot spotDoc) {
        // Create notification object
        AppNotification notification = new AppNotification(
                renterId,
                "Parking Spot Available",
                "A parking spot matching your search is now available at " +
                        spotDoc.getString("address") +
                        " from " + spotDoc.getString("availableFrom") +
                        " to " + spotDoc.getString("availableUntil"),
                AppNotification.NotificationType.PARKING_AVAILABLE,
                spotDoc.getId()
        );

        // Save notification
        notificationRepository.createNotification(notification);
    }
}