package com.inventhys.gael.primofoodproto;

import android.Manifest;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
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
    private final DigitProcess textBitmap = new DigitProcess();

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

        cameraScanner = new CameraScanner(getContext(), binding.previewView);

        Perm perm = new Perm(this, PERMISSIONS);
        if (!perm.areGranted()) {
            perm.askPermissions(PERMISSIONS_REQUEST);
        } else {
            cameraScanner.startScan(this);
        }

        //binding.previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
    }

    @Override
    public void onDestroyView() {
        cameraScanner.stopScan();
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

    private void textRecognition(List<Barcode> barcodes, @NonNull ImageProxy imageProxy, @NonNull InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {

                                if (!isAdded()) {
                                    return;
                                }
                                drawFrame(imageProxy);
                                updateWithBarcodes(barcodes, imageProxy);
                                // Task completed successfully
                                String text = visionText.getText();
                                if (!TextUtils.isEmpty(text)) {
                                    Log.i(TAG, "TEXT: " + visionText.getText());
                                    List<Text.TextBlock> blocks = visionText.getTextBlocks();
                                    updatePictureWithBoxes(blocks, imageProxy);
                                }
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
                                        drawFrame(imageProxy);
                                        updateWithBarcodes(barcodes, imageProxy);
                                        imageProxy.close();
                                    }
                                });
    }

    private void barcodeRecognition(@NonNull ImageProxy imageProxy, @NonNull InputImage image) {
//        BarcodeScannerOptions options =
//                new BarcodeScannerOptions.Builder()
//                        .setBarcodeFormats(
//                                Barcode.FORMAT_EAN_13,
//                                Barcode.FORMAT_EAN_8)
//                        .build();
        //BarcodeScanner scanner = BarcodeScanning.getClient(options);

        BarcodeScanner scanner = BarcodeScanning.getClient();

        Task<List<Barcode>> result = scanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        // Task completed successfully
                        Log.i(TAG, "Found " + barcodes.size() + " barcodes");
                        for(Barcode barcode: barcodes) {
                            Log.i(TAG, "Barcode: " + barcode.getDisplayValue());
                        }
                        textRecognition(barcodes, imageProxy, image);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        Log.i(TAG, "Could not find bar codes " + e.getMessage());
                        e.printStackTrace();
                    }
                });
    }


    @Override
    public void onImageCaptured(@NonNull ImageProxy imageProxy, @NonNull InputImage inputImage) {
        barcodeRecognition(imageProxy, inputImage);
    }

    private void drawFrame(@NonNull ImageProxy imageProxy) {
//        TextBitmap.Data data = textBitmap.getOrCreate(imageProxy);
//        if (data.canvas != null) {
//            textBitmap.updateWithFrame(true);
//            //binding.bitmapView.setImageBitmap(data.bitmap);
//        }
    }

    private void updatePictureWithBoxes(List<Text.TextBlock> blocks, @NonNull ImageProxy imageProxy) {
//        TextBitmap.Data data = textBitmap.getOrCreate(imageProxy);
//        if (data.canvas != null) {
////            textBitmap.updateWithBlocks(blocks, false, true);
//            binding.bitmapView.setImageBitmap(data.bitmap);
//        }
    }

    private void updateWithBarcodes(List<Barcode> barcodes, @NonNull ImageProxy imageProxy) {
//        TextBitmap.Data data = textBitmap.getOrCreate(imageProxy);
//        if (data.canvas != null) {
//            textBitmap.updateWithBarcodes(barcodes, false, data.bitmap.getWidth(), true);
//            //binding.bitmapView.setImageBitmap(data.bitmap);
//        }

    }

}