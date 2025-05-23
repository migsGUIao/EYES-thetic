package com.example.eyesthetic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;

public class ClosetActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    BottomNavigationView bottomNavigationView;
    FloatingActionButton addToCloset;
    Bitmap uploadedPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet);

        mAuth = FirebaseAuth.getInstance();

        bottomNavigationView = findViewById(R.id.closetNavbar);
        addToCloset = findViewById(R.id.addToCloset);

        getPermission();

        addToCloset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });

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

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.closetIcon);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 11) {
            if(grantResults.length>0) {
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==10) {
            if (data != null) {
                Uri uri = data.getData();
                try {
                    uploadedPhoto = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    // TODO: add image to gallery view of favorites
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == 12) {
            uploadedPhoto = (Bitmap) data.getExtras().get("data");
            // TODO: add image to gallery view of favorites
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void getPermission() {
        // Not granted, ask user to grant permission
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ClosetActivity.this, new String[]{Manifest.permission.CAMERA}, 11);
        }
    }



    // Add to closet dialog
    private void showDialog() {
        LayoutInflater dialogInflater = getLayoutInflater();
        View customDialog = dialogInflater.inflate(R.layout.custom_closet_dialog, null);

//        TextView closetDesc = findViewById(R.id.closetDesc);
//        Button takePhotoBtn = findViewById(R.id.takePhotoBtn);
//        Button uploadBtn = findViewById(R.id.uploadBtn);

        // TODO: fix container size
        new MaterialAlertDialogBuilder(this, R.style.DialogTheme)
//                .setView(customDialog)
                .setTitle(R.string.closet_addDesc)
                .setNegativeButton(R.string.closet_takePhoto, (dialog, which) -> {
                    // TODO: open camera
                    Intent openCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(openCamera, 12);
                })
                .setPositiveButton(R.string.closet_upload, (dialog, which) -> {
                    // TODO: open gallery
                    Intent openGallery = new Intent();
                    openGallery.setAction(Intent.ACTION_GET_CONTENT);
                    openGallery.setType("image/*");
                    startActivityForResult(openGallery,10);
                })
                .show();

//        closetDialog.show();
//
//        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent openCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(openCamera, 12);
//            }
//        });
//
//        uploadBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent openGallery = new Intent();
//                openGallery.setAction(Intent.ACTION_GET_CONTENT);
//                openGallery.setType("image/*");
//                startActivityForResult(openGallery,10);
//            }
//        });
    }

}
