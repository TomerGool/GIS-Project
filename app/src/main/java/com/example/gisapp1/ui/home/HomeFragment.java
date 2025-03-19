package com.example.gisapp1.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gisapp1.activities.AddParkingSpotActivity;
import com.example.gisapp1.activities.LoginActivity;
import com.example.gisapp1.activities.MainActivity;
import com.example.gisapp1.activities.ParkingSearchCriteriaActivity;
import com.example.gisapp1.activities.RemoteParkingManagementActivity;
import com.example.gisapp1.databinding.FragmentHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userRole;
    private Button logoutButton;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize logout button
        logoutButton = binding.btnLogout;
        logoutButton.setOnClickListener(v -> logoutUser());

        // Check user role and update UI accordingly
        checkUserRoleAndUpdateUI();

        // Default button setup - will be used if role check fails
        setupDefaultButtons();

        return root;
    }

    private void logoutUser() {
        // Sign out from Firebase Auth
        auth.signOut();

        // Navigate to login screen
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();

        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void checkUserRoleAndUpdateUI() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            userRole = documentSnapshot.getString("role");
                            updateButtonsBasedOnRole(userRole);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupDefaultButtons() {
        // Connect "Rent out my parking spot" button to add parking screen
        binding.buttonRentParking.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddParkingSpotActivity.class);
            startActivity(intent);
        });

        // Connect "Looking for a parking spot" button to map view
        binding.buttonFindParking.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Searching for nearby parking spots...", Toast.LENGTH_SHORT).show();

            // If we're in MainActivity, call the showMapWithCurrentLocation method
            if (getActivity() instanceof MainActivity) {
                try {
                    ((MainActivity) getActivity()).showMapWithCurrentLocation();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error showing map: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Add Check For Spots button - initially invisible
        binding.buttonCheckSpots.setVisibility(View.GONE);
    }

    private void updateButtonsBasedOnRole(String role) {
        if ("owner".equals(role)) {
            // User is a parking spot owner
            binding.buttonRentParking.setText("Rent out my parking spot");
            binding.buttonRentParking.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AddParkingSpotActivity.class);
                startActivity(intent);
            });

            // Add remote management button
            binding.buttonFindParking.setText("Manage my parking spots remotely");
            binding.buttonFindParking.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), RemoteParkingManagementActivity.class);
                startActivity(intent);
            });

            // Hide check spots button for owners
            binding.buttonCheckSpots.setVisibility(View.GONE);

        } else if ("renter".equals(role)) {
            // User is looking for parking
            binding.buttonFindParking.setText("Find parking nearby");
            binding.buttonFindParking.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    try {
                        ((MainActivity) getActivity()).showMapWithCurrentLocation();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error showing map: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Add search criteria button
            binding.buttonRentParking.setText("Set search preferences");
            binding.buttonRentParking.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ParkingSearchCriteriaActivity.class);
                startActivity(intent);
            });

            // Show and setup check spots button for renters
            binding.buttonCheckSpots.setVisibility(View.VISIBLE);
            binding.buttonCheckSpots.setText("Check for available spots");
            binding.buttonCheckSpots.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).checkForAvailableSpots();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}