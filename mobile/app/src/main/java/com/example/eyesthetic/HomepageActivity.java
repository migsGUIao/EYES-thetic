package com.example.eyesthetic;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

//import com.example.eyesthetic.ml.ResnetFashionClassifier;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.IOException;
import java.nio.MappedByteBuffer;

public class HomepageActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    BottomNavigationView bottomNavigationView;
    MaterialButtonToggleGroup toggleButtons;
    MaterialButton selectedRec;
    EditText homeGender;
    EditText homeSeason;
    EditText homeUsage;
    int selectedRecId;

    static final String MODEL_PATH = "resnet_fashion_classifier.tflite";
    ImageClassifier fashionRecommeder;
    Interpreter interpreter;

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
    // Get recommendations button
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

        homeGender = findViewById(R.id.homeGender);
        homeSeason = findViewById(R.id.homeSeason);
        homeUsage = findViewById(R.id.homeUsage);

        String selGender = homeGender.getText().toString();
        String selSeason = homeSeason.getText().toString();
        String selUsage = homeUsage.getText().toString();

        // With random toggle button selected
        if(selectedRecId == R.id.randomRec) {

        }
        // With from closet toggle button selected
        else if(selectedRecId == R.id.closetRec) {

        }
    }

    private void createInterpreter (android.content.Context context) {

    }

    private void inferRec() {

    }
}
