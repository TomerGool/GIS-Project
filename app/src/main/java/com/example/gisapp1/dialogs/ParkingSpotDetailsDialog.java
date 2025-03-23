package com.example.gisapp1.dialogs;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.gisapp1.R;
import com.example.gisapp1.models.AppNotification;
import com.example.gisapp1.models.ParkingSpot;
import com.example.gisapp1.models.Reservation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ParkingSpotDetailsDialog extends DialogFragment {

    private final ParkingSpot parkingSpot;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Components
    private TextView tvAddress, tvAvailability, tvStatus, tvPrice;
    private Button btnSelectStart, btnSelectEnd, btnReserve, btnContactOwner;
    private EditText etNotes;

    // Date and Time
    private Calendar startDateTime, endDateTime;
    private static final double HOURLY_RATE = 10.0; // Example rate, can be dynamic

    public ParkingSpotDetailsDialog(ParkingSpot parkingSpot) {
        this.parkingSpot = parkingSpot;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_parking_spot_details, null);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI Components
        tvAddress = view.findViewById(R.id.tv_address);
        tvAvailability = view.findViewById(R.id.tv_availability);
        tvStatus = view.findViewById(R.id.tv_status);
        tvPrice = view.findViewById(R.id.tv_price);
        btnSelectStart = view.findViewById(R.id.btn_select_start);
        btnSelectEnd = view.findViewById(R.id.btn_select_end);
        btnReserve = view.findViewById(R.id.btn_reserve);
        btnContactOwner = view.findViewById(R.id.btn_contact_owner);
        etNotes = view.findViewById(R.id.et_reservation_notes);

        // Set initial values
        tvAddress.setText(parkingSpot.getAddress());
        tvAvailability.setText(String.format("Available: %s to %s",
                parkingSpot.getAvailableFrom(), parkingSpot.getAvailableUntil()));
        tvStatus.setText(String.format("Status: %s", parkingSpot.getStatus()));

        // Initialize date selections
        startDateTime = Calendar.getInstance();
        endDateTime = Calendar.getInstance();

        // Date and Time Selection Listeners
        btnSelectStart.setOnClickListener(v -> showDateTimePicker(true));
        btnSelectEnd.setOnClickListener(v -> showDateTimePicker(false));

        // Reservation Button
        btnReserve.setOnClickListener(v -> checkAndReserveParkingSpot());

        // Contact Owner Button
        btnContactOwner.setOnClickListener(v -> setupContactOptions());

        // Show/hide contact button based on whether this is the owner
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId != null && !currentUserId.equals(parkingSpot.getOwnerId())) {
            btnContactOwner.setVisibility(View.VISIBLE);
        } else {
            btnContactOwner.setVisibility(View.GONE);
        }

        // Disable reserve button if spot is not available
        btnReserve.setEnabled("available".equals(parkingSpot.getStatus()));

        builder.setView(view)
                .setTitle("Parking Spot Reservation")
                .setNegativeButton("Close", (dialog, id) -> dialog.dismiss());

        return builder.create();
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

    private void setupContactOptions() {
        // Fetch owner's contact details
        db.collection("users").document(parkingSpot.getOwnerId())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String phone = userDoc.getString("phone");
                    if (phone == null || phone.isEmpty()) {
                        Toast.makeText(getContext(), "Owner contact information is not available", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new AlertDialog.Builder(getContext())
                            .setTitle("Contact Parking Spot Owner")
                            .setItems(new String[]{"Call", "WhatsApp"}, (dialog, which) -> {
                                Intent intent;
                                if (which == 0) { // Call
                                    intent = new Intent(Intent.ACTION_DIAL,
                                            Uri.parse("tel:" + phone));
                                } else { // WhatsApp
                                    String formattedPhone = formatPhoneForWhatsApp(phone);
                                    String whatsappUrl = "https://wa.me/" + formattedPhone;
                                    intent = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(whatsappUrl));
                                }
                                startActivity(intent);
                            })
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load owner information: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showDateTimePicker(boolean isStartTime) {
        Calendar currentDateTime = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, dayOfMonth) -> {
                    // Set selected date
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    // Show time picker
                    new TimePickerDialog(
                            requireContext(),
                            (timePicker, hourOfDay, minute) -> {
                                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedDate.set(Calendar.MINUTE, minute);

                                if (isStartTime) {
                                    startDateTime = selectedDate;
                                    btnSelectStart.setText(formatDateTime(startDateTime.getTime()));
                                } else {
                                    endDateTime = selectedDate;
                                    btnSelectEnd.setText(formatDateTime(endDateTime.getTime()));
                                }

                                updatePriceEstimate();
                            },
                            currentDateTime.get(Calendar.HOUR_OF_DAY),
                            currentDateTime.get(Calendar.MINUTE),
                            true
                    ).show();
                },
                currentDateTime.get(Calendar.YEAR),
                currentDateTime.get(Calendar.MONTH),
                currentDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updatePriceEstimate() {
        if (startDateTime != null && endDateTime != null) {
            long durationMillis = endDateTime.getTimeInMillis() - startDateTime.getTimeInMillis();
            double durationHours = durationMillis / (60 * 60 * 1000.0);

            double totalPrice = durationHours * HOURLY_RATE;
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            tvPrice.setText(String.format("Estimated Price: %s", currencyFormat.format(totalPrice)));
            tvPrice.setVisibility(View.VISIBLE);
        }
    }

    private void checkAndReserveParkingSpot() {
        // Validate inputs
        if (startDateTime == null || endDateTime == null) {
            Toast.makeText(getContext(), "Please select start and end times", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDateTime.before(startDateTime)) {
            Toast.makeText(getContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(getContext(), "You must be logged in to reserve", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for existing reservations
        checkReservationAvailability();
    }

    private void checkReservationAvailability() {
        db.collection("reservations")
                .whereEqualTo("parkingSpotId", parkingSpot.getId())
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isAvailable = true;

                    // Check for time conflicts
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Date existingStart = doc.getDate("startTime");
                        Date existingEnd = doc.getDate("endTime");

                        if (isTimeOverlap(existingStart, existingEnd)) {
                            isAvailable = false;
                            break;
                        }
                    }

                    if (isAvailable) {
                        createReservation();
                    } else {
                        Toast.makeText(getContext(), "This time slot is already booked", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error checking availability", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isTimeOverlap(Date existingStart, Date existingEnd) {
        return !(startDateTime.getTime().after(existingEnd) ||
                endDateTime.getTime().before(existingStart));
    }

    private void createReservation() {
        double totalPrice = calculateTotalPrice();
        String userId = mAuth.getCurrentUser().getUid();

        Reservation reservation = new Reservation(
                null, // Firestore will generate ID
                parkingSpot.getId(),
                userId,
                startDateTime.getTime(),
                endDateTime.getTime(),
                totalPrice,
                parkingSpot.getAddress()
        );

        // Save to Firestore
        db.collection("reservations")
                .add(reservation)
                .addOnSuccessListener(documentReference -> {
                    // Update parking spot status
                    db.collection("parkingSpots")
                            .document(parkingSpot.getId())
                            .update("status", "reserved",
                                    "reservedBy", userId)
                            .addOnSuccessListener(aVoid -> {
                                // Create a notification for the owner
                                sendReservationNotificationToOwner();

                                Toast.makeText(getContext(), "Reservation successful!", Toast.LENGTH_SHORT).show();
                                dismiss();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Reservation failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendReservationNotificationToOwner() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(renterDoc -> {
                    String renterName = renterDoc.getString("firstName") + " " + renterDoc.getString("lastName");
                    String renterPhone = renterDoc.getString("phone");
                    String renterEmail = renterDoc.getString("email");

                    // Create notification object with renter details
                    AppNotification notification = new AppNotification(
                            parkingSpot.getOwnerId(),
                            "Parking Spot Reserved",
                            "Your parking spot at " + parkingSpot.getAddress() + " has been reserved by " + renterName,
                            AppNotification.NotificationType.SPOT_BOOKED,
                            parkingSpot.getId()
                    );

                    // Add comprehensive renter details to notification
                    notification.addExtraData("renterName", renterName);
                    notification.addExtraData("renterPhone", renterPhone);
                    notification.addExtraData("renterEmail", renterEmail);
                    notification.addExtraData("renterId", currentUserId);

                    // Save notification to Firestore
                    db.collection("notifications")
                            .add(notification);
                });
    }

    private double calculateTotalPrice() {
        long durationMillis = endDateTime.getTimeInMillis() - startDateTime.getTimeInMillis();
        double durationHours = durationMillis / (60 * 60 * 1000.0);
        return durationHours * HOURLY_RATE;
    }

    private String formatDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }
}