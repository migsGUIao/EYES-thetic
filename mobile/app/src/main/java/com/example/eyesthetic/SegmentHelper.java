package com.example.eyesthetic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.ByteBufferExtractor;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter;
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter.ImageSegmenterOptions;
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult;

import java.nio.ByteBuffer;

public class SegmentHelper {
    private ImageSegmenter segmenter;

    public SegmentHelper(Context context) {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("selfie_multiclass_256x256.tflite")
                    .build();

            ImageSegmenterOptions options = ImageSegmenterOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setOutputCategoryMask(true)
                    .build();

            segmenter = ImageSegmenter.createFromOptions(context, options);
        } catch (Exception e) {
            Log.e("ClothingSegmenter", "Failed to initialize", e);
        }
    }

    public Bitmap extractClothingMaskFromByteBuffer(
            ByteBuffer byteBuffer,
            Bitmap originalBitmap,
            int width,
            int height
    ) {
        // Prepare pixel arrays
        int[] inputPixels = new int[width * height];
        int[] clothingPixels = new int[width * height];

        originalBitmap.getPixels(inputPixels, 0, width, 0, 0, width, height);

        // ByteBuffer may need to be rewound
        byteBuffer.rewind();

        for (int i = 0; i < width * height; i++) {
            int label = byteBuffer.get() & 0xFF; // Get unsigned byte as int
            if (label == 4 || label == 5) { // 4 = upper clothing, 5 = lower clothing
                clothingPixels[i] = inputPixels[i]; // Keep original color
            } else {
                clothingPixels[i] = Color.TRANSPARENT; // Mask out everything else
            }
        }

        return Bitmap.createBitmap(clothingPixels, width, height, Bitmap.Config.ARGB_8888);
    }
    public Bitmap extractClothingOnly(Bitmap inputBitmap) {
        if (segmenter == null) return inputBitmap;

        MPImage mpImage = new BitmapImageBuilder(inputBitmap).build();
        ImageSegmenterResult result = segmenter.segment(mpImage);

        if (result.categoryMask().isEmpty()) return inputBitmap;

        MPImage maskImage = result.categoryMask().get();
        ByteBuffer maskBuffer = ByteBufferExtractor.extract(maskImage);

        return extractClothingMaskFromByteBuffer(
                maskBuffer,
                inputBitmap,
                inputBitmap.getWidth(),
                inputBitmap.getHeight()
        );
    }

}