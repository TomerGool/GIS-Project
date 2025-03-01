package com.example.gisapp1.activities;

//public class RegisterActivity {

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gisapp1.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

    public class RegisterActivity extends AppCompatActivity {

        private FirebaseAuth mAuth;
        private FirebaseFirestore db;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_register);

            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            EditText firstNameEditText = findViewById(R.id.firstName);
            EditText lastNameEditText = findViewById(R.id.lastName);
            EditText emailEditText = findViewById(R.id.email);
            EditText phoneEditText = findViewById(R.id.phone);
            EditText passwordEditText = findViewById(R.id.password);
            Button registerButton = findViewById(R.id.registerButton);

            registerButton.setOnClickListener(v -> {
                String firstName = firstNameEditText.getText().toString();
                String lastName = lastNameEditText.getText().toString();
                String email = emailEditText.getText().toString();
                String phone = phoneEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (!firstName.isEmpty() && !lastName.isEmpty() && !email.isEmpty() && !phone.isEmpty() && !password.isEmpty()) {
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("firstName", firstName);
                                        userData.put("lastName", lastName);
                                        userData.put("email", email);
                                        userData.put("phone", phone);

                                        db.collection("users").document(user.getUid())
                                                .set(userData)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(this, LoginActivity.class));
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show());
                                    }
                                } else {
                                    Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

//}
