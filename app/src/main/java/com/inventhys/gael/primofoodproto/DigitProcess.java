package com.inventhys.gael.primofoodproto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.text.TextUtils;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.inventhys.gael.primofoodproto.ml.Digit;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DigitProcess {


    private final short IMAGE_SIZE = 320;

    ArrayList<RectInformation> boxesStored = new ArrayList<>();

    public ArrayList<RectInformation> getBoxesWithAI(Context context, Bitmap bitmap) {
        ArrayList<RectInformation> boxes = new ArrayList<>();
        boxesStored =  new ArrayList<>();
        try {
            // GET THE MODEL :
            Digit model = Digit.newInstance(context);

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
            Digit.Outputs outputs = model.process(inputFeature0);
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

//            // GET MAX PROBABILITY BOXES :
//            for (int i = 0; i < 2100; i++) {
//                float[] row = new float[14];
//                System.arraycopy(outputDataTransposed, i * 14, row, 0, 14);
//                RectInformation info = RectInformation.parseRow(row);
//                if (info.maxProb > 0.5) {
//                    boxes.add(new RectInformation(info.x1, info.y1, info.x2, info.y2, info.number, info.maxProb));
//                    boxesStored.add(new RectInformation(info.x1, info.y1, info.x2, info.y2, info.number, info.maxProb));
//                }
//            }

            // PRESENT THE BOXES AN OTHER WAY :
            OutputDataRefactor outputDataRefactor = RectInformation.parseRowDict(outputDataTransposed, 14);

            // PERFORM A NON MAXIMUM SUPPRESSION TO FILTER OUT THE OVERLAPPING BOXES :
            List<Integer> cleanBoxIdx = RectInformation.NMSBoxes(outputDataRefactor.bboxes, outputDataRefactor.probabilities, 0.5f, 0.7f);

            // STORE ALL THE BOXES :
            for (int i = 0; i < cleanBoxIdx.size(); i++) {
                float x1 = outputDataRefactor.bboxes.get(cleanBoxIdx.get(i))[0];
                float y1 = outputDataRefactor.bboxes.get(cleanBoxIdx.get(i))[1];
                float x2 = outputDataRefactor.bboxes.get(cleanBoxIdx.get(i))[2];
                float y2 = outputDataRefactor.bboxes.get(cleanBoxIdx.get(i))[3];
                int number = Math.round(outputDataRefactor.classes.get(cleanBoxIdx.get(i)));
                float maxProb = outputDataRefactor.probabilities.get(cleanBoxIdx.get(i));
                boxesStored.add(new RectInformation(x1, y1, x2, y2, number, maxProb));
                boxes.add(new RectInformation(x1, y1, x2, y2, number, maxProb));
            }


            if (!boxes.isEmpty()) {
                // DETECT THE MOST TALL NUMBER :
                float tallest = boxes.get(0).y2 - boxes.get(0).y1;
                int tallestId = 0;
                for (int j = 1; j < boxes.size(); j++) {
                    float boxHeight = boxes.get(j).y2 - boxes.get(j).y1;
                    if (tallest < boxHeight) {
                        tallest = boxHeight;
                        tallestId = j;
                    }
                }

                // PUT IT IN THE FIRST PLACE OF THE LIST :
                boxes.add(0, boxes.get(tallestId));
                boxes.remove(tallestId);
                boxesStored.add(0, boxesStored.get(tallestId));
                boxesStored.remove(tallestId);

                float bias = 10f;

                for (int k = 0; k < boxes.size(); k++) {
                    if (boxes.size() > 0 && k <= boxes.size() - 1) {
                        // REMOVE WHAT's ON THE LEFT OF TALLEST :
                        if (boxes.get(k).x1 < boxes.get(tallestId).x1) {
                            boxes.remove(k);
                            boxesStored.remove(k);
                            k--;
                            continue;
                        }
                        // REMOVE WHAT's ON THE TOP OF TALLEST :
                        if (boxes.get(k).y1 < boxes.get(tallestId).y1) {
                            boxes.remove(k);
                            boxesStored.remove(k);
                            k--;
                            continue;
                        }
                        // REMOVE WHAT's ON THE BOTTOM OF TALLEST :
                        if (boxes.get(k).y2 - bias > boxes.get(tallestId).y2) {
                            boxes.remove(k);
                            boxesStored.remove(k);
                            k--;
                            continue;
                        }
                        // REMOVE NUMBER IF TOO FAR AWAY FROM FIRST NUMBER :
                        float distanceX2 = boxes.get(tallestId).x1 + (boxes.get(0).y2 - boxes.get(0).y1);
                        if (boxes.get(k).x2 > distanceX2 + bias) {
                            boxes.remove(k);
                            boxesStored.remove(k);
                            k--;
                        }
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

        Comparator<RectInformation> x1Comparator = (box1, box2) -> Float.compare(box1.x1, box2.x1);
        boxesStored.sort(x1Comparator);

        for (int i = 0; i < boxesStored.size(); i++) {
            price.append(boxesStored.get(i).number);
        }

        return price.toString();
    }

}
