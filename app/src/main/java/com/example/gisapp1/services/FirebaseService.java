package com.example.gisapp1.services;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FirebaseFirestore getDb() {
        return db;
    }
}
