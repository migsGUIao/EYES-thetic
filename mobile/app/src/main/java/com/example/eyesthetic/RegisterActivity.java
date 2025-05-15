package com.example.eyesthetic;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText registerEmail, registerUsername, registerPassword;
    private FirebaseAuth mAuth;
    public FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Use your register layout

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views
        registerEmail = findViewById(R.id.registerEmail);
        registerUsername = findViewById(R.id.registerUsername);
        registerPassword = findViewById(R.id.registerPassword);
    }

    public void registerUser(View view) {
        String email = registerEmail.getText().toString().trim();
        String username = registerUsername.getText().toString().trim();
        String password = registerPassword.getText().toString().trim();

        if(email.isEmpty()) {
            registerEmail.setError("Email is required");
            registerEmail.requestFocus();
            return;
        }

        if(username.isEmpty()) {
            registerEmail.setError("Username is required");
            registerEmail.requestFocus();
            return;
        }

        if(password.isEmpty()) {
            registerPassword.setError("Password is required");
            registerPassword.requestFocus();
            return;
        }

        if(password.length() < 6) {
            registerPassword.setError("Password must be >= 6 characters");
            registerPassword.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        // Save additional user info to Firestore
                        String uid = mAuth.getCurrentUser().getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("email", email);
                        user.put("username", username);
                        user.put("displayName", username);  // or any other display name logic
                        user.put("uid", uid);

                        db.collection("user").document(uid)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(RegisterActivity.this, "Failed to save user info", Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed.", Toast.LENGTH_LONG).show();
                    }
                });
    }


    public void loginRedirect(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
