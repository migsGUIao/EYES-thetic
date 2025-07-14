package com.example.eyesthetic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

public class RecommendationActivity extends AppCompatActivity {
    FirebaseAuth mAuth;

    TextView topTextView, bottomTextView, indexTextView;
    ImageView topImageView, bottomImageView;
    FloatingActionButton nextBtn, prevBtn;
    BottomNavigationView bottomNavigationView;


    ArrayList<String> topNames, topColors, topImageUrls;
    ArrayList<String> bottomNames, bottomColors, bottomImageUrls;
    int currentIndex = 0;
    int totalCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        topTextView     = findViewById(R.id.topTextView);
        bottomTextView  = findViewById(R.id.bottomTextView);
        indexTextView   = findViewById(R.id.indexTextView);
        topImageView    = findViewById(R.id.topImageView);
        bottomImageView = findViewById(R.id.bottomImageView);
        nextBtn         = findViewById(R.id.nextBtn);
        prevBtn         = findViewById(R.id.prevBtn);
        bottomNavigationView = findViewById(R.id.recoNavbar);

        mAuth = FirebaseAuth.getInstance();

        Button favoriteButton = findViewById(R.id.favoriteButton);

        topNames        = getIntent().getStringArrayListExtra("topNames");
        topColors       = getIntent().getStringArrayListExtra("topColors");
        topImageUrls    = getIntent().getStringArrayListExtra("topImageUrls");
        bottomNames     = getIntent().getStringArrayListExtra("bottomNames");
        bottomColors    = getIntent().getStringArrayListExtra("bottomColors");
        bottomImageUrls = getIntent().getStringArrayListExtra("bottomImageUrls");

        if (topNames == null || bottomNames == null || topImageUrls == null || bottomImageUrls == null) {
            finish();
            return;
        }

        totalCount = topNames.size();
        showRecommendation(currentIndex);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentIndex < totalCount - 1) {
                    currentIndex++;
                    showRecommendation(currentIndex);
                }
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentIndex > 0) {
                    currentIndex--;
                    showRecommendation(currentIndex);
                }
            }
        });

        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFavorite(currentIndex);
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.homeIcon) {
                Intent intent = new Intent(RecommendationActivity.this, HomepageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.closetIcon) {
                Intent intent = new Intent(RecommendationActivity.this, ClosetActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else if (id == R.id.favoritesIcon) {
                Intent intent = new Intent(RecommendationActivity.this, FavoritesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
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

    private void showRecommendation(int index) {
        String topName   = topNames.get(index);
        String topColor  = topColors.get(index);
        String topUrl    = topImageUrls.get(index);

        String bottomName  = bottomNames.get(index);
        String bottomColor = bottomColors.get(index);
        String bottomUrl    = bottomImageUrls.get(index);

        topTextView.setText("Top: " + topName + " (" + topColor + ")");
        bottomTextView.setText("Bottom: " + bottomName + " (" + bottomColor + ")");
        indexTextView.setText("Recommendation " + (index + 1) + " / " + totalCount);

        // sr for top and bottom
        String combinedDesc = "Recommendation " + (index + 1) + " of " + totalCount
                + "Top: " + topName + ", color " + topColor
                + "Bottom: " + bottomName + ", color " + bottomColor + ".";

        topTextView.announceForAccessibility(combinedDesc);

        // Load images with Glide
        if (!topUrl.isEmpty()) {
            Glide.with(this)
                    .load(topUrl)
                    .placeholder(R.drawable.placeholder_top)
                    .error(R.drawable.error_placeholder)
                    .into(topImageView);
        } else {
            topImageView.setImageResource(R.drawable.error_placeholder);
        }

        if (!bottomUrl.isEmpty()) {
            Glide.with(this)
                    .load(bottomUrl)
                    .placeholder(R.drawable.placeholder_bottom)
                    .error(R.drawable.error_placeholder)
                    .into(bottomImageView);
        } else {
            bottomImageView.setImageResource(R.drawable.error_placeholder);
        }
    }

    private void saveFavorite(int index) {
        try {
            JSONObject favObject = new JSONObject();
            favObject.put("topName", topNames.get(index));
            favObject.put("topColor", topColors.get(index));
            favObject.put("topImageUrl", topImageUrls.get(index));

            favObject.put("bottomName", bottomNames.get(index));
            favObject.put("bottomColor", bottomColors.get(index));
            favObject.put("bottomImageUrl", bottomImageUrls.get(index));

            String favJsonString = favObject.toString();

            // retrieve existing favorites set from SharedPreferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            Set<String> existing = prefs.getStringSet("favorites", null);

            // copy into a new HashSet so we can edit it
            Set<String> newSet = (existing == null) ? new HashSet<>() : new HashSet<>(existing);

            // add our new favorite JSON
            newSet.add(favJsonString);

            // save back to SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet("favorites", newSet);
            editor.apply();

            Toast.makeText(this, "Saved to favorites!", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save favorite.", Toast.LENGTH_SHORT).show();
        }
    }

}
