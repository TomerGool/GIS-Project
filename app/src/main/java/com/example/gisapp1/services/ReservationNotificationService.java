package com.example.gisapp1.services;

import android.util.Log;

import com.example.gisapp1.models.AppNotification;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ReservationNotificationService {
    private static final String TAG = "ReservationNotificationService";
    private FirebaseFirestore db;
    private NotificationRepository notificationRepository;
    private ListenerRegistration reservationListener;

    public ReservationNotificationService() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
    }

    public void startReservationNotificationListener() {
        reservationListener = db.collection("reservations")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed", e);
                        return;
                    }

                    for (DocumentChange documentChange : snapshots.getDocumentChanges()) {
                        switch (documentChange.getType()) {
                            case ADDED:
                                handleNewReservation(documentChange.getDocument());
                                break;
                            case MODIFIED:
                                handleReservationModification(documentChange.getDocument());
                                break;
                            case REMOVED:
                                handleReservationCancellation(documentChange.getDocument());
                                break;
                        }
                    }
                });
    }

    private void handleNewReservation(DocumentSnapshot document) {
        String userId = document.getString("userId");
        String parkingSpotId = document.getString("parkingSpotId");

        AppNotification notification = new AppNotification(
                userId,
                "Reservation Confirmed",
                "Your parking reservation has been confirmed",
                AppNotification.NotificationType.RESERVATION_CONFIRMED,
                parkingSpotId
        );

        notificationRepository.createNotification(notification);
    }

    private void handleReservationModification(DocumentSnapshot document) {
        // Handle reservation updates
    }

    private void handleReservationCancellation(DocumentSnapshot document) {
        String userId = document.getString("userId");
        String parkingSpotId = document.getString("parkingSpotId");

        AppNotification notification = new AppNotification(
                userId,
                "Reservation Cancelled",
                "Your parking reservation has been cancelled",
                AppNotification.NotificationType.RESERVATION_CANCELED,
                parkingSpotId
        );

        notificationRepository.createNotification(notification);
    }

    public void stopReservationNotificationListener() {
        if (reservationListener != null) {
            reservationListener.remove();
        }
    }
}