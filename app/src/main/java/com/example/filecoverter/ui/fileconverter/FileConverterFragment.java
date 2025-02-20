package com.example.filecoverter.ui.fileconverter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.filecoverter.R;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FileConverterFragment extends Fragment {
    private Button selectFileButton;
    private Button convertButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private AdView adView;
    private Uri selectedFileUri;
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Toast.makeText(getContext(), R.string.permission_granted, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
                        }
                    });

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedFileUri = uri;
                            statusText.setText(getString(R.string.file_selected, uri.getLastPathSegment()));
                            convertButton.setEnabled(true);
                        }
                    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_file_converter, container, false);
        
        initializeViews(root);
        setupAds();
        setupClickListeners();
        checkPermissions();
        
        return root;
    }

    private void initializeViews(View view) {
        selectFileButton = view.findViewById(R.id.button_select_file);
        convertButton = view.findViewById(R.id.button_convert);
        progressBar = view.findViewById(R.id.progress_bar);
        statusText = view.findViewById(R.id.text_status);
        adView = view.findViewById(R.id.ad_view);
        
        convertButton.setEnabled(false);
    }

    private void setupAds() {
        MobileAds.initialize(requireContext(), initializationStatus -> {});
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void setupClickListeners() {
        selectFileButton.setOnClickListener(v -> selectFile());
        convertButton.setOnClickListener(v -> convertToPdf());
    }

    private void selectFile() {
        pickImage.launch("image/*");
    }

    private void convertToPdf() {
        if (selectedFileUri == null) {
            Toast.makeText(getContext(), R.string.please_select_file, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        statusText.setText(R.string.converting);

        new Thread(() -> {
            try {
                // Create PDF document
                String outputPath = new File(requireContext().getExternalFilesDir(null), "converted_file.pdf").getAbsolutePath();
                PdfWriter writer = new PdfWriter(outputPath);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                // Get input stream from Uri
                InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedFileUri);
                if (inputStream == null) throw new IOException(getString(R.string.error_open_file));

                // Create image and add to PDF using backward-compatible approach
                byte[] imageBytes = readAllBytesCompat(inputStream);
                ImageData imageData = ImageDataFactory.create(imageBytes);
                Image image = new Image(imageData);
                document.add(image);
                document.close();

                requireActivity().runOnUiThread(() -> {
                    statusText.setText(getString(R.string.conversion_complete, outputPath));
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    statusText.setText(getString(R.string.error_converting, e.getMessage()));
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private byte[] readAllBytesCompat(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}