package com.example.gisapp1.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.gisapp1.R;
import com.example.gisapp1.activities.MainActivity;
import com.example.gisapp1.models.AppNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseMessaging";
    private static int notificationId = 0;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Log data
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Map<String, String> data = remoteMessage.getData();

            // Check if it's a parking notification
            if (data.containsKey("parkingSpotId")) {
                String title = data.getOrDefault("title", "Parking Available");
                String body = data.getOrDefault("body", "A parking spot is available");
                String parkingSpotId = data.get("parkingSpotId");

                // Send notification with intent to view this parking spot
                sendParkingNotification(title, body, parkingSpotId);
            } else {
                // Regular notification
                sendNotification(
                        data.getOrDefault("title", "New Notification"),
                        data.getOrDefault("body", "You have a new notification")
                );
            }
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
            );
        }

        // Store notification in Firestore for in-app view
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            storeNotificationInFirestore(
                    remoteMessage.getNotification() != null ?
                            remoteMessage.getNotification().getTitle() :
                            remoteMessage.getData().getOrDefault("title", "New Notification"),
                    remoteMessage.getNotification() != null ?
                            remoteMessage.getNotification().getBody() :
                            remoteMessage.getData().getOrDefault("body", "You have a new notification"),
                    remoteMessage.getData().get("parkingSpotId")
            );
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Save the token to Firestore for the current user
        saveTokenToFirestore(token);
    }

    private void saveTokenToFirestore(String token) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            db.collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Token updated successfully"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error updating token", e));
        }
    }

    private void storeNotificationInFirestore(String title, String body, String parkingSpotId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Create notification object
        AppNotification notification = new AppNotification(
                userId,
                title,
                body,
                parkingSpotId != null ?
                        AppNotification.NotificationType.PARKING_AVAILABLE :
                        AppNotification.NotificationType.SYSTEM_MESSAGE,
                parkingSpotId
        );

        // Store in Firestore
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> Log.d(TAG, "Notification stored in Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error storing notification in Firestore", e));
    }

    private void sendNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("navigateTo", "notifications");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "parking_notifications";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Notification Channel for Android Oreo and above
        createNotificationChannel(notificationManager, channelId);

        notificationManager.notify(notificationId++, notificationBuilder.build());
    }

    private void sendParkingNotification(String title, String body, String parkingSpotId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Include parking spot ID to direct to this specific spot
        intent.putExtra("parkingSpotId", parkingSpotId);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "parking_notifications";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Notification Channel for Android Oreo and above
        createNotificationChannel(notificationManager, channelId);

        notificationManager.notify(notificationId++, notificationBuilder.build());
    }

    private void createNotificationChannel(NotificationManager notificationManager, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Parking Spot Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for available parking spots");
            channel.enableLights(true);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
    }
}