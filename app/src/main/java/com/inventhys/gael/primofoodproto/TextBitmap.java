package com.inventhys.gael.primofoodproto;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.text.TextUtils;

import com.google.mlkit.vision.text.Text;

import java.util.List;

public class TextBitmap {

    public class Data {
        Bitmap bitmap;
        Canvas canvas;
    }
    private Data data = new Data();
    private Paint paint = new Paint();
    private Rect tmpRect = new Rect();

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

    public void updateWithBlocks(List<Text.TextBlock> blocks) {
        if (data.canvas != null) {
            data.canvas.drawColor(0xFFFFFFFF, PorterDuff.Mode.CLEAR);

            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFFFFFF00);
            data.canvas.drawRect(1,1, data.bitmap.getWidth() - 2, data.bitmap.getHeight() - 2, paint);

            for (Text.TextBlock block : blocks) {
                Rect rect = block.getBoundingBox();

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


}
