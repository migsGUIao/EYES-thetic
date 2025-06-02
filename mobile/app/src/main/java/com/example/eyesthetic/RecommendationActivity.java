package com.example.eyesthetic;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

        // 1) Retrieve Intent Extras
        topNames    = getIntent().getStringArrayListExtra("topNames");
        topColors   = getIntent().getStringArrayListExtra("topColors");
        topImageUrls  = getIntent().getStringArrayListExtra("topImageUrls");
        bottomNames  = getIntent().getStringArrayListExtra("bottomNames");
        bottomColors = getIntent().getStringArrayListExtra("bottomColors");
        bottomImageUrls= getIntent().getStringArrayListExtra("bottomImageUrls");

        if (topNames == null || bottomNames == null || topImageUrls == null || bottomImageUrls == null) {
            // No data passed—finish the activity
            finish();
            return;
        }

        totalCount = topNames.size();
        showRecommendation(currentIndex);

        // 2) Next button increments index
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentIndex++;
                if (currentIndex >= totalCount) {
                    // Hide Next button if we've reached the end
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

        topTextView.setText("Top: " + topName + " (" + topColor + ")" + topUrl);
        bottomTextView.setText("Bottom: " + bottomName + " (" + bottomColor + ")" + bottomUrl);
        indexTextView.setText("Recommendation " + (index + 1) + " / " + totalCount);

        topImageView.setImageResource(R.drawable.placeholder_top);
        if (!topUrl.isEmpty()) {
            new ImageLoader(topImageView).execute(topUrl);
        } else {
            topImageView.setImageResource(R.drawable.error_placeholder);
        }

        // Load bottom image manually
        bottomImageView.setImageResource(R.drawable.placeholder_bottom);
        if (!bottomUrl.isEmpty()) {
            new ImageLoader(bottomImageView).execute(bottomUrl);
        } else {
            bottomImageView.setImageResource(R.drawable.error_placeholder);
        }
    }

    private static class ImageLoader extends AsyncTask<String, Void, Bitmap> {
        private final ImageView imageView;

        ImageLoader(ImageView iv) {
            imageView = iv;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String urlString = params[0];
            if (urlString == null || urlString.isEmpty()) return null;

            InputStream input = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                // Log or handle the error as needed
                e.printStackTrace();
                return null;
            } finally {
                if (input != null) {
                    try { input.close(); } catch (Exception ignored) {}
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            // If bitmap is null, you can choose to set a placeholder or error drawable:
            else {
                imageView.setImageResource(R.drawable.error_placeholder);
            }
        }
    }
}
