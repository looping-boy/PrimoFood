package com.inventhys.gael.primofoodproto;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
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

import java.util.ArrayList;
import java.util.List;

import fr.arnaudguyon.perm.Perm;
import fr.arnaudguyon.perm.PermResult;

public class SecondFragment extends Fragment implements CameraScanner.CameraScannerListener {

    private static final String TAG = "SecondFragment";
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};

    private FragmentSecondBinding binding;
    private CameraScanner cameraScanner;
//    private List<Rect> boxes = new ArrayList<>();
    private Paint paint = new Paint();
    private Bitmap bitmapRect;
    private Rect tmpRect = new Rect();

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

    private void textRecognition(@NonNull ImageProxy imageProxy, @NonNull InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {

                                if (!isAdded()) {
                                    return;
                                }

                                // Task completed successfully
                                String text = visionText.getText();
                                if (!TextUtils.isEmpty(text)) {
                                    Log.i(TAG, "TEXT: " + visionText.getText());
//                                    boxes.clear();
                                    List<Text.TextBlock> blocks = visionText.getTextBlocks();
//                                    for (Text.TextBlock block : blocks) {
//                                        Rect rect = block.getBoundingBox();
//                                        boxes.add(rect);
//                                    }
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
                                        imageProxy.close();
                                    }
                                });
    }


    @Override
    public void onImageCaptured(@NonNull ImageProxy imageProxy, @NonNull InputImage inputImage) {
        textRecognition(imageProxy, inputImage);
    }

    private void updatePictureWithBoxes(List<Text.TextBlock> blocks, @NonNull ImageProxy imageProxy) {
        if ((bitmapRect == null) || (bitmapRect.isRecycled()) || (bitmapRect.getWidth() != imageProxy.getWidth()) || (bitmapRect.getHeight() != imageProxy.getHeight())) {
            bitmapRect = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
        }
        if (bitmapRect != null) {
            Canvas canvas = new Canvas(bitmapRect);
            canvas.drawColor(0xFFFFFFFF, PorterDuff.Mode.CLEAR);
            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFFFFFF00);
            canvas.drawRect(1,1, bitmapRect.getWidth() - 2, bitmapRect.getHeight() - 2, paint);

            for (Text.TextBlock block : blocks) {
                Rect rect = block.getBoundingBox();

                paint.setColor(0x77000000); // Darker
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(rect, paint);

                paint.setColor(0xFFFF0000); // Red Border
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(rect, paint);

                paint.setStyle(Paint.Style.FILL);   // White texts
                paint.setColor(0xFFFFFFFF);
                List<Text.Line> lines = block.getLines();
                for(Text.Line line : lines) {
                    String text = line.getText();
                    if (!TextUtils.isEmpty(text)) {
                        rect = line.getBoundingBox();
                        paint.setTextSize(rect.bottom - rect.top);
                        paint.getTextBounds(text, 0, text.length(), tmpRect);
                        float x = rect.left - tmpRect.left + ((rect.right - rect.left - tmpRect.width()) / 2);
                        float y = rect.top - tmpRect.top + ((rect.bottom - rect.top - tmpRect.height()) / 2);
                        canvas.drawText(text, x, y, paint);
                    }
                }
            }
            binding.bitmapView.setImageBitmap(bitmapRect);
        }
    }
}