package com.example.gisapp1.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gisapp1.models.AppNotification;
import com.example.gisapp1.services.NotificationRepository;

import java.util.List;

public class NotificationsViewModel extends ViewModel {
    private NotificationRepository notificationRepository;
    private MutableLiveData<List<AppNotification>> notifications;

    public NotificationsViewModel() {
        notificationRepository = new NotificationRepository();
        notifications = new MutableLiveData<>();
        refreshNotifications();
    }

    public LiveData<List<AppNotification>> getNotifications() {
        return notifications;
    }

    public void refreshNotifications() {
        notificationRepository.getUserNotifications().observeForever(notificationList -> {
            notifications.setValue(notificationList);
        });
    }

    public void markNotificationAsRead(String notificationId) {
        notificationRepository.markNotificationAsRead(notificationId);
    }

    public void deleteNotification(String notificationId) {
        notificationRepository.deleteNotification(notificationId);
    }
}