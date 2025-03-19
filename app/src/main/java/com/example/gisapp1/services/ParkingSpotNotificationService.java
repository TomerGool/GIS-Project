package com.example.gisapp1.services;

import android.util.Log;

import com.example.gisapp1.models.AppNotification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ParkingSpotNotificationService {
    private static final String TAG = "ParkingSpotNotificationService";
    private FirebaseFirestore db;
    private NotificationRepository notificationRepository;

    public ParkingSpotNotificationService() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
    }

    // Notify users when a parking spot becomes available
    public void checkAndNotifyAvailableParkingSpots() {
        db.collection("parkingInterests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot interestDoc : queryDocumentSnapshots) {
                        String userId = interestDoc.getString("userId");
                        String desiredLocation = interestDoc.getString("location");
                        String desiredStartTime = interestDoc.getString("startTime");
                        String desiredEndTime = interestDoc.getString("endTime");

                        // Find matching parking spots
                        findMatchingParkingSpots(userId, desiredLocation,
                                desiredStartTime, desiredEndTime);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error fetching parking interests", e));
    }

    private void findMatchingParkingSpots(String userId, String location,
                                          String startTime, String endTime) {
        db.collection("parkingSpots")
                .whereEqualTo("status", "available")
                .whereEqualTo("address", location)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot spotDoc : queryDocumentSnapshots) {
                        // Check time compatibility
                        if (isTimeCompatible(spotDoc, startTime, endTime)) {
                            createAvailabilityNotification(userId, spotDoc);
                        }
                    }
                });
    }

    private boolean isTimeCompatible(QueryDocumentSnapshot spotDoc,
                                     String desiredStartTime,
                                     String desiredEndTime) {
        String spotAvailableFrom = spotDoc.getString("availableFrom");
        String spotAvailableUntil = spotDoc.getString("availableUntil");

        // Implement time range compatibility check
        return isTimeRangeOverlap(spotAvailableFrom, spotAvailableUntil,
                desiredStartTime, desiredEndTime);
    }

    private boolean isTimeRangeOverlap(String availableFrom, String availableUntil,
                                       String desiredStartTime, String desiredEndTime) {
        // Implement time range overlap logic
        // This is a simplified example and should be enhanced
        return true; // Placeholder
    }

    private void createAvailabilityNotification(String userId,
                                                QueryDocumentSnapshot spotDoc) {
        AppNotification notification = new AppNotification(
                userId,
                "Parking Spot Available",
                "A parking spot is available at " + spotDoc.getString("address"),
                AppNotification.NotificationType.PARKING_AVAILABLE,
                spotDoc.getId()
        );

        notificationRepository.createNotification(notification);
    }
}