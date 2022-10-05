package com.inventhys.gael.primofoodproto;

import android.content.Context;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

public class BarCodeScanner {

    private static final String TAG = "BarCodeScanner";
    private final @NonNull WeakReference<Context> contextRef;
    private final @NonNull PreviewView previewView;
    private ImageCapture imageCapture;
    private final ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private boolean started = false;
    //private OnBarCodesFoundListener listener;

    public BarCodeScanner(@NonNull Context context, @NonNull PreviewView previewView) {
        this.contextRef = new WeakReference<>(context);
        this.previewView = previewView;
        this.cameraProviderFuture = ProcessCameraProvider.getInstance(context);
    }

    public void startScan(/*@NonNull OnBarCodesFoundListener listener*/) {
        Context context = contextRef.get();
        if (!started) {
            started = true;
            //this.listener = listener;
            cameraProviderFuture.addListener(captureRunnable, ContextCompat.getMainExecutor(context));
        }
    }

    public void stopScan() {
        if (started) {
            started = false;
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            Context context = contextRef.get();
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();

                // Set up the capture use case to allow users to take photos
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        //.setMaxResolution(new Size(1024, 1024))
                        .build();

                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Use Case Image Analysis
                ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
                ImageAnalysis analysisUseCase = builder.build();

                ImageAnalysis.Analyzer imageAnalyzer = new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        analyzeImage(image);
                    }
                };

                analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(context), imageAnalyzer);

                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) context),
                        cameraSelector,
                        preview,
                        imageCapture, analysisUseCase);

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get()
                // shouldn't block since the listener is being called, so no need to
                // handle InterruptedException.
            }
        }
    };

    private void analyzeImage(@NonNull ImageProxy image) {
        // TODO: barcode + text recognition
        Log.i(TAG, "analyzeImage");
        image.close();
    }


//    private void barCode(final @NonNull ImageProxy imageProxy) { //final InputImage inputImage) {
//        Image mediaImage = imageProxy.getImage();
//        if (mediaImage != null) {
//            final InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
//
//            //options = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
//            BarcodeScanner scanner = BarcodeScanning.getClient();
//            scanner.process(inputImage)
//                    .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
//                        @Override
//                        public void onComplete(@NonNull Task<List<Barcode>> task) {
//                            mediaImage.close();
//                            imageProxy.close();
//                            if (task.isSuccessful()) {
//                                onBarCodeRead(task.getResult());
//                            }
//                        }
//                    });
//        } else {
//            imageProxy.close();
//        }
//    }
//
//    private void onBarCodeRead(List<Barcode> barcodes) {
//        if ((barcodes != null) && !barcodes.isEmpty()) {
//            if (listener != null) {
//                listener.onBarCodesFound(BarCodeScanner.this, barcodes);
//            }
//        }
//    }
//
//    public interface OnBarCodesFoundListener {
//        void onBarCodesFound(@NonNull BarCodeScanner barCodeScanner, @NonNull List<Barcode> barcodes);
//    }

//    private void takePicture() {
//        if (imageCapture != null) {
//            imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
//                @Override
//                public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
//                    barCode(imageProxy);
//                }
//            });
//        }
//    }

}

