package com.example.gisapp1.models;

import java.io.Serializable;
import java.util.Date;

public class Reservation implements Serializable {
    // Unique identifier for the reservation
    private String id;

    // Reference to the specific parking spot being reserved
    private String parkingSpotId;

    // User who made the reservation
    private String userId;

    // Start and end times of the reservation
    private Date startTime;
    private Date endTime;

    // Total cost of the reservation
    private double totalCost;

    // Status of the reservation
    private String status; // Possible values: "active", "completed", "cancelled"

    // Additional optional fields for more detailed tracking
    private String parkingAddress;
    private String reservationCode;

    // Default constructor
    public Reservation() {
        // Empty constructor for Firestore
    }

    // Comprehensive constructor
    public Reservation(String id, String parkingSpotId, String userId,
                       Date startTime, Date endTime,
                       double totalCost, String parkingAddress) {
        this.id = id;
        this.parkingSpotId = parkingSpotId;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalCost = totalCost;
        this.status = "active";
        this.parkingAddress = parkingAddress;
        this.reservationCode = generateReservationCode();
    }

    // Generate a unique reservation code
    private String generateReservationCode() {
        // Simple implementation - can be made more complex
        return "RES-" + System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParkingSpotId() { return parkingSpotId; }
    public void setParkingSpotId(String parkingSpotId) { this.parkingSpotId = parkingSpotId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getParkingAddress() { return parkingAddress; }
    public void setParkingAddress(String parkingAddress) { this.parkingAddress = parkingAddress; }

    public String getReservationCode() { return reservationCode; }
    public void setReservationCode(String reservationCode) { this.reservationCode = reservationCode; }

    // Utility method to check if reservation is active
    public boolean isActive() {
        return "active".equals(status) &&
                new Date().before(endTime);
    }

    // Utility method to calculate reservation duration in hours
    public long getDurationHours() {
        return (endTime.getTime() - startTime.getTime()) / (60 * 60 * 1000);
    }
}