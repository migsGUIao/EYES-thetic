package com.example.eyesthetic;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

public class FavoritesActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    BottomNavigationView bottomNavigationView;

    private static final String TAG = "FavoritesActivity";
    private ArrayList<JSONObject> favoritesList = new ArrayList<>();
    private int currentIndex = 0;

    // View references
    private ImageView topImg, botImg;
    private TextView topName, topFavDescInfo;
    private TextView botName, botFavDescInfo;
    private FloatingActionButton prevBtn, nextBtn;
    private Button unfavBtn;
    private LinearLayout favoritesBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        mAuth = FirebaseAuth.getInstance();

        topImg          = findViewById(R.id.topImg);
        botImg          = findViewById(R.id.botImg);
        topName         = findViewById(R.id.topName);
        topFavDescInfo  = findViewById(R.id.topFavDescInfo);
//        botName         = findViewById(R.id.botName);
//        botFavDescInfo  = findViewById(R.id.botFavDescInfo);

        prevBtn   = findViewById(R.id.prevBtn);
        nextBtn   = findViewById(R.id.nextBtn);
        unfavBtn = findViewById(R.id.unfavBtn);
        favoritesBox = findViewById(R.id.favoritesBox);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> favoritesSet = prefs.getStringSet("favorites", null);

        if (favoritesSet == null || favoritesSet.isEmpty()) {
            Toast.makeText(this, "No favorites saved yet.", Toast.LENGTH_SHORT).show();
            // Disable navigation buttons if nothing to show
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            return;
        }

        // Convert Set<String> → ArrayList<JSONObject>
        for (String favJson : favoritesSet) {
            try {
                JSONObject obj = new JSONObject(favJson);
                favoritesList.add(obj);
            } catch (Exception e) {
                Log.e(TAG, "Invalid favorite JSON skipped: " + favJson, e);
            }
        }

        if (favoritesList.isEmpty()) {
            Toast.makeText(this, "No valid favorites to display.", Toast.LENGTH_SHORT).show();
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            return;
        }

        // show the first favorite
        displayFavorite(currentIndex);

        // wire up prevBtn
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentIndex > 0) {
                    currentIndex--;
                    displayFavorite(currentIndex);
                }
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentIndex < favoritesList.size() - 1) {
                    currentIndex++;
                    displayFavorite(currentIndex);
                }
            }
        });

        unfavBtn.setOnClickListener(view -> {
            if (favoritesList.isEmpty()) return;

            JSONObject toRemove = favoritesList.get(currentIndex);
            String favToRemoveString = toRemove.toString();

            Set<String> existing = prefs.getStringSet("favorites", null);
            if (existing == null) return;

            Set<String> updatedSet = new java.util.HashSet<>(existing);
            updatedSet.remove(favToRemoveString);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet("favorites", updatedSet);
            editor.apply();

            // 5. Remove from list and update UI
            favoritesList.remove(currentIndex);

            if (favoritesList.isEmpty()) {
                Toast.makeText(this, "No more favorites.", Toast.LENGTH_SHORT).show();
                topName.setText("");
                topFavDescInfo.setText("");
                topImg.setImageResource(R.drawable.error_placeholder);
                botImg.setImageResource(R.drawable.error_placeholder);
                prevBtn.setEnabled(false);
                nextBtn.setEnabled(false);
                unfavBtn.setEnabled(false);
            } else {
                if (currentIndex >= favoritesList.size()) {
                    currentIndex = favoritesList.size() - 1;
                }
                displayFavorite(currentIndex);
            }

            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
        });


        bottomNavigationView = findViewById(R.id.favNavbar);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.homeIcon) {
                Intent intent = new Intent(FavoritesActivity.this, HomepageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.closetIcon) {
                Intent intent = new Intent(FavoritesActivity.this, ClosetActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.favoritesIcon) {
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

    private void displayFavorite(int index) {
        JSONObject favObj = favoritesList.get(index);

        // Extract fields (fallback to empty string if missing)
        String topNameStr    = favObj.optString("topName", "Unknown Top");
        String topColor      = favObj.optString("topColor", "");
        String topImageUrl   = favObj.optString("topImageUrl", "");

        String botNameStr    = favObj.optString("bottomName", "Unknown Bottom");
        String botColor      = favObj.optString("bottomColor", "");
        String botImageUrl   = favObj.optString("bottomImageUrl", "");

        // Set the name TextViews
        String combo = "Top: " + topNameStr + "\nBottom: " + botNameStr;
        topName.setText(combo);

        // set description
        String topDesc = "Top: Color " + topColor + " Age: (no age yet)\n";
        String botDesc = "Bottom: Color " + botColor + " Age: (no age yet)";
        String comboDesc = topDesc + botDesc;
        topFavDescInfo.setText(comboDesc);

        // sir for top and bottom
        String combinedDesc = "Recommendation " + (index+1)
                + "Top: " + topName + ", color" + topColor
                + "Bottom: " + botName + ", color" + botColor + ".";

        topName.announceForAccessibility(combinedDesc);

        // Load images with Glide
        if (!topImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(topImageUrl)
                    .placeholder(R.drawable.top_placeholder)
                    .error(R.drawable.error_placeholder)
                    .into(topImg);
        } else {
            topImg.setImageResource(R.drawable.error_placeholder);
        }

        if (!botImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(botImageUrl)
                    .placeholder(R.drawable.bottom_placeholder)
                    .error(R.drawable.error_placeholder)
                    .into(botImg);
        } else {
            botImg.setImageResource(R.drawable.error_placeholder);
        }

        // 4) Enable/disable prev/next buttons based on index
        prevBtn.setEnabled(index > 0);
        nextBtn.setEnabled(index < favoritesList.size() - 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.favoritesIcon);
    }

}
