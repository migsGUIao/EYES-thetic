package com.example.eyesthetic;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
        prevBtn         = findViewById(R.id.prevBtn);
        nextBtn         = findViewById(R.id.nextBtn);
        unfavBtn        = findViewById(R.id.unfavBtn);
        favoritesBox    = findViewById(R.id.favoritesBox);

        loadFavoritesFromFirestore();

        prevBtn.setOnClickListener(view -> {
            if (currentIndex > 0) {
                currentIndex--;
                displayFavorite(currentIndex);
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (currentIndex < favoritesList.size() - 1) {
                currentIndex++;
                displayFavorite(currentIndex);
            }
        });

        unfavBtn.setOnClickListener(view -> {
            if (favoritesList.isEmpty()) return;

            JSONObject toRemove = favoritesList.get(currentIndex);
            String docId = toRemove.optString("docId", null);
            if (docId == null) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            db.collection("user").document(uid)
                    .collection("favorites").document(docId)
                    .delete()
                    .addOnSuccessListener(unused -> {
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
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete favorite", e));
        });

        bottomNavigationView = findViewById(R.id.favNavbar);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.homeIcon) {
                startActivity(new Intent(this, HomepageActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            } else if (id == R.id.closetIcon) {
                startActivity(new Intent(this, ClosetActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return true;
            } else if (id == R.id.favoritesIcon) {
                return true;
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

    private void loadFavoritesFromFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("user").document(uid)
                .collection("favorites")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    favoritesList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        JSONObject json = new JSONObject(doc.getData());
                        try {
                            json.put("docId", doc.getId());
                            favoritesList.add(json);
                        } catch (JSONException e) {
                            Log.e(TAG, "Invalid JSON object", e);
                        }
                    }

                    if (favoritesList.isEmpty()) {
                        Toast.makeText(this, "No favorites saved yet.", Toast.LENGTH_SHORT).show();
                        prevBtn.setEnabled(false);
                        nextBtn.setEnabled(false);
                    } else {
                        displayFavorite(currentIndex);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading favorites", e));
    }

    private void displayFavorite(int index) {
        JSONObject favObj = favoritesList.get(index);

        String topNameStr  = favObj.optString("topName", "Unknown Top");
        String topColor    = favObj.optString("topColor", "");
        String topImageUrl = favObj.optString("topImageUrl", "");
        String botNameStr  = favObj.optString("bottomName", "Unknown Bottom");
        String botColor    = favObj.optString("bottomColor", "");
        String botImageUrl = favObj.optString("bottomImageUrl", "");

        String combo = "Top: " + topNameStr + "\nBottom: " + botNameStr;
        topName.setText(combo);

        String topDesc = "Top: Color " + topColor + " Age: (no age yet)\n";
        String botDesc = "Bottom: Color " + botColor + " Age: (no age yet)";
        topFavDescInfo.setText(topDesc + botDesc);

        String combinedDesc = "Recommendation " + (index + 1)
                + "Top: " + topName + ", color" + topColor
                + "Bottom: " + botName + ", color" + botColor + ".";

        topName.announceForAccessibility(combinedDesc);

        if (!topImageUrl.isEmpty()) {
            Glide.with(this).load(topImageUrl)
                    .placeholder(R.drawable.top_placeholder)
                    .error(R.drawable.error_placeholder)
                    .into(topImg);
        } else {
            topImg.setImageResource(R.drawable.error_placeholder);
        }

        if (!botImageUrl.isEmpty()) {
            Glide.with(this).load(botImageUrl)
                    .placeholder(R.drawable.bottom_placeholder)
                    .error(R.drawable.error_placeholder)
                    .into(botImg);
        } else {
            botImg.setImageResource(R.drawable.error_placeholder);
        }

        prevBtn.setEnabled(index > 0);
        nextBtn.setEnabled(index < favoritesList.size() - 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.favoritesIcon);
    }
}
