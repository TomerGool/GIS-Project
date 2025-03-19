package com.example.gisapp1.utils;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NotificationUtils {
    public static void sendAvailabilityNotification(String parkingSpotId, String message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Find users interested in this parking spot
        db.collection("parkingInterests")
                .whereEqualTo("parkingSpotId", parkingSpotId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    queryDocumentSnapshots.forEach(documentSnapshot -> {
                        String userId = documentSnapshot.getString("userId");

                        // Retrieve user's FCM token
                        db.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String fcmToken = userDoc.getString("fcmToken");

                                    // Send notification to user
                                    if (fcmToken != null) {
                                        sendFCMNotification(fcmToken, "Parking Spot Available", message);
                                    }
                                });
                    });
                });
    }

    private static void sendFCMNotification(String token, String title, String body) {
        // Implementation of sending FCM notification
        // (typically done via a server-side API or Cloud Functions)
    }
}