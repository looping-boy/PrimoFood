package com.inventhys.gael.primofoodproto;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.inventhys.gael.primofoodproto.databinding.FragmentSecondBinding;

import java.util.List;

import fr.arnaudguyon.perm.Perm;
import fr.arnaudguyon.perm.PermResult;

public class SecondFragment extends Fragment implements CameraScanner.CameraScannerListener {

    private static final String TAG = "SecondFragment";
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};

    private FragmentSecondBinding binding;
    private CameraScanner cameraScanner;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });

        cameraScanner = new CameraScanner(getContext(), binding.previewView);

        Perm perm = new Perm(this, PERMISSIONS);
        if (!perm.areGranted()) {
            perm.askPermissions(PERMISSIONS_REQUEST);
        } else {
            cameraScanner.startScan(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        PermResult permResult = new PermResult(permissions, grantResults);
        if (permResult.areGranted()) {
            cameraScanner.startScan(this);
        } else {
            getActivity().finish();
        }
    }

    private void textRecognition(@NonNull ImageProxy imageProxy, @NonNull InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                // Task completed successfully
                                Log.i(TAG, "TEXT: " + visionText.getText());
//                                boxes.clear();
//                                List<Text.TextBlock> blocks = visionText.getTextBlocks();
//                                for (Text.TextBlock block : blocks) {
//                                    Rect rect = block.getBoundingBox();
//                                    boxes.add(rect);
//                                }
//                                updatePictureWithBoxes();
                                imageProxy.close();
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        Log.i(TAG, "Could not find texts " + e.getMessage());
                                        e.printStackTrace();
                                        imageProxy.close();
                                    }
                                });
    }


    @Override
    public void onImageCaptured(@NonNull ImageProxy imageProxy, @NonNull InputImage inputImage) {
        textRecognition(imageProxy, inputImage);
    }
}