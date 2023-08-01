package com.inventhys.gael.primofoodproto;

import static com.inventhys.gael.primofoodproto.MainActivity.boxesMat;
import static com.inventhys.gael.primofoodproto.MainActivity.scoresMat;
import static org.opencv.dnn.Dnn.NMSBoxes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.inventhys.gael.primofoodproto.ml.Pricetag;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Rect2d;
import org.opencv.dnn.Dnn;
import org.opencv.utils.Converters;
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

            // PRESENT THE BOXES AN OTHER WAY :
            RectInformation.Result finalResult = RectInformation.parseRowDict(outputDataTransposed);

            // PERFORM A NON MAXIMUM SUPPRESSION TO FILTER OUT THE OVERLAPPING BOXES :
            List<Integer> cleanBoxIdx = NMSBoxes(finalResult.bboxes, finalResult.probabilities, 0.5f, 0.8f);

            for (int i = 0; i < cleanBoxIdx.size(); i++) {
                float x1 = finalResult.bboxes.get(cleanBoxIdx.get(i))[0];
                float y1 = finalResult.bboxes.get(cleanBoxIdx.get(i))[1];
                float x2 = finalResult.bboxes.get(cleanBoxIdx.get(i))[2];
                float y2 = finalResult.bboxes.get(cleanBoxIdx.get(i))[3];
                int number = Math.round(finalResult.classes.get(i));
                float maxProb = finalResult.probabilities.get(i);
                boxesStored.add(new RectInformation(x1, y1, x2, y2, number, maxProb));
                boxes.add(new RectInformation(x1, y1, x2, y2, number, maxProb));
            }

            model.close();

        } catch (IOException e) {
            // TODO Handle the exception
        }

        return boxes;
    }

    public static List<Integer> NMSBoxes(List<float[]> bboxes, List<Float> probabilities, float scoreThreshold, float nmsThreshold) {
        List<Integer> indices = new ArrayList<>();
        if (bboxes.isEmpty() || probabilities.isEmpty()) {
            return indices;
        }

        // Prepare the bounding boxes and scores for NMS
        for (Float probability : probabilities) {
            scoresMat.push_back(new MatOfFloat(probability));
        }

        for (float[] bbox : bboxes) {
            boxesMat.push_back(new MatOfRect2d(new Rect2d(bbox[0], bbox[1], bbox[2] - bbox[0], bbox[3] - bbox[1])));
        }

        // Perform Non-Maximum Suppression
        MatOfInt indicesMat = new MatOfInt();
        Dnn.NMSBoxes(boxesMat, scoresMat, (float) scoreThreshold, (float) nmsThreshold, indicesMat);

        // Convert the MatOfInt to a List of Integers
        if (!indicesMat.empty()) {
            int[] indicesArray = indicesMat.toArray();
            for (int i : indicesArray) {
                indices.add(i);
            }
        }

        return indices;
    }

}
