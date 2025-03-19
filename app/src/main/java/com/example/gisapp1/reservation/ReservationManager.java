package com.example.gisapp1.reservation;

import com.example.gisapp1.models.ParkingSpot;
import com.example.gisapp1.models.Reservation;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReservationManager {
    private ReservationRuleEngine ruleEngine;
    private PricingStrategy pricingStrategy;
    private ConflictResolutionStrategy conflictResolutionStrategy;
    private FirebaseFirestore db;

    public ReservationManager(ReservationRuleEngine ruleEngine, PricingStrategy pricingStrategy,
                              ConflictResolutionStrategy conflictResolutionStrategy) {
        this.ruleEngine = ruleEngine;
        this.pricingStrategy = pricingStrategy;
        this.conflictResolutionStrategy = conflictResolutionStrategy;
        this.db = FirebaseFirestore.getInstance();
    }

    public Reservation createReservation(String parkingSpotId, String userId, Date startTime, Date endTime) {
        // Check if the reservation is valid according to the rules
        if (!ruleEngine.isReservationValid(parkingSpotId, userId, startTime, endTime)) {
            throw new IllegalArgumentException("Reservation does not meet the required rules.");
        }

        // Check for conflicts with existing reservations
        List<Reservation> conflicts = getConflictingReservations(parkingSpotId, startTime, endTime);
        if (!conflicts.isEmpty()) {
            // Try to resolve conflicts
            if (!conflictResolutionStrategy.resolveConflicts(conflicts)) {
                throw new IllegalStateException("Reservation conflicts could not be resolved.");
            }
        }

        // Calculate the reservation price
        double price = pricingStrategy.calculatePrice(parkingSpotId, startTime, endTime);

        // Create and return the reservation
        Reservation reservation = new Reservation(null, parkingSpotId, userId, startTime, endTime, price, "");
        saveReservation(reservation);
        return reservation;
    }

    private List<Reservation> getConflictingReservations(String parkingSpotId, Date startTime, Date endTime) {
        List<Reservation> conflictingReservations = new ArrayList<>();
        // Query Firestore for conflicting reservations
        // Add logic to fetch reservations from Firestore that conflict with the given time range
        // and add them to the conflictingReservations list
        return conflictingReservations;
    }

    private void saveReservation(Reservation reservation) {
        // Save the reservation to Firestore
        db.collection("reservations")
                .add(reservation)
                .addOnSuccessListener(documentReference -> {
                    // Reservation saved successfully
                    // You can add any additional logic here, like updating the parking spot status
                })
                .addOnFailureListener(e -> {
                    // Error occurred while saving the reservation
                    // Handle the error appropriately
                });
    }

    // Other methods for managing reservations...
}