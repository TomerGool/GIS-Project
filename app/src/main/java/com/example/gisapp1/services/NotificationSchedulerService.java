package com.example.gisapp1.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationSchedulerService extends Service {
    private ScheduledExecutorService scheduler;
    private ParkingAvailabilityNotificationService parkingAvailabilityNotificationService;
    private ReservationNotificationService reservationNotificationService;

    @Override
    public void onCreate() {
        super.onCreate();
        parkingAvailabilityNotificationService = new ParkingAvailabilityNotificationService();
        reservationNotificationService = new ReservationNotificationService();

        scheduler = Executors.newScheduledThreadPool(2);

        // Check parking spot availability every 15 minutes
        scheduler.scheduleWithFixedDelay(
                () -> parkingAvailabilityNotificationService.checkAvailableParkingSpots(),
                0, 15, TimeUnit.MINUTES
        );

        // Start real-time reservation listener
        reservationNotificationService.startReservationNotificationListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdown();
        }
        reservationNotificationService.stopReservationNotificationListener();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}