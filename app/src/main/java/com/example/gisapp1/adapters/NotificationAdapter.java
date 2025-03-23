package com.example.gisapp1.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gisapp1.R;
import com.example.gisapp1.activities.MainActivity;
import com.example.gisapp1.activities.ParkingSearchCriteriaActivity;
import com.example.gisapp1.models.AppNotification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<AppNotification> notifications;
    private OnNotificationActionListener actionListener;

    public interface OnNotificationActionListener {
        void onMarkAsRead(AppNotification notification);

        void onDelete(AppNotification notification);

        void onViewParkingSpot(AppNotification notification);

        void onContactRenter(AppNotification notification, String renterPhone);
    }

    public NotificationAdapter(List<AppNotification> notifications,
                               OnNotificationActionListener actionListener) {
        this.notifications = notifications;
        this.actionListener = actionListener;
    }

    public List<AppNotification> getNotifications() {
        return notifications;
    }

    public void updateNotifications(List<AppNotification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification notification = notifications.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, messageTextView, timestampTextView;
        ImageButton readButton, deleteButton;
        Button contactButton;
        View itemView;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            titleTextView = itemView.findViewById(R.id.text_notification_title);
            messageTextView = itemView.findViewById(R.id.text_notification_message);
            timestampTextView = itemView.findViewById(R.id.text_notification_timestamp);
            readButton = itemView.findViewById(R.id.button_mark_read);
            deleteButton = itemView.findViewById(R.id.button_delete);
            contactButton = itemView.findViewById(R.id.button_contact_renter);
        }

        void bind(AppNotification notification) {
            titleTextView.setText(notification.getTitle());
            messageTextView.setText(notification.getMessage());

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            timestampTextView.setText(sdf.format(notification.getTimestamp()));

            // Set the click listeners for buttons
            readButton.setOnClickListener(v -> actionListener.onMarkAsRead(notification));
            deleteButton.setOnClickListener(v -> {
                // Explicitly call delete handler
                if (actionListener != null) {
                    actionListener.onDelete(notification);
                }
            });

            // Handle contact/reservation button for different notification types
            if (notification.getExtraData() != null) {
                switch (notification.getType()) {
                    case SPOT_BOOKED:
                        handleSpotBookedNotification(notification);
                        break;
                    case PARKING_AVAILABLE:
                        handleParkingAvailableNotification(notification);
                        break;
                    default:
                        contactButton.setVisibility(View.GONE);
                }
            } else {
                contactButton.setVisibility(View.GONE);
            }
        }

        private void handleSpotBookedNotification(AppNotification notification) {
            String renterName = notification.getExtraData("renterName");
            String renterPhone = notification.getExtraData("renterPhone");
            String renterEmail = notification.getExtraData("renterEmail");

            if (renterPhone != null && !renterPhone.isEmpty()) {
                contactButton.setVisibility(View.VISIBLE);
                contactButton.setText("Contact " + (renterName != null ? renterName : "Renter"));

                contactButton.setOnClickListener(v -> {
                    new AlertDialog.Builder(itemView.getContext())
                            .setTitle("Contact Renter")
                            .setItems(new String[]{"Call", "WhatsApp", "Email"}, (dialog, which) -> {
                                Intent intent;
                                switch (which) {
                                    case 0: // Call
                                        intent = new Intent(Intent.ACTION_DIAL,
                                                Uri.parse("tel:" + renterPhone));
                                        itemView.getContext().startActivity(intent);
                                        break;
                                    case 1: // WhatsApp
                                        // Format phone number for WhatsApp (ensure it has country code)
                                        String formattedPhone = formatPhoneForWhatsApp(renterPhone);
                                        String whatsappUrl = "https://wa.me/" + formattedPhone;
                                        intent = new Intent(Intent.ACTION_VIEW,
                                                Uri.parse(whatsappUrl));
                                        itemView.getContext().startActivity(intent);
                                        break;
                                    case 2: // Email
                                        intent = new Intent(Intent.ACTION_SENDTO,
                                                Uri.parse("mailto:" + renterEmail));
                                        itemView.getContext().startActivity(intent);
                                        break;
                                }
                            })
                            .show();
                });
            } else {
                contactButton.setVisibility(View.GONE);
            }
        }

        /**
         * Format phone number for WhatsApp by ensuring it has country code
         * @param phone The phone number to format
         * @return Formatted phone number for WhatsApp
         */
        private String formatPhoneForWhatsApp(String phone) {
            // Remove any non-digit characters
            String digits = phone.replaceAll("\\D", "");

            // If the number doesn't start with a country code (assuming Israel +972 as default)
            if (digits.startsWith("0")) {
                // Replace leading 0 with 972 (Israel country code)
                return "972" + digits.substring(1);
            } else if (!digits.startsWith("972") && !digits.startsWith("+")) {
                // If no country code and doesn't start with 0, add 972
                return "972" + digits;
            } else if (digits.startsWith("+")) {
                // Remove the + if it exists (WhatsApp URLs don't use +)
                return digits.substring(1);
            }

            // Return as is if already has country code
            return digits;
        }

        private void handleParkingAvailableNotification(AppNotification notification) {
            contactButton.setVisibility(View.VISIBLE);
            contactButton.setText("Reserve Spot");
            contactButton.setOnClickListener(v -> {
                // Get parking spot details from notification
                String parkingSpotId = notification.getRelatedEntityId();
                String location = notification.getExtraData("spotAddress");
                String availableFrom = notification.getExtraData("availableFrom");
                String availableUntil = notification.getExtraData("availableUntil");

                // Open parking spot details dialog
                if (itemView.getContext() instanceof MainActivity) {
                    ((MainActivity) itemView.getContext()).loadAndShowParkingSpot(parkingSpotId);
                } else {
                    // Fallback: Navigate to Parking Search Criteria with pre-filled data
                    Intent intent = new Intent(itemView.getContext(), ParkingSearchCriteriaActivity.class);
                    intent.putExtra("location", location);
                    intent.putExtra("startTime", availableFrom);
                    intent.putExtra("endTime", availableUntil);
                    itemView.getContext().startActivity(intent);
                }
            });
        }
    }
}