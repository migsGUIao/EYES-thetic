package com.example.eyesthetic;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import java.io.ByteArrayOutputStream;


import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
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

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.support.image.TensorImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView detectionStatusText;
    private TextView clothingStatusText;
    private Bitmap latestBitmap = null;
    private volatile boolean isClassifying = false;


    private ExecutorService cameraExecutor;
    private ImageClassifier classifier;
    private YuvToRgbConverter yuvToRgbConverter;
    private TextToSpeech tts;
    private long lastTtsTime = 0;
    private static final long TTS_INTERVAL_MS = 2500; // every 2.5s max
    private SegmentHelper segmentHelper;
    private boolean isGuidanceEnabled = true;
    private ImageButton guidanceToggleButton;

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

        segmentHelper = new SegmentHelper(this);
        yuvToRgbConverter = new YuvToRgbConverter(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
            Log.d("TTS_DEBUG", "TTS initialized with status: " + status);
        });

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        isGuidanceEnabled = prefs.getBoolean("guidanceEnabled", true);
        guidanceToggleButton = findViewById(R.id.guidanceToggleButton);
        updateGuidanceIcon();

        prefs.edit().putBoolean("guidanceEnabled", isGuidanceEnabled).apply();
        guidanceToggleButton.setOnClickListener(v -> {
            isGuidanceEnabled = !isGuidanceEnabled;

            // Save preference
            prefs.edit().putBoolean("guidanceEnabled", isGuidanceEnabled).apply();

            // Update icon and description
            updateGuidanceIcon();
        });

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

                Bitmap segmentedMask = segmentHelper.extractClothingOnly(latestBitmap);
                Bitmap cropped = cropSegmentedRegionWithPadding(latestBitmap, segmentedMask, 20);
                if (cropped == null || cropped.getWidth() == 0 || cropped.getHeight() == 0) {
                    Toast.makeText(this, "Invalid cropped image", Toast.LENGTH_SHORT).show();
                    return;
                }

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                cropped.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                Bitmap clothingOnlyBitmap = segmentHelper.extractClothingOnly(cropped);

                // DEBUG oNLY for showing segmented clothing image
                ImageView debugView = new ImageView(this);
                debugView.setImageBitmap(clothingOnlyBitmap);
                new AlertDialog.Builder(this)
                        .setTitle("Segmentation Output")
                        .setView(debugView)
                        .setPositiveButton("OK", null)
                        .show();


                String colorName = getDominantColorWithKMeans(clothingOnlyBitmap, 3);

                Log.i("ColorDebug", "Final detected color: " + colorName);
                Toast.makeText(this, "Detected color: " + colorName, Toast.LENGTH_SHORT).show();


                String matchedLabel = null;
                String mappedType = null;

                try {
                    //List<Classifications> results = classifier.classify(TensorImage.fromBitmap(latestBitmap));

                    if (cropped == null || cropped.getWidth() <= 1 || cropped.getHeight() <= 1) {
                        Log.e("CrashDebug", "Invalid cropped bitmap passed to classifier");
                        Toast.makeText(this, "Image too small or invalid for classification", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (cropped.getConfig() != Bitmap.Config.ARGB_8888) {
                        cropped = cropped.copy(Bitmap.Config.ARGB_8888, false);
                    }

                    TensorImage image = TensorImage.fromBitmap(cropped);
                    List<Classifications> results = classifier.classify(image);

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
                    Toast.makeText(this, "Classification failed", Toast.LENGTH_SHORT).show();
                    return;
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
                intent.putExtra("detected_color", colorName);
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

        // Prevent re-entry while the previous classification is still running
        if (mediaImage == null || classifier == null || isClassifying) {
            imageProxy.close();
            return;
        }

        isClassifying = true;

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
                            if (label.contains(clothing.toLowerCase()) && score > 0.6f) {
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

            if (latestBitmap != null && isClothingDetected) {
                long now = System.currentTimeMillis();
                if (now - lastTtsTime >= TTS_INTERVAL_MS) {
                    SegmentHelper segmentHelper = new SegmentHelper(this);
                    Bitmap segmented = segmentHelper.extractClothingOnly(latestBitmap);
                    Log.d("TTS_DEBUG", "Segmented image size: " + segmented.getWidth() + "x" + segmented.getHeight());
                    String hint = getFramingGuidance(segmented);
                    Log.d("TTS_DEBUG", "Framing guidance = " + hint);
                    if (isGuidanceEnabled) {
                        speak(hint);
                    }
                    lastTtsTime = now;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isClassifying = false;
            imageProxy.close();
        }
    }

    private String getFramingGuidance(Bitmap maskBitmap) {
        int width = maskBitmap.getWidth();
        int height = maskBitmap.getHeight();
        long sumX = 0, sumY = 0, count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = maskBitmap.getPixel(x, y);
                if (Color.alpha(pixel) > 128) {
                    sumX += x;
                    sumY += y;
                    count++;
                }
            }
        }

        if (count == 0) return "No clothing detected. Try again.";

        float centerX = sumX / (float) count;
        float centerY = sumY / (float) count;
        float deltaX = centerX - (width / 2.0f);
        float deltaY = centerY - (height / 2.0f);

        if (count < 5000) return "Move closer to the camera.";
        if (deltaX < -40) return "Move camera slightly left.";
        if (deltaX > 40) return "Move camera slightly right.";
        if (deltaY < -40) return "Move camera slightly up.";
        if (deltaY > 40) return "Move camera slightly down.";

        return "Clothing item detected.";
    }



    public String mapHSVToColorName(float hue, float sat, float val) {
        if (val < 0.2) return "Black";
        if (val > 0.9 && sat < 0.1) return "White";
        if (sat < 0.25) return "Gray";

        if (hue < 15 || hue >= 345) return "Red";
        if (hue < 30) return "Orange";
        if (hue < 45) return "Brown";
        if (hue < 65) return "Yellow";
        if (hue < 80) return "Beige";
        if (hue < 170) return "Green";
        if (hue < 200) return "Teal";
        if (hue < 250) return "Blue";
        if (hue < 290) return "Purple";
        return "Pink";
    }

    public Bitmap cropSegmentedRegionWithPadding(Bitmap original, Bitmap mask, int paddingPx) {
        int width = mask.getWidth();
        int height = mask.getHeight();

        int minX = width, minY = height;
        int maxX = 0, maxY = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = mask.getPixel(x, y);
                if (Color.alpha(pixel) > 128) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        // Fallback if segmentation failed
        if (minX >= maxX || minY >= maxY) {
            return original;
        }

        // Add padding, clamped to image bounds
        minX = Math.max(minX - paddingPx, 0);
        minY = Math.max(minY - paddingPx, 0);
        maxX = Math.min(maxX + paddingPx, width - 1);
        maxY = Math.min(maxY + paddingPx, height - 1);

        int cropWidth = maxX - minX;
        int cropHeight = maxY - minY;

        return Bitmap.createBitmap(original, minX, minY, cropWidth, cropHeight);
    }

    public String getDominantColorWithKMeans(Bitmap bitmap, int K) {
        List<Scalar> pixels = new ArrayList<>();

        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                int alpha = Color.alpha(pixel);
                if (alpha < 128) continue;

                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                float[] hsv = new float[3];
                Color.RGBToHSV(r, g, b, hsv);
                if (hsv[2] < 0.2f) continue;

                pixels.add(new Scalar(b, g, r)); // OpenCV uses BGR
            }
        }

        if (pixels.size() < 10) {
            Log.w("ColorDebug", "Too few valid pixels for KMeans: " + pixels.size());
            return "Unknown";
        }

        Mat samples = new Mat(pixels.size(), 3, CvType.CV_32F);
        for (int i = 0; i < pixels.size(); i++) {
            Scalar color = pixels.get(i);
            samples.put(i, 0, (float) color.val[0]); // B
            samples.put(i, 1, (float) color.val[1]); // G
            samples.put(i, 2, (float) color.val[2]); // R
        }

        Mat labels = new Mat();
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0);
        Core.kmeans(samples, K, labels, criteria, 3, Core.KMEANS_PP_CENTERS, centers);

        int[] counts = new int[K];
        for (int i = 0; i < labels.rows(); i++) {
            int label = (int) labels.get(i, 0)[0];
            counts[label]++;
        }

        int dominantIndex = 0;
        for (int i = 1; i < K; i++) {
            if (counts[i] > counts[dominantIndex]) {
                dominantIndex = i;
            }
        }

        centers.convertTo(centers, CvType.CV_64F);
        double[] dominantColor = new double[3];
        centers.get(dominantIndex, 0, dominantColor); // B, G, R

        int r = (int) dominantColor[2];
        int g = (int) dominantColor[1];
        int b = (int) dominantColor[0];

        Log.i("ColorDebug", "Dominant BGR: " + Arrays.toString(dominantColor));
        Log.i("ColorDebug", "R: " + r + ", G: " + g + ", B: " + b);

        float[] hsv = new float[3];
        Color.RGBToHSV(r, g, b, hsv);
        Log.i("ColorDebug", "Hue: " + hsv[0] + ", Sat: " + hsv[1] + ", Val: " + hsv[2]);

        return mapHSVToColorName(hsv[0], hsv[1], hsv[2]);
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

    private void speak(String text) {
        if (!isGuidanceEnabled) {
            Log.d("TTS_DEBUG", "Guidance is OFF — skipping TTS.");
            return;
        }

        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am != null && am.isEnabled()) {
            if (tts != null && !tts.isSpeaking()) {
                Log.d("TTS_DEBUG", "Speaking: " + text);
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Log.d("TTS_DEBUG", "TTS is null or already speaking");
            }
        } else {
            Log.d("TTS_DEBUG", "AccessibilityManager not enabled");
        }
    }



    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        return am != null && am.isEnabled();
    }
    private boolean isTalkBackEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am == null) return false;

        List<AccessibilityServiceInfo> services = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN);
        for (AccessibilityServiceInfo service : services) {
            if (service.getResolveInfo().serviceInfo.name.toLowerCase().contains("talkback")) {
                return true;
            }
        }
        return false;
    }

    private void updateGuidanceIcon() {
        if (isGuidanceEnabled) {
            guidanceToggleButton.setImageResource(android.R.drawable.ic_lock_silent_mode_off); // sound on
            guidanceToggleButton.setContentDescription("Voice guidance is on. Tap to turn off.");
        } else {
            guidanceToggleButton.setImageResource(android.R.drawable.ic_lock_silent_mode); // sound off
            guidanceToggleButton.setContentDescription("Voice guidance is off. Tap to turn on.");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (yuvToRgbConverter != null) yuvToRgbConverter.destroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
