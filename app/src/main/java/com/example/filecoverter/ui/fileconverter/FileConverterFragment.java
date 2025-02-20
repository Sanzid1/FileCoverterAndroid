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
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dhaval2404.imagepicker.ImagePicker;
import com.example.filecoverter.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

import java.io.File;
import java.io.IOException;

public class FileConverterFragment extends Fragment {
    private static final int PERMISSION_REQUEST_CODE = 200;
    private Button selectFileButton;
    private Button convertButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private AdView adView;
    private Uri selectedFileUri;

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
        ImagePicker.with(this)
                .galleryOnly()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start();
    }

    private void convertToPdf() {
        if (selectedFileUri == null) {
            Toast.makeText(getContext(), "Please select a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Converting...");

        try {
            // Create PDF document
            String outputPath = requireContext().getExternalFilesDir(null) + "/converted_file.pdf";
            PdfWriter writer = new PdfWriter(outputPath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Add content to PDF
            Image image = new Image(com.itextpdf.io.image.ImageDataFactory.create(selectedFileUri.getPath()));
            document.add(image);
            document.close();

            statusText.setText("Conversion completed! File saved at: " + outputPath);
        } catch (IOException e) {
            statusText.setText("Error converting file: " + e.getMessage());
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}