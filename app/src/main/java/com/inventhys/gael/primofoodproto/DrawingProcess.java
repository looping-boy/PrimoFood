package com.inventhys.gael.primofoodproto;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.text.TextUtils;

import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.ArrayList;
import java.util.List;

public class DrawingProcess {

    private final float FRAME_WIDTH_PERCENT = 0.5f;
    private final float FRAME_RATIO = 4 / 3f;
    private Rect tmpRect = new Rect();

    public BitmapCanvas drawBoxes(Bitmap bitmap, ArrayList<RectInformation> boxes, int color, int strokeWidth) {
        BitmapCanvas bitmapCanvas = new BitmapCanvas();
        Paint paint = new Paint();

        bitmapCanvas.bitmap = bitmap;
        bitmapCanvas.canvas = new Canvas(bitmapCanvas.bitmap);

//        paint.setStrokeWidth(3);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setColor(color);
//        bitmapCanvas.canvas.drawRect(1, 1, bitmapCanvas.bitmap.getWidth() - 2, bitmapCanvas.bitmap.getHeight() - 2, paint);

        for (RectInformation block : boxes) {
            Rect rect = new Rect();
            rect.left = (int) block.x1;
            rect.right = (int) block.x2;
            rect.top = (int) block.y1;
            rect.bottom = (int) block.y2;

            paint.setColor(color);
            paint.setStrokeWidth(strokeWidth);
            paint.setStyle(Paint.Style.STROKE);
            bitmapCanvas.canvas.drawRect(rect, paint);
        }
        return bitmapCanvas;
    }

    public BitmapCanvas updateWithBarcodes(List<Barcode> blocks, boolean cleanBitmap, int maxWidth, boolean skipIfOutOfBounds, Bitmap bitmap) {
        BitmapCanvas bitmapCanvas = new BitmapCanvas();
        Paint paint = new Paint();
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        bitmapCanvas.bitmap = mutableBitmap;
        bitmapCanvas.canvas = new Canvas(bitmapCanvas.bitmap);

        if (bitmapCanvas.canvas != null) {

            if (cleanBitmap) {
                bitmapCanvas.canvas.drawColor(0xFFFFFFFF, PorterDuff.Mode.CLEAR);
            }

            float boxW = FRAME_WIDTH_PERCENT * bitmapCanvas.bitmap.getWidth();
            float boxH = boxW / FRAME_RATIO;
            float boxX = (bitmapCanvas.bitmap.getWidth() - boxW) / 2;
            float boxY = (bitmapCanvas.bitmap.getHeight() - boxH) / 2;

            float textSize = maxWidth * 0.07f;

            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFFFFFF00);
            bitmapCanvas.canvas.drawRect(1, 1, bitmapCanvas.bitmap.getWidth() - 2, bitmapCanvas.bitmap.getHeight() - 2, paint);

            int yStep = 0;
            for (Barcode block : blocks) {
                Rect rect = block.getBoundingBox();
                if (rect == null) {
                    continue;
                }

                // ignore barcode out of the frame box
                if (skipIfOutOfBounds) {
                    float middleRectX = rect.centerX();
                    float middleRectY = rect.centerY();
                    if ((middleRectX < boxX) || (middleRectX > boxX + boxW) ||
                            (middleRectY < boxY) || (middleRectY > boxY + boxH)) {
                        continue;
                    }
                }

                paint.setColor(0x77000000); // Darker
                paint.setStyle(Paint.Style.FILL);
                bitmapCanvas.canvas.drawRect(rect, paint);

                paint.setColor(0xFFFF0000); // Red Border
                paint.setStyle(Paint.Style.STROKE);
                bitmapCanvas.canvas.drawRect(rect, paint);

                paint.setStyle(Paint.Style.FILL);
                String text = block.getDisplayValue();
                if (!TextUtils.isEmpty(text)) {
                    paint.setTextSize(textSize);
                    paint.getTextBounds(text, 0, text.length(), tmpRect);
                    float x = -tmpRect.left;
                    float y = yStep - tmpRect.top;

                    paint.setColor(0x77000000); // Darker
                    bitmapCanvas.canvas.drawRect(0, yStep, bitmapCanvas.bitmap.getWidth(), yStep + 134, paint);

                    paint.setColor(0xFF00FF00);   // GREEN texts
                    bitmapCanvas.canvas.drawText(text, x, y, paint);
                    yStep += textSize * 1.2f;
                }
            }
        }

        return bitmapCanvas;
    }
}
