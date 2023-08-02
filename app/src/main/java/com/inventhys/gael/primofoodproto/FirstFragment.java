package com.inventhys.gael.primofoodproto;

import static com.inventhys.gael.primofoodproto.MainActivity.screenMode;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.inventhys.gael.primofoodproto.databinding.FragmentFirstBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment {

    private static final String TAG = "LABELLING";
    private FragmentFirstBinding binding;
    public static final int RESULT_OK = -1;
    int pictureIndex = 0;
    int pictureResId = R.drawable.carrefour199;
    Uri imageUri;
    int dimensionFromCamera;
    private final DigitProcess digitProcess = new DigitProcess();
    private final PriceTagProcess priceTagProcess = new PriceTagProcess();

    // SCREEN MODE IS BECAUSE IT APPEARS THAT THE THUMBNAIL (LOW RESOLUTION) WORKS BETTER
    // WHEN YOU CAPTURE A SCREEN :


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (screenMode) binding.knobSeekBar.setProgress(0);
        else binding.knobSeekBar.setProgress(1);

        binding.knobSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i == 0) {
                    screenMode = true;
                } else {
                    screenMode = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ((MainActivity) getActivity()).setTitle("hello");

        // Get the ActionBar or Toolbar reference
        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        // Set the title for the ActionBar/Toolbar
        if (actionBar != null) {
            actionBar.setTitle("Your Title"); // Replace "Your Title" with the desired title
        }

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    if (screenMode) {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, 3);
                    } else {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.TITLE, "New Picture");
                        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                        imageUri = getContext().getContentResolver().insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        startActivityForResult(intent, 3);
                    }
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });


        binding.picturePreview.setImageResource(pictureResId);
        binding.pictureOverlay.setImageBitmap(null);

//        binding.textrecognition.setOnClickListener(v -> {
//            Bitmap bitmap = loadBitmap(pictureResId);
//            updatePictureWithBoxes(bitmap);
//        });

//        binding.barCode.setOnClickListener(v -> {
//            Bitmap bitmap = loadBitmap(pictureResId);
//            barCode(bitmap);
//        });

        // ----- FOR TEST -------------
        Bitmap bitmap = loadBitmap(R.drawable.script_figures);
        updatePictureWithBoxes(bitmap);

    }

    private void updatePictureWithBoxes(Bitmap bitmap) {

//        binding.picturePreview.setImageBitmap(bitmap);

        Bitmap bitmapCopy = bitmap.copy(bitmap.getConfig(), true);
        binding.picturePreview.setImageBitmap(bitmapCopy);

        bitmap = Bitmap.createScaledBitmap(bitmap, 320, 320, false);

        DrawingProcess drawingProcess = new DrawingProcess();
//            ArrayList<RectInformation> digitBoxesBefore = digitProcess.getBoxesWithAIBefore(this.getContext(), bitmap);
        ArrayList<RectInformation> digitBoxes = digitProcess.getBoxesWithAI(this.getContext(), bitmap);
        ArrayList<RectInformation> priceTagBoxes = priceTagProcess.getBoxesWithAI(this.getContext(), bitmap);


        bitmap.eraseColor(Color.TRANSPARENT);
//            BitmapCanvas canvasWithDigitsBefore = drawingProcess.drawBoxes(bitmap, digitBoxesBefore, 0xFF00FFFF, 4);
        BitmapCanvas canvasWithDigits = drawingProcess.drawBoxes(bitmap, digitBoxes, 0xFFFF0000, 2);
        BitmapCanvas canvasWithDigitAndPriceTag = drawingProcess.drawBoxes(bitmap, priceTagBoxes, 0xFFFFFF00, 2);

        binding.pictureOverlay.setImageBitmap(canvasWithDigitAndPriceTag.bitmap);
        String price = digitProcess.giveThePriceString();
        binding.result.setText(price);

    }

    private void updateWithBarcodes(List<Barcode> barcodes) {
        Bitmap bitmap = loadBitmap(pictureResId);
        if (bitmap != null) {
            binding.picturePreview.setImageBitmap(bitmap);
//            BitmapCanvas bitmapCanvas = digitProcess.updateWithBarcodes(barcodes, true, bitmap.getWidth(), false, bitmap);
//            binding.pictureOverlay.setImageBitmap(bitmapCanvas.bitmap);
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
                        for (Barcode barcode : barcodes) {
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
                try {
                    if (screenMode) {
                        // OLD CODE :
                        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                        int dimension = Math.min(thumbnail.getWidth(), thumbnail.getHeight());
                        thumbnail = ThumbnailUtils.extractThumbnail(thumbnail, dimension, dimension);
                        updatePictureWithBoxes(thumbnail);
                    } else {
                        // GET BITMAP :
                        Bitmap thumbnail = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);

                        // ROTATE BITMAP :
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        Bitmap rotatedBitmap = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true);

                        // GET THE MIDDLE OF IT :
                        Bitmap squareBitmap = getMiddleOfBitmap(rotatedBitmap);

                        // SEND TO UPDATE
                        updatePictureWithBoxes(squareBitmap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                updatePictureWithBoxes(image);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Bitmap getMiddleOfBitmap(Bitmap bitmap) {
        Bitmap dstBmp;

        if (bitmap.getWidth() >= bitmap.getHeight()) {
            dimensionFromCamera = bitmap.getHeight();
            dstBmp = Bitmap.createBitmap(
                    bitmap,
                    bitmap.getWidth() / 2 - bitmap.getHeight() / 2,
                    0,
                    bitmap.getHeight(),
                    bitmap.getHeight()
            );

        } else {
            dimensionFromCamera = bitmap.getWidth();
            dstBmp = Bitmap.createBitmap(
                    bitmap,
                    0,
                    bitmap.getHeight() / 2 - bitmap.getWidth() / 2,
                    bitmap.getWidth(),
                    bitmap.getWidth()
            );
        }

        return dstBmp;
    }

}