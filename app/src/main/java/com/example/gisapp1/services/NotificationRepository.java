package com.example.gisapp1.services;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gisapp1.models.AppNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class NotificationRepository {
    private static final String TAG = "NotificationRepository";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public NotificationRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void createNotification(AppNotification notification) {
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference ->
                        Log.d(TAG, "Notification created with ID: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error creating notification", e));
    }

    public LiveData<List<AppNotification>> getUserNotifications() {
        MutableLiveData<List<AppNotification>> notificationsLiveData = new MutableLiveData<>();

        String userId = auth.getCurrentUser() != null ?
                auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            return notificationsLiveData;
        }

        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error fetching notifications", e);
                        return;
                    }

                    List<AppNotification> notifications = queryDocumentSnapshots.toObjects(AppNotification.class);
                    notificationsLiveData.setValue(notifications);
                });

        return notificationsLiveData;
    }

    public void markNotificationAsRead(String notificationId) {
        db.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Notification marked as read"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error marking notification as read", e));
    }

    public void deleteNotification(String notificationId) {
        db.collection("notifications")
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Notification deleted"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error deleting notification", e));
    }
}