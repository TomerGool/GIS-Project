package com.example.gisapp1.activities;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.example.gisapp1.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Example: Adding a parking spot marker
        LatLng parkingSpot = new LatLng(32.0853, 34.7818);
        mMap.addMarker(new MarkerOptions().position(parkingSpot).title("Available Parking"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(parkingSpot, 15));
    }
}
