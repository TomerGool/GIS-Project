package com.example.gisapp1.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gisapp1.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.loginButton);
        TextView registerButton = findViewById(R.id.registerButton);
        TextView forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        CheckBox rememberMe = findViewById(R.id.rememberMe);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (!email.isEmpty() && !password.isEmpty()) {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();

                                // Check if email is verified
                                if (user != null && user.isEmailVerified()) {
                                    // Get current user ID
                                    String userId = user.getUid();

                                    // Fetch user role from Firestore
                                    db.collection("users").document(userId)
                                            .get()
                                            .addOnSuccessListener(documentSnapshot -> {
                                                // Store user role in SharedPreferences for quick access
                                                String role = documentSnapshot.getString("role");
                                                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                                prefs.edit().putString("userRole", role).apply();

                                                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(this, MainActivity.class);
                                                startActivity(intent);
                                                finish(); // Close the login screen
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Error retrieving user data: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    // Email not verified
                                    Toast.makeText(this,
                                            "Please verify your email before logging in. Check your inbox.",
                                            Toast.LENGTH_LONG).show();

                                    // Optional: Send verification email again
                                    if (user != null) {
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(verificationTask -> {
                                                    if (verificationTask.isSuccessful()) {
                                                        Toast.makeText(this,
                                                                "Verification email sent again. Please check your inbox.",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }

                                    // Sign out the user
                                    mAuth.signOut();
                                }
                            } else {
                                Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            }
        });

        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        forgotPasswordButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }
}