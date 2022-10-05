package com.inventhys.gael.primofoodproto;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.inventhys.gael.primofoodproto.databinding.FragmentSecondBinding;

import fr.arnaudguyon.perm.Perm;
import fr.arnaudguyon.perm.PermResult;

public class SecondFragment extends Fragment {

    private static final int PERMISSIONS_REQUEST = 1;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};

    private FragmentSecondBinding binding;
    private BarCodeScanner barCodeScanner;

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

        barCodeScanner = new BarCodeScanner(getContext(), binding.previewView);

        Perm perm = new Perm(this, PERMISSIONS);
        if (!perm.areGranted()) {
            perm.askPermissions(PERMISSIONS_REQUEST);
        } else {
            barCodeScanner.startScan();
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
            barCodeScanner.startScan();
        } else {
            getActivity().finish();
        }
    }

}