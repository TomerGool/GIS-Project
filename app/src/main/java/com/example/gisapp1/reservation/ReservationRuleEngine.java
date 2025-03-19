package com.example.gisapp1.reservation;

import java.util.Date;

public interface ReservationRuleEngine {
    boolean isReservationValid(String parkingSpotId, String userId, Date startTime, Date endTime);
}