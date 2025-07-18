package com.example.eyesthetic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClosetRecommendationActivity extends AppCompatActivity {
    FirebaseAuth mAuth;

    private static final String TAG = "ClosetRecoActivity";

    //private View    scrollView;
    //private View    container;
    private ProgressBar progressBar;
    private TextView indexTextView, topTextView, bottomTextView;
    private ImageView topImageView, bottomImageView;
    private Button favoriteButton;
    private FloatingActionButton nextBtn, prevBtn;

    private FirebaseFirestore db;
    private String uid;
    BottomNavigationView bottomNavigationView;


    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler  mainHandler = new Handler(Looper.getMainLooper());
    private List<PairRec> recs = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet_recommendation);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        bottomNavigationView = findViewById(R.id.recoNavbar);

        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(ClosetRecommendationActivity.this, HomepageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });



        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.homeIcon) {
                Intent intent = new Intent(ClosetRecommendationActivity.this, HomepageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.closetIcon) {
                Intent intent = new Intent(ClosetRecommendationActivity.this, ClosetActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else if (id == R.id.favoritesIcon) {
                Intent intent = new Intent(ClosetRecommendationActivity.this, FavoritesActivity.class);
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

        bindViews();
        loadClosetAndBuildPairs();
        setupListeners();
    }

    private void bindViews() {
        //scrollView      = findViewById(R.id.scrollView);
        //container       = findViewById(R.id.recommendationContainer);
        progressBar     = findViewById(R.id.progressBar);
        indexTextView   = findViewById(R.id.indexTextView);
        topTextView     = findViewById(R.id.topTextView);
        bottomTextView  = findViewById(R.id.bottomTextView);
        topImageView    = findViewById(R.id.topImageView);
        bottomImageView = findViewById(R.id.bottomImageView);
        nextBtn      = findViewById(R.id.nextBtn);
        prevBtn      = findViewById(R.id.prevBtn);
        favoriteButton  = findViewById(R.id.favoriteButton);
    }

    private void setupListeners() {
        nextBtn.setOnClickListener(v -> {
            if (currentIndex < recs.size() - 1) {
                showPair(++currentIndex);
            }
        });
        prevBtn.setOnClickListener(v -> {
            if (currentIndex > 0) {
                showPair(--currentIndex);
            }
        });

        favoriteButton.setOnClickListener(v -> saveCurrentPairToFavorites());
    }
    private void saveCurrentPairToFavorites() {
        if (recs.isEmpty()) return;

        PairRec pair = recs.get(currentIndex);
        try {
            JSONObject favObject = new JSONObject();
            favObject.put("topName", pair.topName);
            favObject.put("topImageUrl", pair.topImageUrl);
            favObject.put("bottomName", pair.bottomName);
            favObject.put("bottomImageUrl", pair.bottomImageUrl);

            db.collection("user").document(uid)
                    .collection("favorites")
                    .add(jsonToMap(favObject))
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Saved to favorites!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save favorite", e);
                        Toast.makeText(this, "Failed to save favorite.", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to prepare favorite data.", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadClosetAndBuildPairs() {
        if (uid == null) {
            Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        db.collection("user").document(uid).collection("closet")
                .get()
                .addOnSuccessListener((QuerySnapshot qs) -> {
                    List<ClosetItem> items = qs.toObjects(ClosetItem.class);

                    // Separate tops & bottoms
                    List<ClosetItem> tops    = new ArrayList<>();
                    List<ClosetItem> bottoms = new ArrayList<>();

                    for (ClosetItem ci : items) {
                        if ("Top".equalsIgnoreCase(ci.getType()))
                            tops.add(ci);
                        if ("Bottom".equalsIgnoreCase(ci.getType()))
                            bottoms.add(ci);
                    }

                    if (tops.isEmpty() || bottoms.isEmpty()) {
                        showError("Please add both a Top and a Bottom to your closet first.");
                    } else {
                        // Build all pairs
                        executor.execute(() -> {
                            List<PairRec> pairs = new ArrayList<>();
                            for (ClosetItem t : tops) {
                                for (ClosetItem b : bottoms) {
                                    pairs.add(new PairRec(
                                            t.getName(), t.getImageUrl(),
                                            b.getName(), b.getImageUrl()
                                    ));
                                }
                            }
                            recs = pairs;

                            mainHandler.post(() -> {
                                showLoading(false);
                                showPair(0);
                            });
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading closet", e);
                    showError("Failed to load your closet.");
                });
    }
    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                map.put(key, json.get(key));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return map;
    }



    private void showPair(int idx) {
        currentIndex = idx;
        PairRec p = recs.get(idx);

        indexTextView.setText("Outfit " + (idx+1) + " / " + recs.size());

        topTextView   .setText("Top: "    + p.topName);
        bottomTextView.setText("Bottom: " + p.bottomName);

        Glide.with(this).load(p.topImageUrl)
                .placeholder(R.drawable.placeholder_top)
                .error(R.drawable.error_placeholder)
                .into(topImageView);

        Glide.with(this).load(p.bottomImageUrl)
                .placeholder(R.drawable.placeholder_bottom)
                .error(R.drawable.error_placeholder)
                .into(bottomImageView);

        // TalkBack announcement
        String ann = "Outfit " + (idx+1) + " of " + recs.size()
                + ". Top: "    + p.topName
                + ". Bottom: " + p.bottomName;
        findViewById(R.id.recommendationBox).setContentDescription(ann);
        findViewById(R.id.recommendationBox).announceForAccessibility(ann);

        nextBtn.setEnabled(idx < recs.size() - 1);
        prevBtn.setEnabled(idx > 0);
        //scrollView.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.recommendationBox).setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        finish();
    }
}