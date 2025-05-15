package com.example.eyesthetic;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class ClosetActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet);

        mAuth = FirebaseAuth.getInstance();

        bottomNavigationView = findViewById(R.id.closetNavbar);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.homeIcon) {
                Intent intent = new Intent(ClosetActivity.this, HomepageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);

                return true;
            } else if (id == R.id.closetIcon) {
                return true;
            } else if (id == R.id.favoritesIcon) {
                Intent intent = new Intent(ClosetActivity.this, FavoritesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.logoutIcon) {
                // Logout user
                mAuth.signOut();

                // Go to login and clear back stack
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.closetIcon);
    }

}
