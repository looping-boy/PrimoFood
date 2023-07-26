package com.inventhys.gael.primofoodproto;

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
}