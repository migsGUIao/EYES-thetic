package com.example.eyesthetic;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClosetRecommendationActivity extends AppCompatActivity {
    private static final String TAG = "ClosetRecoActivity";

    private View    scrollView;
    private View    container;
    private ProgressBar progressBar;
    private TextView indexTextView, topTextView, bottomTextView;
    private ImageView topImageView, bottomImageView;
    private Button  nextButton, favoriteButton;

    private FirebaseFirestore db;
    private String uid;

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

        bindViews();
        loadClosetAndBuildPairs();
        setupListeners();
    }

    private void bindViews() {
        scrollView      = findViewById(R.id.scrollView);
        container       = findViewById(R.id.recommendationContainer);
        progressBar     = findViewById(R.id.progressBar);
        indexTextView   = findViewById(R.id.indexTextView);
        topTextView     = findViewById(R.id.topTextView);
        bottomTextView  = findViewById(R.id.bottomTextView);
        topImageView    = findViewById(R.id.topImageView);
        bottomImageView = findViewById(R.id.bottomImageView);
        nextButton      = findViewById(R.id.nextButton);
        favoriteButton  = findViewById(R.id.favoriteButton);
    }

    private void setupListeners() {
        nextButton.setOnClickListener(v -> {
            if (currentIndex < recs.size() - 1) {
                showPair(++currentIndex);
            }
        });
        favoriteButton.setOnClickListener(v ->
                Toast.makeText(this,
                        "★ Favorited: " + recs.get(currentIndex).topName
                                + " + " + recs.get(currentIndex).bottomName,
                        Toast.LENGTH_SHORT).show()
        );
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
        container.setContentDescription(ann);
        container.announceForAccessibility(ann);

        nextButton.setEnabled(idx < recs.size() - 1);
        scrollView.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        scrollView .setVisibility(loading ? View.GONE    : View.VISIBLE);
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        finish();
    }
}
