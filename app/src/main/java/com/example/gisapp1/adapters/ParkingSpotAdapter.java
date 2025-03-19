package com.example.gisapp1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gisapp1.R;
import com.example.gisapp1.models.ParkingSpot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ParkingSpotAdapter extends RecyclerView.Adapter<ParkingSpotAdapter.ParkingSpotViewHolder> {

    public static final int TYPE_RESERVATION = 1;
    public static final int TYPE_MY_SPOT = 2;

    private List<ParkingSpot> parkingSpots = new ArrayList<>();
    private final Fragment fragment;
    private final int type;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ParkingSpotAdapter(Fragment fragment, int type) {
        this.fragment = fragment;
        this.type = type;
    }

    @NonNull
    @Override
    public ParkingSpotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking_spot, parent, false);
        return new ParkingSpotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParkingSpotViewHolder holder, int position) {
        ParkingSpot spot = parkingSpots.get(position);
        holder.address.setText(spot.getAddress());
        holder.availability.setText(String.format("Available: %s to %s",
                spot.getAvailableFrom(), spot.getAvailableUntil()));

        // Configure based on adapter type
        if (type == TYPE_RESERVATION) {
            holder.actionButton.setText("Cancel Reservation");
            holder.actionButton.setOnClickListener(v -> cancelReservation(spot));
        } else { // TYPE_MY_SPOT
            if ("available".equals(spot.getStatus())) {
                holder.actionButton.setText("Make Unavailable");
                holder.actionButton.setOnClickListener(v -> updateSpotStatus(spot, "unavailable"));
            } else {
                holder.actionButton.setText("Make Available");
                holder.actionButton.setOnClickListener(v -> updateSpotStatus(spot, "available"));
            }
        }
    }

    @Override
    public int getItemCount() {
        return parkingSpots.size();
    }

    public void updateParkingSpots(List<ParkingSpot> spots) {
        this.parkingSpots = spots;
        notifyDataSetChanged();
    }

    private void cancelReservation(ParkingSpot spot) {
        db.collection("parkingSpots").document(spot.getId())
                .update("status", "available", "reservedBy", null)
                .addOnSuccessListener(aVoid -> Toast.makeText(fragment.getContext(),
                        "Reservation cancelled", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(fragment.getContext(),
                        "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateSpotStatus(ParkingSpot spot, String newStatus) {
        db.collection("parkingSpots").document(spot.getId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(fragment.getContext(),
                        "Status updated to " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(fragment.getContext(),
                        "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    static class ParkingSpotViewHolder extends RecyclerView.ViewHolder {
        TextView address;
        TextView availability;
        Button actionButton;

        public ParkingSpotViewHolder(@NonNull View itemView) {
            super(itemView);
            address = itemView.findViewById(R.id.text_address);
            availability = itemView.findViewById(R.id.text_availability);
            actionButton = itemView.findViewById(R.id.button_action);
        }
    }
}