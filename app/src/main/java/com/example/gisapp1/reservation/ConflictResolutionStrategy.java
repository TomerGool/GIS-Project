package com.example.gisapp1.reservation;

import com.example.gisapp1.models.Reservation;

import java.util.List;

public interface ConflictResolutionStrategy {
    boolean resolveConflicts(List<Reservation> conflicts);
}