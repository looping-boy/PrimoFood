package com.inventhys.gael.primofoodproto;

import static org.opencv.dnn.Dnn.NMSBoxes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.text.TextUtils;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.inventhys.gael.primofoodproto.ml.Pricetag;

import org.opencv.core.MatOfRect2d;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Result;

public class PriceTagProcess {

    private final float FRAME_WIDTH_PERCENT = 0.5f;
    private final float FRAME_RATIO = 4 / 3f;
    private final short IMAGE_SIZE = 320;

    ArrayList<RectInformation> boxesStored = new ArrayList<>();

    private Rect tmpRect = new Rect();

    public ArrayList<RectInformation> getBoxesWithAI(Context context, Bitmap bitmap) {
        ArrayList<RectInformation> boxes = new ArrayList<>();
        boxesStored = new ArrayList<>();
        try {
            // GET THE MODEL :
            Pricetag model = Pricetag.newInstance(context);

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
            Pricetag.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputAsTensorBuffer();
            float[] outputData = outputFeature0.getFloatArray();

            // TRANSPOSE IT : "-" to "|"
            float[] outputDataTransposed = new float[12600];
            int increment = 0;
            for (int j = 0; j < 2100; j++) {
                for (int i = 0; i < 12600; i += 2100) {
                    outputDataTransposed[increment] = outputData[i + j];
                    increment++;
                }
            }

//            RectInformation.Result finalResult = new RectInformation.Result();
            // GET MAX PROBABILITY BOXES :
            RectInformation.Result finalResult = RectInformation.parseRowDict(outputDataTransposed);
// Parse the row data to get bboxes, classes, and probabilities
            List<double[]> bboxes = finalResult.bboxes;
            List<Double> probabilities = finalResult.probabilities;

            // Perform Non-Maximum Suppression (NMS) to filter out overlapping boxes
//            List<Integer> cleanBoxIdx = NMSBoxes(new MatOfRect(boxes), probabilities, 0.5, 0.8);


            // THERE IS DUPLICATED BOX, WE NEED TO REMOVE THEM :
            int bias = 2; // FORKS BETWEEN -> TODO : should depend of the number size.
            boolean removed = false;
            for (int i = 0; i < boxesStored.size(); i++) {
                if (removed) {
                    i = i - 1;
                    removed = false;
                }
                for (int j = i + 1; j < boxesStored.size(); j++) {
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

        return boxes;
    }

    public String giveThePriceString() {
        StringBuilder price = new StringBuilder();

        for (int i = 0; i < boxesStored.size(); i++) {
            price.append(boxesStored.get(i).number);
        }

        return price.toString();
    }

    // --------------------------------------------------------------------







}
