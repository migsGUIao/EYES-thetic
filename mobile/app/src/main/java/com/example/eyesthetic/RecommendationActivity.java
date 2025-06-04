package com.example.eyesthetic;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class RecommendationActivity extends AppCompatActivity {

    TextView topTextView, bottomTextView, indexTextView;
    ImageView topImageView, bottomImageView;
    Button nextButton;

    ArrayList<String> topNames, topColors, topImageUrls;
    ArrayList<String> bottomNames, bottomColors, bottomImageUrls;
    int currentIndex = 0;
    int totalCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        topTextView    = findViewById(R.id.topTextView);
        bottomTextView = findViewById(R.id.bottomTextView);
        indexTextView  = findViewById(R.id.indexTextView);
        topImageView    = findViewById(R.id.topImageView);
        bottomImageView = findViewById(R.id.bottomImageView);
        nextButton     = findViewById(R.id.nextButton);

        topNames    = getIntent().getStringArrayListExtra("topNames");
        topColors   = getIntent().getStringArrayListExtra("topColors");
        topImageUrls  = getIntent().getStringArrayListExtra("topImageUrls");
        bottomNames  = getIntent().getStringArrayListExtra("bottomNames");
        bottomColors = getIntent().getStringArrayListExtra("bottomColors");
        bottomImageUrls= getIntent().getStringArrayListExtra("bottomImageUrls");

        if (topNames == null || bottomNames == null || topImageUrls == null || bottomImageUrls == null) {
            finish();
            return;
        }

        totalCount = topNames.size();
        showRecommendation(currentIndex);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentIndex++;
                if (currentIndex >= totalCount) {
                    nextButton.setVisibility(View.GONE);
                } else {
                    showRecommendation(currentIndex);
                }
            }
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

}
