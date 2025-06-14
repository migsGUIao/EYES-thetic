package com.example.eyesthetic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import java.nio.ByteBuffer;

public class YuvToRgbConverter {
    private final RenderScript rs;
    private final ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Allocation yuvAllocation = null;
    private Allocation rgbAllocation = null;

    public YuvToRgbConverter(Context context) {
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public Bitmap yuvToRgb(Context context, Image image, int rotationDegrees) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Image format must be YUV_420_888");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        byte[] yuvBytes = getDataFromImage(image);

        if (yuvAllocation == null || yuvAllocation.getBytesSize() != yuvBytes.length) {
            Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(yuvBytes.length);
            if (yuvAllocation != null) yuvAllocation.destroy();
            yuvAllocation = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
        }

        if (rgbAllocation == null || rgbAllocation.getType().getX() != width || rgbAllocation.getType().getY() != height) {
            Type.Builder rgbType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            if (rgbAllocation != null) rgbAllocation.destroy();
            rgbAllocation = Allocation.createTyped(rs, rgbType.create(), Allocation.USAGE_SCRIPT);
        }

        yuvAllocation.copyFrom(yuvBytes);

        yuvToRgbIntrinsic.setInput(yuvAllocation);
        yuvToRgbIntrinsic.forEach(rgbAllocation);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        rgbAllocation.copyTo(bmp);

        if (rotationDegrees != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        }

        return bmp;
    }

    private static byte[] getDataFromImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        int width = image.getWidth();
        int height = image.getHeight();

        int ySize = planes[0].getBuffer().remaining();
        int uSize = planes[1].getBuffer().remaining();
        int vSize = planes[2].getBuffer().remaining();

        byte[] data = new byte[ySize + uSize + vSize];

        planes[0].getBuffer().get(data, 0, ySize);

        ByteBuffer vBuffer = planes[2].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();

        vBuffer.get(data, ySize, vSize);
        uBuffer.get(data, ySize + vSize, uSize);

        return data;
    }

    public void destroy() {
        if (yuvAllocation != null) yuvAllocation.destroy();
        if (rgbAllocation != null) rgbAllocation.destroy();
        if (yuvToRgbIntrinsic != null) yuvToRgbIntrinsic.destroy();
        if (rs != null) rs.destroy();
    }
}
