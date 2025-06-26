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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClosetRecommendationActivity extends AppCompatActivity {
    private static final String TAG = "ClosetRecommendationActivity";

    private View    container;
    private TextView indexTextView, itemTextView;
    private ImageView itemImageView;
    private Button  nextButton, favoriteButton;
    private ProgressBar progressBar;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler  mainHandler = new Handler(Looper.getMainLooper());
    private Map<String,String> idToUrl  = new HashMap<>();
    private List<ClothingRec> recs      = new ArrayList<>();
    private int currentIndex = 0;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet_recommendation);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        bindViews();
        loadImageUrlMap();
        fetchClosetItems();
        setupListeners();
    }

    private void bindViews() {
        container       = findViewById(R.id.recommendationContainer);
        indexTextView   = findViewById(R.id.indexTextView);
        itemTextView    = findViewById(R.id.itemTextView);
        itemImageView   = findViewById(R.id.itemImageView);
        nextButton      = findViewById(R.id.nextButton);
        favoriteButton  = findViewById(R.id.favoriteButton);
        progressBar     = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        nextButton.setOnClickListener(v -> {
            if (currentIndex < recs.size() - 1) {
                showRec(++currentIndex);
            }
        });
        favoriteButton.setOnClickListener(v -> saveFavorite(recs.get(currentIndex)));
    }

    // load images
    private void loadImageUrlMap() {
        try (InputStream is = getAssets().open("images.json")) {
            byte[] buf = new byte[is.available()];
            is.read(buf);
            JSONArray arr = new JSONArray(new String(buf, StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String filename = o.getString("filename");  // e.g. "123.jpg"
                String link     = o.getString("link");
                idToUrl.put(filename.replace(".jpg",""), link);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load images.json", e);
        }
    }

    // fetch closet from firestore
    private void fetchClosetItems() {
        showLoading(true);

        db.collection("user").document(uid).collection("closet")
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            List<ClosetItem> items = new ArrayList<>();
                            items = querySnapshot.toObjects(ClosetItem.class);
                            if (items.isEmpty()) {
                                showError("Your closet is empty.");
                            } else {
                                buildRecommendations(items);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error loading closet", e);
                            // show on error but stay on screen
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Error loading your closet:\n" + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });

    }

    private void buildRecommendations(List<ClosetItem> items) {
        Set<String> genders = new HashSet<>(), seasons = new HashSet<>(), usages = new HashSet<>();
        boolean hasTops = false, hasBottoms = false;

        for (ClosetItem ci : items) {
            genders.add(ci.getGender());
            seasons.add(ci.getSeason());
            usages.add(ci.getUsage());

            if ("Top".equalsIgnoreCase(ci.getType()))    hasTops = true;
            if ("Bottom".equalsIgnoreCase(ci.getType())) hasBottoms = true;
        }

        Set<String> targetSubcats = new HashSet<>();
        if (hasTops && !hasBottoms)   targetSubcats.add("Bottomwear");
        if (hasBottoms && !hasTops)   targetSubcats.add("Topwear");
        if (hasTops && hasBottoms) {
            targetSubcats.add("Topwear");
            targetSubcats.add("Bottomwear");
        }

        executor.execute(() -> {
            try {
                InputStream is = getResources().openRawResource(R.raw.clothing);
                byte[] buf = new byte[is.available()];
                is.read(buf);
                is.close();
                JSONArray arr = new JSONArray(new String(buf, StandardCharsets.UTF_8));

                Map<String,Integer> freq      = new HashMap<>();
                Map<String,String>  sampleUrl = new HashMap<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String g    = o.getString("gender");
                    String se   = o.getString("season");
                    String u    = o.getString("usage");
                    String sub  = o.getString("subCategory");
                    String name = o.getString("productDisplayName");
                    String id   = o.getString("id");

                    if ( genders.contains(g)
                            && seasons.contains(se)
                            && usages.contains(u)
                            && targetSubcats.contains(sub)) {

                        freq.put(name, freq.getOrDefault(name,0) + 1);
                        sampleUrl.putIfAbsent(name, idToUrl.getOrDefault(id,""));
                    }
                }

                List<Map.Entry<String,Integer>> list = new ArrayList<>(freq.entrySet());
                Collections.sort(list, (a,b)->b.getValue()-a.getValue());

                recs.clear();
                for (int i = 0; i < Math.min(10, list.size()); i++) {
                    String name = list.get(i).getKey();
                    String url  = sampleUrl.getOrDefault(name,"");
                    recs.add(new ClothingRec(name, url));
                }

                mainHandler.post(() -> {
                    showLoading(false);
                    if (recs.isEmpty()) showError("No recommendations found.");
                    else showRec(0);
                });
            } catch (Exception ex) {
                Log.e(TAG, "Error building recommendations", ex);
                mainHandler.post(() -> {
                    showLoading(false);
                    showError("Failed to build recommendations.");
                });
            }
        });
    }

    // display single recommendation
    private void showRec(int idx) {
        currentIndex = idx;
        ClothingRec r = recs.get(idx);

        indexTextView.setText(String.format(
                Locale.getDefault(),
                "Recommendation %d / %d",
                idx+1, recs.size()
        ));
        itemTextView.setText(r.name);

        Glide.with(this)
                .load(r.imageUrl)
                .placeholder(R.drawable.placeholder_top)
                .error(R.drawable.error_placeholder)
                .into(itemImageView);

        String announcement = String.format(
                Locale.getDefault(),
                "Recommendation %d of %d. Item: %s",
                idx+1, recs.size(), r.name
        );

        container.setContentDescription(announcement);
        container.announceForAccessibility(announcement);

        nextButton .setEnabled(idx < recs.size() - 1);
        favoriteButton.setEnabled(true);
    }


    private void saveFavorite(ClothingRec r) {
        String docId = r.name.toLowerCase()
                .replaceAll("[^a-z0-9]", "_");

        db.collection("user").document(uid).collection("favorites")
                        .document(docId)
                        .set(r)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(this, "Added to favorites!", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed to save favorite.", Toast.LENGTH_SHORT).show()
                        );
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        container   .setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }
    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }
}
