package com.example.eyesthetic;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;


public class HomepageActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    BottomNavigationView bottomNavigationView;
    MaterialButtonToggleGroup toggleButtons;
    MaterialButton selectedRec;
    private Spinner spinnerGender, spinnerSeason, spinnerUsage;
    Button getRecBtn;
    int selectedRecId;
    private static final String TAG = "HomepageActivity";


    static final String MODEL_PATH = "resnet_fashion_classifier.tflite";
    ImageClassifier fashionRecommeder;
    Interpreter interpreter;

    // image mapping
    private Map<String, String> idToImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        mAuth = FirebaseAuth.getInstance();

        bottomNavigationView = findViewById(R.id.homeNavbar);
        toggleButtons = findViewById(R.id.toggleButtons);

        // Toggle buttons, select random rec by default
        if (toggleButtons.getCheckedButtonId() == View.NO_ID) {
            toggleButtons.check(R.id.randomRec);
        }

        // Button selection changes
        toggleButtons.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if(isChecked) {
                    selectedRec = findViewById(checkedId);
                    selectedRecId = checkedId;
                }
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.homeIcon) {
                // Already on Home, do nothing or refresh
                return true;
            } else if (id == R.id.closetIcon) {
                Intent intent = new Intent(HomepageActivity.this, ClosetActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else if (id == R.id.favoritesIcon) {
                Intent intent = new Intent(HomepageActivity.this, FavoritesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } /*else if (id == R.id.logoutIcon) {
                // Logout user
                mAuth.signOut();

                // Go to login and clear back stack
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }*/
            return false;
        });

        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerSeason = findViewById(R.id.spinnerSeason);
        spinnerUsage  = findViewById(R.id.spinnerUsage);

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{ "Men", "Women" }
        );
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<String> seasonAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{ "Winter", "Spring", "Summer", "Fall" }
        );
        spinnerSeason.setAdapter(seasonAdapter);

        ArrayAdapter<String> usageAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{ "Casual", "Formal", "Sport" }
        );
        spinnerUsage.setAdapter(usageAdapter);


        getRecBtn = findViewById(R.id.getRecBtn);

        idToImageUrl = loadImageMapFromJson();

        getRecBtn.setOnClickListener(v -> {
                String selGender = (String)spinnerGender.getSelectedItem();
                String selSeason = (String)spinnerSeason.getSelectedItem();
                String selUsage = (String)spinnerUsage.getSelectedItem();

                if (selGender == null || selSeason == null || selUsage == null) {
                    Toast.makeText(this, "Please select all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                gatherRecommendations(selGender, selSeason, selUsage);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.homeIcon);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    // TODO: Recommender system here
    public void getRec() {
        // IMPORT MODEL
        //        try {
        //            ResnetFashionClassifier model;
        //            model = ResnetFashionClassifier.newInstance(context);
        //
        //            // Creates inputs for reference.
        //            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
        //            inputFeature0.loadBuffer(byteBuffer);
        //
        //            // Runs model inference and gets result.
        //            ResnetFashionClassifier.Outputs outputs = model.process(inputFeature0);
        //            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        //
        //            // Releases model resources if no longer used.
        //            model.close();
        //        } catch (IOException e) {
        //            // TODO Handle the exception
        //        }



        /*
        // With random toggle button selected
        if (selectedRecId == R.id.randomRec) {
        }
        // With from closet toggle button selected
        else if (selectedRecId == R.id.closetRec) {

        }*/

    }

    /**
     * Reads clothing.json from assets, corrects any \"Kids\" items, then
     * filters topwear and bottomwear based on gender, season, usage.
     */

    private void gatherRecommendations(String genderFilter, String seasonFilter, String usageFilter) {
        try {
            // load clothing JSON
            Resources res = getResources();
            InputStream is = res.openRawResource(R.raw.clothing);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String clothingJSON = new String(buffer, StandardCharsets.UTF_8);

            List<JSONObject> topwearList = new ArrayList<>();
            List<JSONObject> bottomwearList = new ArrayList<>();

            // Neutral colors set (exact matches)
            Set<String> neutralColors = new HashSet<>();
            Collections.addAll(neutralColors,
                    "Black", "White", "Grey", "Beige", "Navy Blue", "Brown");

            // === 3. Parse and filter ===
            JSONArray jsonArray = new JSONArray(clothingJSON);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);

                // extract fields from JSON
                String id = item.optString("id", "").trim();
                String gender = item.optString("gender", "").trim();
                String season = item.optString("season", "").trim();
                String usage = item.optString("usage", "").trim();
                String subCategory = item.optString("subCategory", "").trim();
                String baseColour = item.optString("baseColour", "").trim();
                String displayName = item.optString("productDisplayName", "").trim();

                // handle kids clothing labeled as men
                if (displayName.toLowerCase().contains("kids") && gender.equalsIgnoreCase("Men")) {
                    gender = "Boys";
                }
                if (displayName.toLowerCase().contains("suspenders") && gender.equalsIgnoreCase("Men")) {
                    gender = "Boys";
                }


                // apply rule-based filters
                if (gender.equalsIgnoreCase(genderFilter)
                        && season.equalsIgnoreCase(seasonFilter)
                        && usage.equalsIgnoreCase(usageFilter)) {

                    // separate by subCategory
                    if (subCategory.equalsIgnoreCase("Topwear")) {
                        topwearList.add(item);
                    } else if (subCategory.equalsIgnoreCase("Bottomwear")) {
                        bottomwearList.add(item);
                    }
                }
            }

            Log.d(TAG, "Filtered topwear count: " + topwearList.size());
            Log.d(TAG, "Filtered bottomwear count: " + bottomwearList.size());

            // Handle no matches
            if (topwearList.isEmpty() || bottomwearList.isEmpty()) {
                Toast.makeText(this, "No matching recommendations found.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Topwear count: " + topwearList.size() + ", Bottomwear count: " + bottomwearList.size());
                return;
            }

            // randomize combinations
            Collections.shuffle(topwearList);
            Collections.shuffle(bottomwearList);

            // build up to 10 pairs
            int pairCount = Math.min(10, Math.min(topwearList.size(), bottomwearList.size()));
            ArrayList<String> topNames   = new ArrayList<>();
            ArrayList<String> topColors  = new ArrayList<>();
            ArrayList<String> topImageUrls  = new ArrayList<>();
            ArrayList<String> bottomNames  = new ArrayList<>();
            ArrayList<String> bottomColors = new ArrayList<>();
            ArrayList<String> bottomImageUrls= new ArrayList<>();

            for (int i = 0; i < pairCount; i++) {
                JSONObject topItem = topwearList.get(i);
                JSONObject bottomItem = bottomwearList.get(i);

                // Topwear details
                String topId    = topItem.optString("id", "").trim();
                String topName  = topItem.optString("productDisplayName", "Unknown");
                String topColor = topItem.optString("baseColour", "Unknown");
                // Look up image URL by ID
                String topImgUrl= idToImageUrl.getOrDefault(topId, "");

                topNames.add(topName);
                topColors.add(topColor);
                topImageUrls.add(topImgUrl);

                // Bottomwear details
                String bottomId    = bottomItem.optString("id", "").trim();
                String bottomName  = bottomItem.optString("productDisplayName", "Unknown");
                String bottomColor = bottomItem.optString("baseColour", "Unknown");
                String bottomImgUrl= idToImageUrl.getOrDefault(bottomId, "");

                bottomNames.add(bottomName);
                bottomColors.add(bottomColor);
                bottomImageUrls.add(bottomImgUrl);
            }

            // launch RecommendationActivity
            Intent intent = new Intent(this, RecommendationActivity.class);
            intent.putStringArrayListExtra("topNames", topNames);
            intent.putStringArrayListExtra("topColors", topColors);
            intent.putStringArrayListExtra("topImageUrls", topImageUrls);
            intent.putStringArrayListExtra("bottomNames", bottomNames);
            intent.putStringArrayListExtra("bottomColors", bottomColors);
            intent.putStringArrayListExtra("bottomImageUrls", bottomImageUrls);
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error gathering recommendations:", e);
            Toast.makeText(this, "Error loading or filtering data.", Toast.LENGTH_SHORT).show();
        }
    }

    private Map<String, String> loadImageMapFromJson() {
        Map<String, String> map = new HashMap<>();
        try {
            Resources res = getResources();
            InputStream is = res.openRawResource(R.raw.images);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();

            String jsonText = sb.toString();
            JSONArray jsonArray = new JSONArray(jsonText);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String id  = obj.optString("id", "").trim();
                String url = obj.optString("url", "").trim();
                if (!id.isEmpty() && !url.isEmpty()) {
                    map.put(id, url);
                }
            }
            Log.d(TAG, "Loaded " + map.size() + " image mappings from images.json");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load images.json", e);
            Toast.makeText(this, "Error loading images.json", Toast.LENGTH_SHORT).show();
        }
        return map;
    }


    private void createInterpreter (android.content.Context context) {

    }

    private void inferRec() {

    }
}
