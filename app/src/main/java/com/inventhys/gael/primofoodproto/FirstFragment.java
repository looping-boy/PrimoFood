package com.inventhys.gael.primofoodproto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.inventhys.gael.primofoodproto.databinding.FragmentFirstBinding;

import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment {

    private static final String TAG = "LABELLING";
    private FragmentFirstBinding binding;

    int pictureIndex = 0;
    private int[] pictures = new int[]{
            R.drawable.tni,
            R.drawable.zoom,
            R.drawable.casino_20221004_121529,
            R.drawable.casino_20221004_121401,
            R.drawable.casino_20221004_121345,
            R.drawable.carrefour_middle_3_20220831,
            R.drawable.carrefour_middle_shelf_1,
            R.drawable.carrefour_middle_shelf_3,
            R.drawable.carrefour_top_1_20220831,
            R.drawable.carrefour_top_shelf_1,
            R.drawable.lidl_1_20220831,
            R.drawable.lidl_4_20220831
    };

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        updatePicture();

        binding.labellize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pictureResId = pictures[pictureIndex];
                Bitmap bitmap = loadBitmap(pictureResId);
                labellizePicture(bitmap);
            }
        });

        binding.recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pictureResId = pictures[pictureIndex];
                Bitmap bitmap = loadBitmap(pictureResId);
                recognizePicture(bitmap);
            }
        });

        binding.textrecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pictureResId = pictures[pictureIndex];
                Bitmap bitmap = loadBitmap(pictureResId);
                textRecognition(bitmap);
            }
        });

        binding.previouspicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pictureIndex == 0) {
                    pictureIndex = pictures.length - 1;
                } else {
                    --pictureIndex;
                }
                updatePicture();
            }
        });

        binding.nextpicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pictureIndex == pictures.length - 1) {
                    pictureIndex = 0;
                } else {
                    ++pictureIndex;
                }
                updatePicture();
            }
        });

    }

    private int getAngle() {
        String text = binding.angle.getText().toString();
        try {
            int value = Integer.parseInt(text);
            return value;
        } catch (NumberFormatException exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    private void updatePicture() {
        int pictureResId = pictures[pictureIndex];
        binding.picturePreview.setImageResource(pictureResId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private Bitmap loadBitmap(@DrawableRes int resId) {
        return BitmapFactory.decodeResource(getResources(), resId);
    }

    private void labellizePicture(@NonNull Bitmap bitmap) {
        int rotationDegree = getAngle();
        InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);

        // To use default options:
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        // Or, to set the minimum confidence required:
        // ImageLabelerOptions options =
        //     new ImageLabelerOptions.Builder()
        //         .setConfidenceThreshold(0.7f)
        //         .build();
        // ImageLabeler labeler = ImageLabeling.getClient(options);

        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {
                        // Task completed successfully
                        Log.i(TAG, labels.size() + " labels found in the picture");
                        for(ImageLabel imageLabel : labels) {
                            Log.i(TAG, "  " + imageLabel.getText());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                        Log.i(TAG, "Could not find labels " + e.getMessage());
                        e.printStackTrace();
                    }
                });
    }

    private void recognizePicture(@NonNull Bitmap bitmap) {
        int rotationDegree = getAngle();
        InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);

        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()  // Optional
                        .build();

        ObjectDetector objectDetector = ObjectDetection.getClient(options);

        objectDetector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<DetectedObject>>() {
                            @Override
                            public void onSuccess(List<DetectedObject> detectedObjects) {
                                // Task completed successfully
                                Log.i(TAG, detectedObjects.size() + " objects found in the picture");
                                for(DetectedObject detectedObject : detectedObjects) {
                                    Log.i(TAG, "  " + detectedObject.getLabels().size() + " labels");
                                    for(DetectedObject.Label label : detectedObject.getLabels()) {
                                        Log.i(TAG, "  " + label.getText());
                                    }
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                Log.i(TAG, "Could not find labels " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
    }

    private void textRecognition(@NonNull Bitmap bitmap) {
        int rotationDegree = getAngle();
        InputImage image = InputImage.fromBitmap(bitmap, rotationDegree);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                // Task completed successfully
                                Log.i(TAG, "TEXT: " + visionText.getText());
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.i(TAG, "Could not find texts " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                });
    }

}