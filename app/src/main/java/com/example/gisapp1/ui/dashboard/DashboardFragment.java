package com.example.gisapp1.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gisapp1.R;
import com.example.gisapp1.adapters.ParkingSpotAdapter;
import com.example.gisapp1.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private ParkingSpotAdapter reservationsAdapter;
    private ParkingSpotAdapter myParkingSpotsAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup reservations recycler view
        RecyclerView reservationsRecyclerView = binding.recyclerReservations;
        reservationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reservationsAdapter = new ParkingSpotAdapter(this, ParkingSpotAdapter.TYPE_RESERVATION);
        reservationsRecyclerView.setAdapter(reservationsAdapter);

        // Setup my parking spots recycler view
        RecyclerView spotsRecyclerView = binding.recyclerMySpots;
        spotsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myParkingSpotsAdapter = new ParkingSpotAdapter(this, ParkingSpotAdapter.TYPE_MY_SPOT);
        spotsRecyclerView.setAdapter(myParkingSpotsAdapter);

        // Observe reservations
        viewModel.getMyReservations().observe(getViewLifecycleOwner(), spots -> {
            reservationsAdapter.updateParkingSpots(spots);
            binding.textReservationsEmpty.setVisibility(spots.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // Observe my parking spots
        viewModel.getMyParkingSpots().observe(getViewLifecycleOwner(), spots -> {
            myParkingSpotsAdapter.updateParkingSpots(spots);
            binding.textMySpotsEmpty.setVisibility(spots.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // Observe status messages
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                binding.textStatusMessage.setText(message);
                binding.textStatusMessage.setVisibility(View.VISIBLE);
            } else {
                binding.textStatusMessage.setVisibility(View.GONE);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}