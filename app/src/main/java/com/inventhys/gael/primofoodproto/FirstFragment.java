package com.inventhys.gael.primofoodproto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
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

import java.io.IOException;
import java.util.List;

public class FirstFragment extends Fragment {

    private static final String TAG = "LABELLING";
    private FragmentFirstBinding binding;
    public static final int RESULT_OK = -1;
    int pictureIndex = 0;
    private int[] pictures = new int[]{
            R.drawable.triangle,
            R.drawable.carann3,
            R.drawable.carrefour199,
            R.drawable.tni,
            R.drawable.zoom,
            R.drawable.casino_20221004_121529,
            R.drawable.casino_20221004_121401,
            R.drawable.casino_20221004_121345,
    };

    private final TextBitmap textBitmap = new TextBitmap();
    private boolean displayOverlay = false;

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
                if (getActivity().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 3);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });

        updatePicture();

        binding.textrecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayOverlay = !displayOverlay;
                int pictureResId = pictures[pictureIndex];
                Bitmap bitmap = loadBitmap(pictureResId);
                updatePictureWithBoxes(bitmap);
            }
        });

        binding.barCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pictureResId = pictures[pictureIndex];
                Bitmap bitmap = loadBitmap(pictureResId);
                barCode(bitmap);
            }
        });

    }

    private void updatePicture() {
        int pictureResId = pictures[pictureIndex];
        binding.picturePreview.setImageResource(pictureResId);
        binding.pictureOverlay.setImageBitmap(null);
        displayOverlay = false;
    }

    private void updatePictureWithBoxes(Bitmap bitmap) {

//        bitmap = Bitmap.createBitmap(bitmap, 0, 0, 640, 640);
        bitmap = Bitmap.createScaledBitmap(bitmap, 320, 320, false);

        if (bitmap != null) {
            binding.picturePreview.setImageBitmap(bitmap);
//            if (displayOverlay) {
                TextBitmap.Data data = textBitmap.getOrCreate(bitmap.getWidth(), bitmap.getHeight());
                if (data.canvas != null) {
                    textBitmap.updatePictureWithBlockIA(this.getContext(), bitmap);
                    binding.pictureOverlay.setImageBitmap(data.bitmap);
                }
//            } else {
//                binding.pictureOverlay.setImageBitmap(null);
//            }
        }
    }

    private void updateWithBarcodes(List<Barcode> barcodes) {
        displayOverlay = !displayOverlay;
        int pictureResId = pictures[pictureIndex];
        Bitmap bitmap = loadBitmap(pictureResId);
        if (bitmap != null) {
            binding.picturePreview.setImageBitmap(bitmap);
            if (displayOverlay) {
                TextBitmap.Data data = textBitmap.getOrCreate(bitmap.getWidth(), bitmap.getHeight());
                if (data.canvas != null) {
                    textBitmap.updateWithBarcodes(barcodes, true, data.bitmap.getWidth(), false);
                    binding.pictureOverlay.setImageBitmap(data.bitmap);
                }
            } else {
                binding.pictureOverlay.setImageBitmap(null);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private Bitmap loadBitmap(@DrawableRes int resId) {
        return BitmapFactory.decodeResource(getResources(), resId);
    }

    private void barCode(@NonNull Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);

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
                        updateWithBarcodes(barcodes);
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 3) {
                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);

                image = Bitmap.createScaledBitmap(image, 320, 320, false);

                updatePictureWithBoxes(image);

//                TextBitmap.Data dataPhoto = textBitmap.getOrCreate(image.getWidth(), image.getHeight());
//                binding.picturePreview.setImageBitmap(image);
//                textBitmap.updatePictureWithBlockIA(this.getContext(), image);
//                binding.pictureOverlay.setImageBitmap(dataPhoto.bitmap);
            } else {
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                image = Bitmap.createScaledBitmap(image, 320, 320, false);

                updatePictureWithBoxes(image);

//                TextBitmap.Data dataGallery = textBitmap.getOrCreate(image.getWidth(), image.getHeight());
//                binding.picturePreview.setImageBitmap(image);
//                textBitmap.updatePictureWithBlockIA(this.getContext(), image);
//                binding.pictureOverlay.setImageBitmap(dataGallery.bitmap);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}