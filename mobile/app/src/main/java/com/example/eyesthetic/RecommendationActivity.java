package com.example.eyesthetic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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

        nextBtn.setOnClickListener(view -> {
            if (currentIndex < totalCount - 1) {
                currentIndex++;
                showRecommendation(currentIndex);
            }
        });

        prevBtn.setOnClickListener(view -> {
            if (currentIndex > 0) {
                currentIndex--;
                showRecommendation(currentIndex);
            }
        });

        favoriteButton.setOnClickListener(view -> saveFavoriteToFirestore(currentIndex));

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(RecommendationActivity.this, HomepageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });


        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.homeIcon) {
                startActivity(new Intent(this, HomepageActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            } else if (id == R.id.closetIcon) {
                startActivity(new Intent(this, ClosetActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            } else if (id == R.id.favoritesIcon) {
                startActivity(new Intent(this, FavoritesActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            } else if (id == R.id.logoutIcon) {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
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
        String bottomUrl   = bottomImageUrls.get(index);

        topTextView.setText("Top: " + topName + " (" + topColor + ")");
        bottomTextView.setText("Bottom: " + bottomName + " (" + bottomColor + ")");
        indexTextView.setText("Recommendation " + (index + 1) + " / " + totalCount);

        String combinedDesc = "Recommendation " + (index + 1) + " of " + totalCount
                + "Top: " + topName + ", color " + topColor
                + "Bottom: " + bottomName + ", color " + bottomColor + ".";

        topTextView.announceForAccessibility(combinedDesc);

        if (!topUrl.isEmpty()) {
            Glide.with(this).load(topUrl)
                    .placeholder(R.drawable.placeholder_top)
                    .error(R.drawable.error_placeholder)
                    .into(topImageView);
        } else {
            topImageView.setImageResource(R.drawable.error_placeholder);
        }

        if (!bottomUrl.isEmpty()) {
            Glide.with(this).load(bottomUrl)
                    .placeholder(R.drawable.placeholder_bottom)
                    .error(R.drawable.error_placeholder)
                    .into(bottomImageView);
        } else {
            bottomImageView.setImageResource(R.drawable.error_placeholder);
        }
    }

    private void saveFavoriteToFirestore(int index) {
        try {
            JSONObject favObject = new JSONObject();
            favObject.put("topName", topNames.get(index));
            favObject.put("topColor", topColors.get(index));
            favObject.put("topImageUrl", topImageUrls.get(index));

            favObject.put("bottomName", bottomNames.get(index));
            favObject.put("bottomColor", bottomColors.get(index));
            favObject.put("bottomImageUrl", bottomImageUrls.get(index));

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("user").document(uid)
                    .collection("favorites")
                    .add(jsonToMap(favObject))
                    .addOnSuccessListener(documentReference ->
                            Toast.makeText(this, "Saved to favorites!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save favorite.", Toast.LENGTH_SHORT).show());
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save favorite.", Toast.LENGTH_SHORT).show();
        }
    }

    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                map.put(key, json.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return map;
    }
}
