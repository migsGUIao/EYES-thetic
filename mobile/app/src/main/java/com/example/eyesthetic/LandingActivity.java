package com.example.eyesthetic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.util.Log;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.opencv.android.OpenCVLoader;


public class LandingActivity extends AppCompatActivity {
    Button landingBtn;
    public FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();

        super.onCreate(savedInstanceState);

        /*if (OpenCVLoader.initLocal()) {
            //Log.i(TAG, "OpenCV loaded successfully");
            (Toast.makeText(this, "OpenCV loaded successfully!", Toast.LENGTH_LONG)).show();
        } else {
            //Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        } */

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is logged in, redirect to HomepageActivity
            Intent intent = new Intent(this, HomepageActivity.class);
            startActivity(intent);
            finish();
            return;  // Prevent running further code
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.landing), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        landingBtn = findViewById(R.id.landingBtn);
    }

    public void toLogin(View v) {
        //Function to go to login
        Intent intent = new Intent(this, LoginActivity.class);  // replace LoginActivity with your login activity class name
        startActivity(intent);
        finish();
    }

}
