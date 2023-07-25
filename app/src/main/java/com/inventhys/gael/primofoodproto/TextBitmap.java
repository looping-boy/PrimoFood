package com.inventhys.gael.primofoodproto;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.text.TextUtils;

import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.text.Text;
import com.inventhys.gael.primofoodproto.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class TextBitmap {

    private final float FRAME_WIDTH_PERCENT = 0.5f;
    private final float FRAME_RATIO = 4 / 3f;
    private final short IMAGE_SIZE = 320;

    ArrayList<RectInformation> boxesStored = new ArrayList<>();

    public class Data {
        Bitmap bitmap;
        Canvas canvas;
    }

    public static class RectInformation {
        float x1, y1, x2, y2;
        int number;
        float maxProb;

        public RectInformation(float x1, float y1, float x2, float y2, int number, float maxProb) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.number = number;
            this.maxProb = maxProb;
        }
    }

    private Rect tmpRect = new Rect();

//    public Data getOrCreate(ImageProxy imageProxy) {
//        ImageInfo imageInfo = imageProxy.getImageInfo();
//        boolean rotated = ((imageInfo.getRotationDegrees() != 0) && (imageInfo.getRotationDegrees() != 180));
//        int width = rotated ? imageProxy.getHeight() : imageProxy.getWidth();
//        int height = rotated ? imageProxy.getWidth() : imageProxy.getHeight();
//        return getOrCreate(width, height);
//    }

    public Data updatePictureWithBlockIA(Context context, Bitmap bitmap) {
        ArrayList<RectInformation> boxes = new ArrayList<>();
        boxesStored =  new ArrayList<>();
        try {
            // GET THE MODEL :
            Model model = Model.newInstance(context);

            // CREATE EMPTY ARRAYS :
            float[] floatArray = new float[320 * 320 * 3];
            int[] intValues = new int[IMAGE_SIZE * IMAGE_SIZE];

            // FILL INT[] IT WITH PIXELS :
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            int pixelIncrement = 0;
            int rgbIncrement = 0;

            // FILL FLOAT[] WITH RGBs :
            for (int i = 0; i < IMAGE_SIZE; i++) {
                for (int j = 0; j < IMAGE_SIZE; j++) {
                    int val = intValues[pixelIncrement++];// RGB
                    float red = Color.red(val) * 1.f / 255;
                    floatArray[rgbIncrement] = red;
                    rgbIncrement++;
                    float green = Color.green(val) * 1.f / 255;
                    floatArray[rgbIncrement] = green;
                    rgbIncrement++;
                    float blue = Color.blue(val) * 1.f / 255;
                    floatArray[rgbIncrement] = blue;
                    rgbIncrement++;
                }
            }

            // CREATE & FILL A TENSOR BUFFER :
            // SHAPE [ 1 image, 320 pixel, 320 pixel, 3 rvb]
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, IMAGE_SIZE, IMAGE_SIZE, 3}, DataType.FLOAT32);
            inputFeature0.loadArray(floatArray, new int[]{1, IMAGE_SIZE, IMAGE_SIZE, 3});

            // RUN THE MODEL & GET THE OUTPUT :
            // SHAPE [ 1 picture, 4 coordinates + 10 probabilities = 14 , 2100 boxes]
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputAsTensorBuffer();
            float[] outputData = outputFeature0.getFloatArray();

            // TRANSPOSE IT : "-" to "|"
            float[] outputDataTransposed = new float[29400];
            int increment = 0;
            for (int j = 0; j < 2100; j++) {
                for (int i = 0; i < 29400; i += 2100) {
                    outputDataTransposed[increment] = outputData[i + j];
                    increment++;
                }
            }

            // GET MAX PROBABILITY BOXES :
            for (int i = 0; i < 2100; i++) {
                float[] row = new float[14];
                System.arraycopy(outputDataTransposed, i * 14, row, 0, 14);
                RectInformation info = parseRow(row);
                if (info.maxProb > 0.5) {
                    boxes.add(new RectInformation(info.x1, info.y1, info.x2, info.y2, info.number, info.maxProb));
                    boxesStored.add(new RectInformation(info.x1, info.y1, info.x2, info.y2, info.number, info.maxProb));
                }
            }

            // THERE IS DUPLICATED BOX, WE NEED TO REMOVE THEM :
            int bias = 2; // FORKS BETWEEN -> TODO : should depend of the number size.
            boolean removed = false;
            for (int i = 0; i < boxesStored.size(); i++) {
                if (removed) {
                    i = i - 1;
                    removed = false;
                }
                for (int j = i + 1 ; j < boxesStored.size(); j++) {
                    if (boxesStored.get(i).x1 - bias < boxesStored.get(j).x1 && boxesStored.get(i).x1 + bias > boxesStored.get(j).x1 && boxesStored.get(i).y1 - bias < boxesStored.get(j).y1 && boxesStored.get(i).y1 + bias > boxesStored.get(j).y1) {
                        boxesStored.remove(j);
                        removed = true;
                    }
                }
            }

            model.close();

        } catch (IOException e) {
            // TODO Handle the exception
        }

        return updateWithBlocksAI(bitmap, boxes);
    }

    public RectInformation parseRow(float[] data) {
        float xc = data[0];
        float yc = data[1];
        float w = data[2];
        float h = data[3];
        float x1 = xc - w / 2;
        float y1 = yc - h / 2;
        float x2 = xc + w / 2;
        float y2 = yc + h / 2;

        float maxProbability = data[4];
        int maxClassId = 0;
        float maxProb = maxProbability;

        for (int i = 5; i < data.length; i++) {
            if (data[i] > maxProb) {
                maxProb = data[i];
                maxClassId = i - 4;
            }
        }

        return new RectInformation(x1, y1, x2, y2, maxClassId, maxProb);
    }

