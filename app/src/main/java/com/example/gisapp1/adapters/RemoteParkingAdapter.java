package com.example.gisapp1.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gisapp1.R;
import com.example.gisapp1.models.ParkingSpot;

import java.util.List;

public class RemoteParkingAdapter extends RecyclerView.Adapter<RemoteParkingAdapter.ParkingViewHolder> {

    private List<ParkingSpot> parkingSpots;
    private ParkingStatusCallback callback;

    public interface ParkingStatusCallback {
        void updateParkingSpotStatus(String parkingSpotId, String status);
    }

    public RemoteParkingAdapter(List<ParkingSpot> parkingSpots, ParkingStatusCallback callback) {
        this.parkingSpots = parkingSpots;
        this.callback = callback;
    }

    @NonNull
    @Override
    public ParkingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_parking_spot, parent, false);
        return new ParkingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParkingViewHolder holder, int position) {
        ParkingSpot spot = parkingSpots.get(position);
        holder.bind(spot);
    }

    @Override
    public int getItemCount() {
        return parkingSpots.size();
    }

    class ParkingViewHolder extends RecyclerView.ViewHolder {
        TextView addressTextView, availabilityTextView;
        Button actionButton;

        public ParkingViewHolder(@NonNull View itemView) {
            super(itemView);
            addressTextView = itemView.findViewById(R.id.text_address);
            availabilityTextView = itemView.findViewById(R.id.text_availability);
            actionButton = itemView.findViewById(R.id.button_action);
        }

        void bind(ParkingSpot spot) {
            addressTextView.setText(spot.getAddress());
            availabilityTextView.setText(String.format("Available: %s to %s",
                    spot.getAvailableFrom(), spot.getAvailableUntil()));

            // Configure the action button based on spot status
            if ("available".equals(spot.getStatus())) {
                actionButton.setText("Mark as Unavailable");
                actionButton.setOnClickListener(v -> {
                    callback.updateParkingSpotStatus(spot.getId(), "unavailable");
                });
            } else {
                actionButton.setText("Mark as Available");
                actionButton.setOnClickListener(v -> {
                    callback.updateParkingSpotStatus(spot.getId(), "available");
                });
            }
        }
    }
}