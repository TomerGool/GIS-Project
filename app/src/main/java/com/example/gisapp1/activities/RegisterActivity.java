package com.example.gisapp1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gisapp1.R;
import com.example.gisapp1.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RadioGroup roleRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        EditText firstNameEditText = findViewById(R.id.firstName);
        EditText lastNameEditText = findViewById(R.id.lastName);
        EditText emailEditText = findViewById(R.id.email);
        EditText phoneEditText = findViewById(R.id.phone);
        EditText passwordEditText = findViewById(R.id.password);
        Button registerButton = findViewById(R.id.registerButton);
        roleRadioGroup = findViewById(R.id.role_selection);

        registerButton.setOnClickListener(v -> {
            // Validate input fields
            String firstName = firstNameEditText.getText().toString().trim();
            String lastName = lastNameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Validate role selection
            int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
            if (selectedRoleId == -1) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine selected role
            String role = (selectedRoleId == R.id.role_owner) ? "owner" : "renter";

            // Validate all fields
            if (validateInputs(firstName, lastName, email, phone, password)) {
                createUserAccount(firstName, lastName, email, phone, password, role);
            }
        });
    }

    private boolean validateInputs(String firstName, String lastName, String email,
                                   String phone, String password) {
        if (firstName.isEmpty()) {
            Toast.makeText(this, "First name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (lastName.isEmpty()) {
            Toast.makeText(this, "Last name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Utils.isValidEmail(email)) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Utils.isValidPhoneNumber(phone)) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Utils.isStrongPassword(password)) {
            Toast.makeText(this,
                    "Password must be at least 8 characters long, contain uppercase, lowercase, number, and special character",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void createUserAccount(String firstName, String lastName, String email,
                                   String phone, String password, String role) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Prepare user data
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("firstName", firstName);
                            userData.put("lastName", lastName);
                            userData.put("email", email);
                            userData.put("phone", phone);
                            userData.put("role", role);

                            // Save user data to Firestore
                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Send email verification
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(verificationTask -> {
                                                    if (verificationTask.isSuccessful()) {
                                                        Toast.makeText(this,
                                                                "Registration successful! Please verify your email.",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                        // Navigate to login activity
                                        Intent intent = new Intent(this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this,
                                                    "Error saving user data: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}