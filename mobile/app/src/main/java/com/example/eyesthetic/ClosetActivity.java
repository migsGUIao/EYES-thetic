package com.example.eyesthetic;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;

public class ClosetActivity extends AppCompatActivity {
    private ClosetAdapter adapter;
    private FirebaseFirestore db;
    private String uid;

    private List<ClosetItem> allItems = new ArrayList<>();
    private List<ClosetItem> tops = new ArrayList<>();
    private List<ClosetItem> bottoms = new ArrayList<>();


    static final int REQUEST_IMAGE_CAPTURE = 1;
    Uri photoURI;
    String currentPhotoPath;

    FirebaseAuth mAuth;
    BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet);

        mAuth = FirebaseAuth.getInstance();

        bottomNavigationView = findViewById(R.id.closetNavbar);
        FloatingActionButton fab = findViewById(R.id.addToCloset);
        fab.setOnClickListener(v -> showPopup());

        RecyclerView recyclerView = findViewById(R.id.closetRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new ClosetAdapter(item -> {
            Intent intent = new Intent(this, ClosetDetailsActivity.class);
            intent.putExtra("documentId", item.getId());
            intent.putExtra("mode", "view");
            intent.putExtra("name", item.getName());
            intent.putExtra("type", item.getType());
            intent.putExtra("color", item.getColor());
            intent.putExtra("gender", item.getGender());
            intent.putExtra("season", item.getSeason());
            intent.putExtra("usage", item.getUsage());
            intent.putExtra("imageUrl", item.getImageUrl());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // Load data from Firestore
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        loadClosetItems();

        findViewById(R.id.filterAllBtn).setOnClickListener(v -> adapter.submitList(allItems));
        findViewById(R.id.filterTopsBtn).setOnClickListener(v -> adapter.submitList(tops));
        findViewById(R.id.filterBottomsBtn).setOnClickListener(v -> adapter.submitList(bottoms));


        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.homeIcon) {
                Intent intent = new Intent(ClosetActivity.this, HomepageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);

                return true;
            } else if (id == R.id.closetIcon) {
                return true;
            } else if (id == R.id.favoritesIcon) {
                Intent intent = new Intent(ClosetActivity.this, FavoritesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
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

    private void showPopup() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_closet);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(true);
        dialog.show();


        Button takePhoto = dialog.findViewById(R.id.takePhotoBtn);
        //Button uploadImage = dialog.findViewById(R.id.uploadImageBtn);

        takePhoto.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        });


//        uploadImage.setOnClickListener(v -> {
//            dialog.dismiss();
//            // Handle image upload here
//        });

        dialog.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Photo saved at:\n" + currentPhotoPath, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadClosetItems() {
        db.collection("user").document(uid).collection("closet")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    //List<ClosetItem> items = new ArrayList<>();
                    allItems.clear();
                    tops.clear();
                    bottoms.clear();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Log.d("FirestoreData", "Raw data: " + doc.getData());

                        ClosetItem item = doc.toObject(ClosetItem.class);

                        if (item != null) {
                            item.setId(doc.getId());
                            allItems.add(item);

                            String type = item.getType();
                            if (type != null) {
                                if (type.equalsIgnoreCase("Top")) {
                                    tops.add(item);
                                } else if (type.equalsIgnoreCase("Bottom")) {
                                    bottoms.add(item);
                                }
                            }
                            Log.d("MappedItem", "Name: " + item.getName() +
                                    ", Type: " + item.getType() +
                                    ", Color: " + item.getColor() +
                                    ", Gender: " + item.getGender() +
                                    ", Season: " + item.getSeason() +
                                    ", Usage: " + item.getUsage() +
                                    ", ImageUrl: " + item.getImageUrl());
                        } else {
                            Log.w("MappedItem", "Failed to map document: " + doc.getId());
                        }
                    }
                    adapter.submitList(allItems);
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Failed to load closet items", e));
    }



    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.closetIcon);
        loadClosetItems();
    }

}
