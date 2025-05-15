package com.example.eyesthetic;

import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText loginEmail, loginPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User already logged in, redirect to Homepage
            Intent intent = new Intent(LoginActivity.this, HomepageActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
    }

    public void loginUser(View view) {
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if(email.isEmpty()) {
            loginEmail.setError("Email is required");
            loginEmail.requestFocus();
            return;
        }

        if(password.isEmpty()) {
            loginPassword.setError("Password is required");
            loginPassword.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                        // Go to main/home screen after login
                        Intent intent = new Intent(LoginActivity.this, HomepageActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: Incorrect email and/or password.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void registerRedirect(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        finish();
    }

}
