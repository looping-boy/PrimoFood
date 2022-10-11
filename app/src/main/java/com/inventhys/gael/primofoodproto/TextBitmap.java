package com.inventhys.gael.primofoodproto;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.text.TextUtils;

import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.text.Text;

import java.util.List;

public class TextBitmap {

    private final float FRAME_WIDTH_PERCENT = 0.5f;
    private final float FRAME_RATIO = 4/3f;

    public class Data {
        Bitmap bitmap;
        Canvas canvas;
    }
    private Data data = new Data();
    private Paint paint = new Paint();
    private Rect tmpRect = new Rect();

    public Data getOrCreate(ImageProxy imageProxy) {
        ImageInfo imageInfo = imageProxy.getImageInfo();
        boolean rotated = ((imageInfo.getRotationDegrees() != 0) && (imageInfo.getRotationDegrees() != 180));
        int width = rotated ? imageProxy.getHeight() : imageProxy.getWidth();
        int height = rotated ? imageProxy.getWidth() : imageProxy.getHeight();
        return getOrCreate(width, height);
    }

    public Data getOrCreate(int width, int height) {
        if ((data.bitmap == null) || (data.bitmap.isRecycled()) || (data.bitmap.getWidth() != width) || (data.bitmap.getHeight() != height)) {
            data.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            data.canvas = (data.bitmap == null) ? null : new Canvas(data.bitmap);
        }
        return data;
    }

//    public void setData(Bitmap bitmap, Canvas canvas) {
//        data.bitmap = bitmap;
//        data.canvas = canvas;
//    }

    public void updateWithFrame(boolean cleanBitmap) {
        if (data.canvas != null) {

            if (cleanBitmap) {
                data.canvas.drawColor(0xFFFFFFFF, PorterDuff.Mode.CLEAR);
            }

            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFFFF00FF);
            float w = FRAME_WIDTH_PERCENT * data.bitmap.getWidth();
            float h = w / FRAME_RATIO;
            float x = (data.bitmap.getWidth() - w) / 2;
            float y = (data.bitmap.getHeight() - h) / 2;
            data.canvas.drawRect(x,y, x + w, y + h, paint);

        }
    }

    public void updateWithBlocks(List<Text.TextBlock> blocks, boolean cleanBitmap) {
        if (data.canvas != null) {

            if (cleanBitmap) {
                data.canvas.drawColor(0xFFFFFFFF, PorterDuff.Mode.CLEAR);
            }

            float boxW = FRAME_WIDTH_PERCENT * data.bitmap.getWidth();
            float boxH = boxW / FRAME_RATIO;
            float boxX = (data.bitmap.getWidth() - boxW) / 2;
            float boxY = (data.bitmap.getHeight() - boxH) / 2;

            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFFFFFF00);
            data.canvas.drawRect(1,1, data.bitmap.getWidth() - 2, data.bitmap.getHeight() - 2, paint);

            for (Text.TextBlock block : blocks) {
                Rect rect = block.getBoundingBox();

                // ignore texts out of the frame box
                float middleRectX = rect.centerX();
                float middleRectY = rect.centerY();
                if ((middleRectX < boxX) || (middleRectX > boxX + boxW) ||
                        (middleRectY < boxY) || (middleRectY > boxY + boxH)) {
                    continue;
                }

                paint.setColor(0x77000000); // Darker
                paint.setStyle(Paint.Style.FILL);
                data.canvas.drawRect(rect, paint);

                paint.setColor(0xFFFF0000); // Red Border
                paint.setStyle(Paint.Style.STROKE);
                data.canvas.drawRect(rect, paint);

                paint.setStyle(Paint.Style.FILL);   // White texts
                paint.setColor(0xFFFFFFFF);
                List<Text.Line> lines = block.getLines();
                for(Text.Line line : lines) {
                    String text = line.getText();
                    if (!TextUtils.isEmpty(text)) {
                        rect = line.getBoundingBox();
                        paint.setTextSize(rect.bottom - rect.top);
                        paint.getTextBounds(text, 0, text.length(), tmpRect);
                        float x = rect.left - tmpRect.left + ((rect.right - rect.left - tmpRect.width()) / 2f);
                        float y = rect.top - tmpRect.top + ((rect.bottom - rect.top - tmpRect.height()) / 2f);
                        data.canvas.drawText(text, x, y, paint);
                    }
                }
            }
        }
    }

    public void updateWithBarcodes(List<Barcode> blocks, boolean cleanBitmap, int maxWidth) {
        if (data.canvas != null) {

            if (cleanBitmap) {
                data.canvas.drawColor(0xFFFFFFFF, PorterDuff.Mode.CLEAR);
            }

            float textSize = maxWidth * 0.07f;

            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFFFFFF00);
            data.canvas.drawRect(1,1, data.bitmap.getWidth() - 2, data.bitmap.getHeight() - 2, paint);

            int yStep = 0;
            for (Barcode block : blocks) {
                Rect rect = block.getBoundingBox();

                paint.setColor(0x77000000); // Darker
                paint.setStyle(Paint.Style.FILL);
                data.canvas.drawRect(rect, paint);

                paint.setColor(0xFFFF0000); // Red Border
                paint.setStyle(Paint.Style.STROKE);
                data.canvas.drawRect(rect, paint);

                paint.setStyle(Paint.Style.FILL);
                String text = block.getDisplayValue();
                if (!TextUtils.isEmpty(text)) {
                    paint.setTextSize(textSize);
                    paint.getTextBounds(text, 0, text.length(), tmpRect);
                    float x = - tmpRect.left;
                    float y = yStep - tmpRect.top;

                    paint.setColor(0x77000000); // Darker
                    data.canvas.drawRect(0, yStep, data.bitmap.getWidth(),yStep+134, paint);

                    paint.setColor(0xFF00FF00);   // GREEN texts
                    data.canvas.drawText(text, x, y, paint);
                    yStep += textSize * 1.2f;
                }
            }
        }
    }

}
