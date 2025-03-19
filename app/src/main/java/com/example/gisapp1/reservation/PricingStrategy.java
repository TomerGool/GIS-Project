package com.example.gisapp1.reservation;

import java.util.Date;

public interface PricingStrategy {
    double calculatePrice(String parkingSpotId, Date startTime, Date endTime);
}