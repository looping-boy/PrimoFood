package com.inventhys.gael.primofoodproto;

import java.util.ArrayList;
import java.util.List;

public class OutputDataRefactor {
    public List<float[]> bboxes;
    public List<Float> classes;
    public List<Float> probabilities;

    public OutputDataRefactor() {
        bboxes = new ArrayList<>();
        classes = new ArrayList<>();
        probabilities = new ArrayList<>();
    }
}