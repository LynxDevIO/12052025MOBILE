package com.example.manipulararquivos;
import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private Fragments adaptador;
    private ViewPager2 viewPager;
    private ImageView imageView;
    private String currentPhotoPath;
    private Bitmap currentBitmap;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adaptador = new Fragments(this);
        viewPager = findViewById(R.id.fragment1);

        imageView = findViewById(R.id.imageView);
        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnLoad = findViewById(R.id.btnLoad);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnShare = findViewById(R.id.btnShare);

        setupViewPager(viewPager);

        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show();
            }
        });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (result && currentPhotoPath != null) {
                File imgFile = new File(currentPhotoPath);
                if (imgFile.exists()) {
                    currentBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    imageView.setImageBitmap(currentBitmap);
                }
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    imageView.setImageBitmap(currentBitmap);
                } catch (IOException e) {
                    Toast.makeText(this, "Erro ao carregar imagem!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCamera.setOnClickListener(v -> checkCameraPermission());

        btnLoad.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnSave.setOnClickListener(v -> saveImage());

        btnShare.setOnClickListener(v -> shareImage());
    }

    protected void setupViewPager(ViewPager2 viewPager) {
        Fragments adaptador = new Fragments(this);
        adaptador.addFragment(new Fragment1(), "Envio de Texto");
        viewPager.setAdapter(adaptador);
    }

    public void setViewPager(int num) {
        viewPager.setCurrentItem(num);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void dispatchTakePictureIntent() {
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this, "com.example.photoapp.fileprovider", photoFile);
            takePictureLauncher.launch(photoUri);
        } catch (IOException ex) {
            Toast.makeText(this, "Erro ao criar arquivo!", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void saveImage() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Nenhuma imagem para salvar!", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "PhotoApp_" + timeStamp + ".jpg";

        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        try {
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try (OutputStream out = resolver.openOutputStream(imageUri)) {
                    currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                }
                Toast.makeText(this, "Imagem salva em Fotos@", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Erro ao salvar imagem!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao salvar imagem: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareImage() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Nenhuma imagem para compartilhar!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = createImageFile();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
            }
            Uri imageUri = FileProvider.getUriForFile(this, "com.example.photoapp.fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Compartilhe imagem"));
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao compartilhar imagem!", Toast.LENGTH_SHORT).show();
        }
    }
}