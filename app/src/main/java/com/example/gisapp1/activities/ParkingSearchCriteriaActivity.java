package com.example.gisapp1.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gisapp1.R;
import com.example.gisapp1.services.ParkingAvailabilityNotificationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ParkingSearchCriteriaActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private EditText locationEditText;
    private Button startTimeButton, endTimeButton, saveSearchButton;
    private String startTime, endTime;
    private SimpleDateFormat timeFormat;
    private ParkingAvailabilityNotificationService notificationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_search_criteria);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        notificationService = new ParkingAvailabilityNotificationService();

        // Initialize views
        locationEditText = findViewById(R.id.et_search_location);
        startTimeButton = findViewById(R.id.btn_start_time);
        endTimeButton = findViewById(R.id.btn_end_time);
        saveSearchButton = findViewById(R.id.btn_save_search);

        // Setup time selection buttons
        startTimeButton.setOnClickListener(v -> showTimePickerDialog(true));
        endTimeButton.setOnClickListener(v -> showTimePickerDialog(false));

        // Setup save button
        saveSearchButton.setOnClickListener(v -> saveParkingSearchCriteria());
    }

    private void showTimePickerDialog(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                    selectedTime.set(Calendar.MINUTE, selectedMinute);

                    String formattedTime = timeFormat.format(selectedTime.getTime());

                    if (isStartTime) {
                        startTime = formattedTime;
                        startTimeButton.setText(formattedTime);
                    } else {
                        endTime = formattedTime;
                        endTimeButton.setText(formattedTime);
                    }
                },
                hour,
                minute,
                true // 24 hour format
        );

        timePickerDialog.show();
    }

    private void saveParkingSearchCriteria() {
        String location = locationEditText.getText().toString().trim();

        if (TextUtils.isEmpty(location)) {
            locationEditText.setError("Please enter a location");
            return;
        }

        if (startTime == null || startTime.isEmpty()) {
            Toast.makeText(this, "Please select a start time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endTime == null || endTime.isEmpty()) {
            Toast.makeText(this, "Please select an end time", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> searchCriteria = new HashMap<>();
        searchCriteria.put("userId", userId);
        searchCriteria.put("location", location);
        searchCriteria.put("startTime", startTime);
        searchCriteria.put("endTime", endTime);
        searchCriteria.put("status", "active");
        searchCriteria.put("createdAt", Calendar.getInstance().getTime());

        db.collection("parkingSearches")
                .add(searchCriteria)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Search criteria saved. Checking for available spots now.",
                            Toast.LENGTH_LONG).show();

                    // Immediately check for available parking spots based on this new search
                    notificationService.checkAvailableParkingSpots();

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving search: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}