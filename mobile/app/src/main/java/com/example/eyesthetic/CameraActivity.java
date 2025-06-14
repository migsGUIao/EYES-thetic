package com.example.eyesthetic;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import java.io.ByteArrayOutputStream;


import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.support.image.TensorImage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView detectionStatusText;
    private TextView clothingStatusText;
    private Bitmap latestBitmap = null;


    private ExecutorService cameraExecutor;
    private ImageClassifier classifier;
    private YuvToRgbConverter yuvToRgbConverter;

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private static final List<String> CLOTHING_LABELS = List.of(
            "abaya", "brassiere", "bra", "bandeau", "cardigan", "jersey", "T-shirt", "tee shirt", "sweatshirt",
            "kimono", "lab coat", "military uniform", "jean", "denim", "miniskirt", "mini",
            "overskirt", "sarong", "swimming trunks", "bathing trunks", "pajama", "pyjama"
    );

    private boolean isClothingDetected = false;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        detectionStatusText = findViewById(R.id.detectionStatusText);
        clothingStatusText = findViewById(R.id.clothingStatusText);

        yuvToRgbConverter = new YuvToRgbConverter(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        initClassifier();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        FloatingActionButton captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(v -> {
            if (isClothingDetected && latestBitmap != null) {
                //Toast.makeText(this, detectionStatusText.getText().toString(), Toast.LENGTH_SHORT).show();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                latestBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                String matchedLabel = null;
                String mappedType = null;

                try {
                    List<Classifications> results = classifier.classify(TensorImage.fromBitmap(latestBitmap));
                    for (Classifications classification : results) {
                        List<Category> categories = new ArrayList<>(classification.getCategories());
                        categories.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

                        // Check top 5 predictions
                        for (int i = 0; i < Math.min(5, categories.size()); i++) {
                            Category category = categories.get(i);
                            String label = category.getLabel();
                            float score = category.getScore();

                            // Check if label is in CLOTHING_LABELS
                            for (String clothing : CLOTHING_LABELS) {
                                if (label.toLowerCase().contains(clothing.toLowerCase()) && score > 0.5f) {
                                    matchedLabel = label;
                                    mappedType = mapLabelToType(label);
                                    break;
                                }
                            }
                            if (matchedLabel != null) break; // exit outer loop early if match found
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

//                if (matchedLabel != null && mappedType != null) {
//                    Toast.makeText(this, "Captured: " + matchedLabel + " → " + mappedType, Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(this, "No valid clothing label in top results", Toast.LENGTH_SHORT).show();
//                }

                // Proceed with intent
                Intent intent = new Intent(CameraActivity.this, ClosetDetailsActivity.class);
                intent.putExtra("mode", "new");
                intent.putExtra("captured_image", byteArray);
                if (mappedType != null) {
                    intent.putExtra("detected_type", mappedType);
                }
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "No clothing item detected. Try again.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private String mapLabelToType(String label) {
        List<String> tops = List.of("abaya", "brassiere", "bra", "bandeau", "cardigan", "jersey", "t-shirt", "tee shirt", "sweatshirt", "kimono", "lab coat", "military uniform");
        List<String> bottoms = List.of("jean", "denim", "miniskirt", "mini", "overskirt", "sarong", "swimming trunks", "bathing trunks", "pajama", "pyjama");

        label = label.toLowerCase();
        if (tops.stream().anyMatch(label::contains)) return "Top";
        if (bottoms.stream().anyMatch(label::contains)) return "Bottom";
        return null;
    }


    private void initClassifier() {
        try {
            classifier = ImageClassifier.createFromFile(this, "mobilenetv2.tflite");
            Toast.makeText(this, "Classifier initialized", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to init classifier: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(260, 260))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @ExperimentalGetImage
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null && classifier != null) {
            try {
                Bitmap bitmap = yuvToRgbConverter.yuvToRgb(this, mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 260, 260, true);
                latestBitmap = resizedBitmap;

                List<Classifications> results = classifier.classify(TensorImage.fromBitmap(resizedBitmap));

                runOnUiThread(() -> {
                    StringBuilder sb = new StringBuilder();
                    boolean foundClothing = false;

                    for (Classifications classification : results) {
                        List<Category> categories = new ArrayList<>(classification.getCategories());
                        categories.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

                        int topN = Math.min(5, categories.size());

                        for (int i = 0; i < topN; i++) {
                            Category category = categories.get(i);
                            String label = category.getLabel().toLowerCase();
                            float score = category.getScore();

                            sb.append(label).append(": ").append(String.format("%.2f", score)).append("\n");

                            for (String clothing : CLOTHING_LABELS) {
                                if (label.contains(clothing.toLowerCase()) && score > 0.6) {
                                    foundClothing = true;
                                    break;
                                }
                            }
                        }
                    }

                    detectionStatusText.setText(sb.toString());
                    isClothingDetected = foundClothing;
                    clothingStatusText.setText(foundClothing ? "Clothing item detected" : "Detecting clothing...");
                });


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        imageProxy.close();
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (yuvToRgbConverter != null) yuvToRgbConverter.destroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
