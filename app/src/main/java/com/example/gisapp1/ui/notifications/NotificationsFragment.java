package com.example.gisapp1.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gisapp1.R;
import com.example.gisapp1.activities.MainActivity;
import com.example.gisapp1.activities.ParkingSearchCriteriaActivity;
import com.example.gisapp1.adapters.NotificationAdapter;
import com.example.gisapp1.databinding.FragmentNotificationsBinding;
import com.example.gisapp1.models.AppNotification;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private NotificationsViewModel viewModel;
    private NotificationAdapter adapter;
    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = root.findViewById(R.id.recycler_notifications);
        TextView emptyView = root.findViewById(R.id.text_no_notifications);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(
                new ArrayList<>(),
                new NotificationAdapter.OnNotificationActionListener() {
                    @Override
                    public void onMarkAsRead(AppNotification notification) {
                        viewModel.markNotificationAsRead(notification.getId());
                        // Refresh notifications list after marking as read
                        refreshNotifications();
                    }

                    @Override
                    public void onDelete(AppNotification notification) {
                        // Show confirmation toast and delete the notification
                        Toast.makeText(getContext(), "Notification deleted", Toast.LENGTH_SHORT).show();
                        viewModel.deleteNotification(notification.getId());
                        // Remove the notification from the adapter's list
                        List<AppNotification> currentList = new ArrayList<>(adapter.getNotifications());
                        currentList.remove(notification);
                        adapter.updateNotifications(currentList);
                    }

                    @Override
                    public void onViewParkingSpot(AppNotification notification) {
                        // Check notification type and handle accordingly
                        if (notification.getType() == AppNotification.NotificationType.PARKING_AVAILABLE) {
                            // Navigate to Parking Search Criteria Activity
                            Intent intent = new Intent(getActivity(), ParkingSearchCriteriaActivity.class);

                            // Pass search criteria from notification
                            if (notification.getExtraData() != null) {
                                intent.putExtra("location", notification.getExtraData("location"));
                                intent.putExtra("startTime", notification.getExtraData("startTime"));
                                intent.putExtra("endTime", notification.getExtraData("endTime"));
                            }

                            startActivity(intent);
                        } else if (notification.getRelatedEntityId() != null &&
                                notification.getType() == AppNotification.NotificationType.SPOT_BOOKED) {

                            if (getActivity() instanceof MainActivity) {
                                MainActivity mainActivity = (MainActivity) getActivity();

                                // Use BottomNavigationView to set selected item
                                BottomNavigationView navView = mainActivity.findViewById(R.id.nav_view);
                                if (navView != null) {
                                    navView.setSelectedItemId(R.id.navigation_notifications);
                                }

                                // Load and show parking spot
                                mainActivity.loadAndShowParkingSpot(notification.getRelatedEntityId());
                            }
                        }
                    }
                    @Override
                    public void onContactRenter(AppNotification notification, String renterPhone) {
                        // This is handled directly in the adapter with the AlertDialog
                    }
                }
        );
        recyclerView.setAdapter(adapter);

        // Observe notifications data
        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            adapter.updateNotifications(notifications);
            emptyView.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
        });

        return root;
    }

    private void refreshNotifications() {
        viewModel.refreshNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}