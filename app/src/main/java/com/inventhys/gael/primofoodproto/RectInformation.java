package com.inventhys.gael.primofoodproto;

import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Rect2d;
import org.opencv.dnn.Dnn;

import java.util.ArrayList;
import java.util.List;

public class RectInformation {
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

    public static RectInformation parseRow(float[] data) {
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

//------------------------------------------------------------------------



    public static OutputDataRefactor parseRowDict(float[] outputDataTransposed, int column) {
        OutputDataRefactor result = new OutputDataRefactor();

        for (int i = 0; i < 2100; i++) {
            float[] row = new float[column];
            System.arraycopy(outputDataTransposed, i * column, row, 0, column);
            float xc = row[0];
            float yc = row[1];
            float w = row[2];
            float h = row[3];
            float x1 = xc - w / 2;
            float y1 = yc - h / 2;
            float x2 = xc + w / 2;
            float y2 = yc + h / 2;

            float maxProbability = row[4];
            int maxClassId = 0;
            float maxProb = maxProbability;

            for (int j = 5; j < row.length; j++) {
                if (row[j] > maxProb) {
                    maxProb = row[j];
                    maxClassId = j - 4;
                }
            }

            result.bboxes.add(new float[]{x1, y1, x2, y2});
            result.classes.add((float) maxClassId);
            result.probabilities.add(maxProb);
        }

        return result;
    }

    public static List<Integer> NMSBoxes(List<float[]> bboxes, List<Float> probabilities, float scoreThreshold, float nmsThreshold, MatOfFloat scoresMat, MatOfRect2d boxesMat) {
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