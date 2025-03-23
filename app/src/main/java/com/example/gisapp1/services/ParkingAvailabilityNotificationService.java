package com.example.gisapp1.services;

import android.location.Location;
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
                                desiredStartTime, desiredEndTime, searchDoc);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error fetching parking search criteria", e));
    }

    /**
     * Find parking spots that match a renter's search criteria
     */
    private void findMatchingParkingSpots(String renterId, String location,
                                          String startTime, String endTime,
                                          QueryDocumentSnapshot searchDoc) {
        db.collection("parkingSpots")
                .whereEqualTo("status", "available")
                .get()
                .addOnSuccessListener(parkingSpots -> {
                    for (QueryDocumentSnapshot spotDoc : parkingSpots) {
                        String spotAddress = spotDoc.getString("address");

                        // Get coordinates (with default values if not available)
                        double spotLat = spotDoc.contains("latitude") ? spotDoc.getDouble("latitude") : 0;
                        double spotLon = spotDoc.contains("longitude") ? spotDoc.getDouble("longitude") : 0;

                        // Get search coordinates (with default values if not available)
                        double searchLat = searchDoc.contains("latitude") ? searchDoc.getDouble("latitude") : 0;
                        double searchLon = searchDoc.contains("longitude") ? searchDoc.getDouble("longitude") : 0;

                        // Check if location matches (with some flexibility for nearby locations)
                        if (isLocationMatching(spotAddress, location, spotLat, spotLon, searchLat, searchLon)) {
                            // Check if spot meets time requirements
                            if (isSpotTimeCompatible(spotDoc, startTime, endTime)) {
                                // Create and send notification
                                createAvailabilityNotification(renterId, spotDoc, searchDoc);
                            }
                        }
                    }
                });
    }

    /**
     * Check if the parking spot location matches the desired location
     * This could be enhanced with geocoding to find nearby spots
     */
    private boolean isLocationMatching(String spotAddress, String desiredLocation,
                                       double spotLat, double spotLon,
                                       double searchLat, double searchLon) {
        // First, do a text-based matching
        boolean textMatch = spotAddress != null && desiredLocation != null &&
                spotAddress.toLowerCase().contains(desiredLocation.toLowerCase());

        // Then do a geographic proximity check
        if (textMatch && searchLat != 0 && searchLon != 0) {
            float[] distance = new float[1];
            Location.distanceBetween(searchLat, searchLon, spotLat, spotLon, distance);

            // Check if spot is within 5 kilometers
            return distance[0] <= 5000;
        }

        return textMatch;
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
                                                QueryDocumentSnapshot spotDoc,
                                                QueryDocumentSnapshot searchDoc) {
        // Get spot details
        String spotAddress = spotDoc.getString("address");
        String availableFrom = spotDoc.getString("availableFrom");
        String availableUntil = spotDoc.getString("availableUntil");

        // Get original search criteria details
        String searchLocation = searchDoc.getString("location");
        String searchStartTime = searchDoc.getString("startTime");
        String searchEndTime = searchDoc.getString("endTime");

        // Create notification object
        AppNotification notification = new AppNotification(
                renterId,
                "Parking Spot Available",
                "A parking spot matching your search is now available at " +
                        spotAddress +
                        " from " + availableFrom +
                        " to " + availableUntil,
                AppNotification.NotificationType.PARKING_AVAILABLE,
                spotDoc.getId()
        );

        // Add extra data for more context
        notification.addExtraData("location", searchLocation);
        notification.addExtraData("startTime", searchStartTime);
        notification.addExtraData("endTime", searchEndTime);

        // Additional spot details
        notification.addExtraData("spotAddress", spotAddress);
        notification.addExtraData("availableFrom", availableFrom);
        notification.addExtraData("availableUntil", availableUntil);

        // Optional: Add more details if available
        if (spotDoc.contains("price")) {
            notification.addExtraData("price", String.valueOf(spotDoc.getDouble("price")));
        }

        // Save notification
        notificationRepository.createNotification(notification);
    }
}