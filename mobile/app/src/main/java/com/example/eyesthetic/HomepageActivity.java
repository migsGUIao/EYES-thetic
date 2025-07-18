package com.example.eyesthetic;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import java.util.Arrays;
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
    Button getRecBtn, closetRecBtn, randomRecBtn;
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
        TextView homeMessage = findViewById(R.id.homeMessage);
        homeMessage.setText(Html.fromHtml(getString(R.string.landing_message), Html.FROM_HTML_MODE_LEGACY));



        mAuth = FirebaseAuth.getInstance();

        bottomNavigationView = findViewById(R.id.homeNavbar);
        /*
        toggleButtons = findViewById(R.id.toggleButtons);

        // Toggle buttons, select random rec by default
        if (toggleButtons.getCheckedButtonId() == View.NO_ID) {
            toggleButtons.check(R.id.randomRec);
        }


        selectedRecId = toggleButtons.getCheckedButtonId();

        toggleButtons.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                selectedRecId = checkedId;
            }
        });

        if (selectedRecId == R.id.closetRec) {
              Intent intent = new Intent(HomepageActivity.this, ClosetRecommendationActivity.class);
              startActivity(intent);
            try {
                Intent intent = new Intent(this, ClosetRecommendationActivityOld.class);
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e("HomepageActivity", "Could not start ClosetRecommendationActivity", e);
                Toast.makeText(this, "Cannot open Closet Recommendations", Toast.LENGTH_LONG).show();
            }

            return;
        }
        */

        closetRecBtn = findViewById(R.id.closetRec);
        randomRecBtn = findViewById(R.id.randomRec);

        setToggleState(randomRecBtn, closetRecBtn);

        final boolean[] isRandomSelected = {true};

        randomRecBtn.setOnClickListener(v -> {
            setToggleState(randomRecBtn, closetRecBtn);
            isRandomSelected[0] = true;
        });

        closetRecBtn.setOnClickListener(v -> {
            setToggleState(closetRecBtn, randomRecBtn);
            isRandomSelected[0] = false;
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

        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerSeason = findViewById(R.id.spinnerSeason);
        spinnerUsage  = findViewById(R.id.spinnerUsage);

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{ "Men", "Women" }
        );
        spinnerGender.setAdapter(genderAdapter);
        spinnerGender.setSelection(0);

        ArrayAdapter<String> seasonAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{ "Winter", "Spring", "Summer", "Fall" }
        );
        spinnerSeason.setAdapter(seasonAdapter);

        ArrayAdapter<String> usageAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{ "Casual", "Formal", "Sports" }
        );
        spinnerUsage.setAdapter(usageAdapter);

        // talkback sr for spinner
        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selection = parent.getItemAtPosition(pos).toString();
                spinnerGender.announceForAccessibility("Gender: " + selection);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        spinnerSeason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selection = parent.getItemAtPosition(pos).toString();
                spinnerSeason.announceForAccessibility("Season: " + selection);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        spinnerUsage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selection = parent.getItemAtPosition(pos).toString();
                spinnerUsage.announceForAccessibility("Usage: " + selection);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });


        getRecBtn = findViewById(R.id.getRecBtn);

        idToImageUrl = loadImageMapFromJson();

        getRecBtn.setOnClickListener(v -> {
            if (isRandomSelected[0]) {
                String selGender = (String) spinnerGender.getSelectedItem();
                String selSeason = (String) spinnerSeason.getSelectedItem();
                String selUsage = (String) spinnerUsage.getSelectedItem();

                if (selGender == null || selSeason == null || selUsage == null) {
                    Toast.makeText(this, "Please select all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                gatherRecommendations(selGender, selSeason, selUsage);
            } else {
                Intent intent = new Intent(HomepageActivity.this, ClosetRecommendationActivity.class);
                startActivity(intent);
            }
        });

    }

    private void setToggleState(Button selected, Button unselected) {
        // selected
        selected.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.md_theme_primaryContainer));
        selected.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        //unselected
        unselected.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.white));
        unselected.setTextColor(ContextCompat.getColor(this, android.R.color.black));
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

            // neutral colors
            Set<String> neutralColors = new HashSet<>(Arrays.asList(
                    "Black", "White", "Grey", "Beige", "Navy Blue", "Brown"
            ));

            // colors according to season
            Set<String> allowedColors;
            if (genderFilter.equalsIgnoreCase("Men")) {
                switch (seasonFilter.toLowerCase()) {
                    case "fall":
                        allowedColors = new HashSet<>(Arrays.asList(
                                "Maroon","Burgundy","Coffee Brown","Mushroom Brown",
                                "Rust","Olive","Mustard","Taupe"
                        ));
                        break;
                    case "summer":
                        allowedColors = new HashSet<>(Arrays.asList(
                                "Blue","Teal","Turquoise Blue","Fluorescent Green",
                                "Magenta","Lime Green","Sea Green","Lavender"
                        ));
                        break;
                    case "winter":
                        allowedColors = new HashSet<>(Arrays.asList(
                                "Navy Blue","Blue","Teal","Turquoise Blue","Fluorescent Green","Magenta"
                        ));
                        break;
                    case "spring":
                        allowedColors = new HashSet<>(Arrays.asList(
                                "Off White","Cream","Beige","Tan","Taupe","Nude",
                                "Peach","Yellow","Pink","Khaki","Skin"
                        ));
                        break;
                    default:
                        allowedColors = neutralColors;
                }
            } else {
                switch (seasonFilter.toLowerCase()) {
                    case "fall":
                        allowedColors = new HashSet<>(Arrays.asList(
                                "Brown", "Bronze", "Copper", "Maroon", "Coffee Brown",
                                "Olive", "Burgundy", "Rust", "Mustard", "Taupe", "Mushroom Brown"
                        ));
                        break;
                    case "summer":
                        allowedColors = new HashSet<>(Arrays.asList(
                                "Silver", "Grey", "Grey Melange", "Steel", "Lavender", "Sea Green",
                                "Mauve", "Rose"
                        ));
                        break;
                    case "winter":
                        allowedColors = new HashSet<>(Arrays.asList(
                                "Blue", "Turquoise Blue", "Teal", "Magenta"
                        ));
                        break;
                    case "spring":
                        allowedColors = new HashSet<>(Arrays.asList(
                                "Off White", "Cream", "Beige", "Tan", "Taupe", "Nude",
                                "Yellow", "Skin"
                        ));
                        break;
                    default:
                        allowedColors = neutralColors;
                }
            }

            JSONArray jsonArray = new JSONArray(clothingJSON);
            List<JSONObject> topwearList = new ArrayList<>();
            List<JSONObject> bottomwearList = new ArrayList<>();

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
                        && usage.equalsIgnoreCase(usageFilter)
                        && allowedColors.contains(baseColour)) {

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