//    public void updateWithFrame(boolean cleanBitmap) {
//        if (data.canvas != null) {
//
//            if (cleanBitmap) {
//                data.canvas.drawColor(0xFFFFFFFF, PorterDuff.Mode.CLEAR);
//            }
//
//            paint.setStrokeWidth(3);
//            paint.setStyle(Paint.Style.STROKE);
//            paint.setColor(0xFFFF00FF);
//            float w = FRAME_WIDTH_PERCENT * data.bitmap.getWidth();
//            float h = w / FRAME_RATIO;
//            float x = (data.bitmap.getWidth() - w) / 2;
//            float y = (data.bitmap.getHeight() - h) / 2;
//            data.canvas.drawRect(x, y, x + w, y + h, paint);
//
//        }
//    }

    public Data updateWithBlocksAI(Bitmap bitmap, ArrayList<RectInformation> boxes) {
        Data data = new Data();
        Paint paint = new Paint();

        data.bitmap = bitmap;
        data.canvas = new Canvas(data.bitmap);

        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFFFFFF00);
        data.canvas.drawRect(1, 1, data.bitmap.getWidth() - 2, data.bitmap.getHeight() - 2, paint);

        for (RectInformation block : boxes) {
            Rect rect = new Rect();
            rect.left = (int) block.x1;
            rect.right = (int) block.x2;
            rect.top = (int) block.y1;
            rect.bottom = (int) block.y2;

            paint.setColor(0xFFFF0000);
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            data.canvas.drawRect(rect, paint);
        }
        return data;
    }

    public Data updateWithBarcodes(List<Barcode> blocks, boolean cleanBitmap, int maxWidth, boolean skipIfOutOfBounds, Bitmap bitmap) {
        Data data = new Data();
        Paint paint = new Paint();
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        data.bitmap = mutableBitmap;
        data.canvas = new Canvas(data.bitmap);

        if (data.canvas != null) {

            if (cleanBitmap) {
                data.canvas.drawColor(0xFFFFFFFF, PorterDuff.Mode.CLEAR);
            }

            float boxW = FRAME_WIDTH_PERCENT * data.bitmap.getWidth();
            float boxH = boxW / FRAME_RATIO;
            float boxX = (data.bitmap.getWidth() - boxW) / 2;
            float boxY = (data.bitmap.getHeight() - boxH) / 2;

            float textSize = maxWidth * 0.07f;

            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFFFFFF00);
            data.canvas.drawRect(1, 1, data.bitmap.getWidth() - 2, data.bitmap.getHeight() - 2, paint);

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
                data.canvas.drawRect(rect, paint);

                paint.setColor(0xFFFF0000); // Red Border
                paint.setStyle(Paint.Style.STROKE);
                data.canvas.drawRect(rect, paint);

                paint.setStyle(Paint.Style.FILL);
                String text = block.getDisplayValue();
                if (!TextUtils.isEmpty(text)) {
                    paint.setTextSize(textSize);
                    paint.getTextBounds(text, 0, text.length(), tmpRect);
                    float x = -tmpRect.left;
                    float y = yStep - tmpRect.top;

                    paint.setColor(0x77000000); // Darker
                    data.canvas.drawRect(0, yStep, data.bitmap.getWidth(), yStep + 134, paint);

                    paint.setColor(0xFF00FF00);   // GREEN texts
                    data.canvas.drawText(text, x, y, paint);
                    yStep += textSize * 1.2f;
                }
            }
        }

        return data;
    }

    public String giveThePriceString() {
        StringBuilder price = new StringBuilder();

        for (int i = 0; i < boxesStored.size(); i++) {
            price.append(boxesStored.get(i).number);
        }

        return price.toString();
    }

}
