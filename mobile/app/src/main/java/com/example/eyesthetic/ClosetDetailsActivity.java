package com.example.eyesthetic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClosetDetailsActivity extends AppCompatActivity {

    private ImageView itemImageView;
    private EditText nameInput, colorInput;
    private Spinner typeSpinner, genderSpinner, seasonSpinner, usageSpinner;
    private Button saveButton, editButton, deleteButton, cancelButton;
    private ClosetItem originalItem;

    private Bitmap capturedBitmap;
    private FirebaseFirestore db;
    private String detectedType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet_details);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupSpinners();
        handleIntent();
        setupButtons();
    }

    private void initViews() {
        itemImageView = findViewById(R.id.itemImageView);
        nameInput = findViewById(R.id.nameInput);
        colorInput = findViewById(R.id.colorInput);
        typeSpinner = findViewById(R.id.typeSpinner);
        genderSpinner = findViewById(R.id.genderSpinner);
        seasonSpinner = findViewById(R.id.seasonSpinner);
        usageSpinner = findViewById(R.id.usageSpinner);
        saveButton = findViewById(R.id.saveButton);
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    tv.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
    }


    private void setupSpinners() {
        setupSpinner(typeSpinner, new String[]{"Type", "Top", "Bottom"});
        setupSpinner(genderSpinner, new String[]{"Gender", "Women", "Men"});
        setupSpinner(seasonSpinner, new String[]{"Season", "Spring", "Summer", "Fall", "Winter"});
        setupSpinner(usageSpinner, new String[]{"Usage", "Casual", "Formal", "Sports"});
    }

    private void setFieldsEnabled(boolean enabled) {
        nameInput.setEnabled(enabled);
        colorInput.setEnabled(enabled);
        typeSpinner.setEnabled(enabled);
        genderSpinner.setEnabled(enabled);
        seasonSpinner.setEnabled(enabled);
        usageSpinner.setEnabled(enabled);

        // Update spinner background and text color based on mode
        int backgroundRes = enabled ? R.drawable.alt_filled_dropdown : R.drawable.alt_filled_box;
        int textColor = ContextCompat.getColor(this, R.color.md_theme_onSurface_highContrast);

        updateSpinnerStyle(typeSpinner, backgroundRes, textColor);
        updateSpinnerStyle(genderSpinner, backgroundRes, textColor);
        updateSpinnerStyle(seasonSpinner, backgroundRes, textColor);
        updateSpinnerStyle(usageSpinner, backgroundRes, textColor);
    }

    private void updateSpinnerStyle(Spinner spinner, int backgroundRes, int textColor) {
        spinner.setBackgroundResource(backgroundRes);

        View selectedView = spinner.getSelectedView();
        if (selectedView instanceof TextView) {
            ((TextView) selectedView).setTextColor(textColor);
        }
    }



    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }


    private void handleIntent() {
        byte[] byteArray = getIntent().getByteArrayExtra("captured_image");
        String mode = getIntent().getStringExtra("mode");
        boolean isViewMode = "view".equals(mode);


        if (byteArray != null) {
            capturedBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            itemImageView.setImageBitmap(capturedBitmap);

            detectedType = getIntent().getStringExtra("detected_type");
            if (detectedType != null) {
                Toast.makeText(this, "Received detected_type: " + detectedType, Toast.LENGTH_SHORT).show();
                setSpinnerSelection(typeSpinner, detectedType);
            }

            String detectedColor = getIntent().getStringExtra("detected_color");
            if (detectedColor != null && !detectedColor.isEmpty()) {
                colorInput.setText(detectedColor);
            }

        } else if (isViewMode) {
            // Populate fields
            String name = getIntent().getStringExtra("name");
            String type = getIntent().getStringExtra("type");
            String color = getIntent().getStringExtra("color");
            String gender = getIntent().getStringExtra("gender");
            String season = getIntent().getStringExtra("season");
            String usage = getIntent().getStringExtra("usage");
            String imageUrl = getIntent().getStringExtra("imageUrl");

            nameInput.setText(name);
            colorInput.setText(color);
            setSpinnerSelection(typeSpinner, type);
            setSpinnerSelection(genderSpinner, gender);
            setSpinnerSelection(seasonSpinner, season);
            setSpinnerSelection(usageSpinner, usage);
            Glide.with(this).load(imageUrl).into(itemImageView);

            // Save to originalItem for cancel behavior
            originalItem = new ClosetItem(name, type, color, gender, season, usage, imageUrl);
        }

        setFieldsEnabled(!isViewMode);

        saveButton.setVisibility(isViewMode ? View.GONE : View.VISIBLE);
        cancelButton.setVisibility(isViewMode ? View.GONE : View.VISIBLE);
        editButton.setVisibility(isViewMode ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(isViewMode ? View.VISIBLE : View.GONE);
    }


    private void setupButtons() {
        saveButton.setOnClickListener(v -> handleSave());
        editButton.setOnClickListener(v -> switchToEditMode());
        deleteButton.setOnClickListener(v -> handleDelete());
        cancelButton.setOnClickListener(v -> {
            String mode = getIntent().getStringExtra("mode");
            if ("new".equals(mode)) {
                // Add new photo
                finish();
            } else {
                // User was editing an existing item
                revertToViewMode();
            }
        });

    }

    private void switchToEditMode() {
        setFieldsEnabled(true);
        saveButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
        editButton.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
    }

    private void revertToViewMode() {
        setFieldsEnabled(false);
        saveButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        editButton.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.VISIBLE);

        if (originalItem != null) {
            nameInput.setText(originalItem.getName());
            colorInput.setText(originalItem.getColor());
            setSpinnerSelection(typeSpinner, originalItem.getType());
            setSpinnerSelection(genderSpinner, originalItem.getGender());
            setSpinnerSelection(seasonSpinner, originalItem.getSeason());
            setSpinnerSelection(usageSpinner, originalItem.getUsage());
        }
    }



    private void handleSave() {
        String name = nameInput.getText().toString().trim();
        String color = colorInput.getText().toString().trim();
        String type = typeSpinner.getSelectedItem().toString();
        String gender = genderSpinner.getSelectedItem().toString();
        String season = seasonSpinner.getSelectedItem().toString();
        String usage = usageSpinner.getSelectedItem().toString();

        if (name.isEmpty() || color.isEmpty()) {
            Toast.makeText(this, "Please fill in name and color.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String docId = getIntent().getStringExtra("documentId");

        // If editing an existing item
        if (docId == null && capturedBitmap != null) {
            // Upload image and add new item
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            byte[] imageData = stream.toByteArray();
            String filename = UUID.randomUUID().toString() + ".jpg";

            StorageReference imageRef = FirebaseStorage.getInstance()
                    .getReference().child("closet/" + uid + "/" + filename);

            imageRef.putBytes(imageData)
                    .continueWithTask(task -> imageRef.getDownloadUrl())
                    .addOnSuccessListener(uri -> {
                        ClosetItem item = new ClosetItem(name, type, color, gender, season, usage, uri.toString());
                        db.collection("user").document(uid).collection("closet")
                                .add(item)
                                .addOnSuccessListener(docRef -> {
                                    startActivity(new Intent(this, ClosetActivity.class));
                                    finish();
                                });
                    });

        } else if (docId != null) {
            //Editing existing item
            Map<String, Object> updatedFields = new HashMap<>();
            updatedFields.put("name", name);
            updatedFields.put("type", type);
            updatedFields.put("color", color);
            updatedFields.put("gender", gender);
            updatedFields.put("season", season);
            updatedFields.put("usage", usage);

            db.collection("user").document(uid)
                    .collection("closet").document(docId)
                    .update(updatedFields)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
                        revertToViewMode(); // go back to View Mode
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void handleDelete() {
        String docId = getIntent().getStringExtra("documentId");
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String imageUrl = getIntent().getStringExtra("imageUrl");

        if (docId == null) {
            Toast.makeText(this, "Missing document ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // First delete Firestore document
        FirebaseFirestore.getInstance()
                .collection("user").document(uid)
                .collection("closet").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Then delete from Storage
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                                .delete()
                                .addOnSuccessListener(unused -> Log.d("Delete", "Image deleted from Storage"))
                                .addOnFailureListener(e -> Log.w("Delete", "Image deletion failed", e));
                    }

                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show();
                });
    }


}
