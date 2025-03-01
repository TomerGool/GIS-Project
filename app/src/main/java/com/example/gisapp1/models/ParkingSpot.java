package com.example.gisapp1.models;


public class ParkingSpot {
    private String id;
    private String ownerId;
    private String address;
    private double latitude;
    private double longitude;
    private String status;
    private String reservedBy;
    private String availableFrom;
    private String availableUntil;

    // Constructor
    public ParkingSpot(String id, String ownerId, String address, double latitude, double longitude,
                       String status, String reservedBy, String availableFrom, String availableUntil) {
        this.id = id;
        this.ownerId = ownerId;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.reservedBy = reservedBy;
        this.availableFrom = availableFrom;
        this.availableUntil = availableUntil;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReservedBy() { return reservedBy; }
    public void setReservedBy(String reservedBy) { this.reservedBy = reservedBy; }
    public String getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(String availableFrom) { this.availableFrom = availableFrom; }
    public String getAvailableUntil() { return availableUntil; }
    public void setAvailableUntil(String availableUntil) { this.availableUntil = availableUntil; }

}


