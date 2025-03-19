package com.example.gisapp1.reservation;

import java.util.Date;

public class DefaultReservationRuleEngine implements ReservationRuleEngine {
    @Override
    public boolean isReservationValid(String parkingSpotId, String userId, Date startTime, Date endTime) {
        // Implement your reservation rules here
        // For example, check if the start time is before the end time
        // Check if the reservation duration is within the allowed limits
        // Check if the user has the necessary permissions to make a reservation
        // Return true if the reservation is valid according to your rules, false otherwise
        return true; // Placeholder implementation
    }
}