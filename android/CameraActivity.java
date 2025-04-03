package com.example.attendancesystem;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraActivity extends AppCompatActivity {
    
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private Spinner classSpinner;
    private Button captureButton;
    private ProgressBar progressBar;
    private TextView statusText;
    
    private ApiService apiService;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private List<ClassItem> classes = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        // Initialize views
        previewView = findViewById(R.id.preview_view);
        classSpinner = findViewById(R.id.class_spinner);
        captureButton = findViewById(R.id.capture_button);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        
        // Initialize API service
        apiService = RetrofitClient.getApiService();
        
        // Set up camera
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        
        // Load classes for spinner
        loadClasses();
        
        // Set up capture button click listener
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (classes.isEmpty()) {
                    Toast.makeText(CameraActivity.this, "No classes available. Please add classes first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                capturePhoto();
            }
        });
    }
    
    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    
                    // Set up the preview use case
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                    
                    // Set up the capture use case
                    imageCapture = new ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build();
                    
                    // Choose the back camera
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();
                    
                    // Unbind any bound use cases before rebinding
                    cameraProvider.unbindAll();
                    
                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(CameraActivity.this, cameraSelector, preview, imageCapture);
                    
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Error starting camera: " + e.getMessage());
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void capturePhoto() {
        if (imageCapture == null) {
            return;
        }
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Taking photo...");
        captureButton.setEnabled(false);
        
        // Create the image capture listener
        imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("Processing image...");
                    }
                });
                
                // Convert the image to bitmap
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                
                // Convert bitmap to base64
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                String base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
                
                // Close the image
                image.close();
                
                // Get selected class
                ClassItem selectedClass = (ClassItem) classSpinner.getSelectedItem();
                
                if (selectedClass == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            captureButton.setEnabled(true);
                            statusText.setText("Error: No class selected.");
                        }
                    });
                    return;
                }
                
                // Prepare request data
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("class_id", selectedClass.getId());
                requestData.put("date", currentDate);
                requestData.put("photo", base64Image);
                
                // Send image to server for recognition
                apiService.takeAttendance(requestData).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        progressBar.setVisibility(View.GONE);
                        captureButton.setEnabled(true);
                        
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, Object> responseData = response.body();
                            boolean success = (boolean) responseData.get("success");
                            
                            if (success) {
                                // Get the attendance results
                                ArrayList<Map<String, Object>> recognized = (ArrayList<Map<String, Object>>) responseData.get("recognized_students");
                                ArrayList<Map<String, Object>> unrecognized = (ArrayList<Map<String, Object>>) responseData.get("unrecognized_faces");
                                
                                // Show the summary
                                showAttendanceSummary(recognized, unrecognized);
                            } else {
                                String error = (String) responseData.get("error");
                                statusText.setText("Error: " + error);
                            }
                        } else {
                            statusText.setText("Error: " + response.message());
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        captureButton.setEnabled(true);
                        statusText.setText("Error: " + t.getMessage());
                    }
                });
            }
            
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        captureButton.setEnabled(true);
                        statusText.setText("Error taking photo: " + exception.getMessage());
                    }
                });
            }
        });
    }
    
    private void loadClasses() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Loading classes...");
        
        apiService.getClasses().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseData = response.body();
                    boolean success = (boolean) responseData.get("success");
                    
                    if (success) {
                        classes.clear();
                        ArrayList<Map<String, Object>> classesData = (ArrayList<Map<String, Object>>) responseData.get("classes");
                        
                        for (Map<String, Object> classData : classesData) {
                            String id = (String) classData.get("id");
                            String name = (String) classData.get("name");
                            classes.add(new ClassItem(id, name));
                        }
                        
                        if (classes.isEmpty()) {
                            statusText.setText("No classes found. Please add classes first.");
                        } else {
                            statusText.setText("Ready to take attendance.");
                            
                            // Set up spinner adapter
                            ArrayAdapter<ClassItem> adapter = new ArrayAdapter<>(
                                    CameraActivity.this,
                                    android.R.layout.simple_spinner_item,
                                    classes
                            );
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            classSpinner.setAdapter(adapter);
                        }
                    } else {
                        String error = (String) responseData.get("error");
                        statusText.setText("Error: " + error);
                    }
                } else {
                    statusText.setText("Error: " + response.message());
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                statusText.setText("Error loading classes: " + t.getMessage());
            }
        });
    }
    
    private void showAttendanceSummary(ArrayList<Map<String, Object>> recognized, ArrayList<Map<String, Object>> unrecognized) {
        StringBuilder message = new StringBuilder();
        
        message.append("Attendance Summary:\n\n");
        message.append("✅ Recognized Students: ").append(recognized.size()).append("\n");
        for (Map<String, Object> student : recognized) {
            message.append("  - ").append(student.get("name"))
                   .append(" (Confidence: ").append(String.format("%.2f", (double) student.get("confidence")))
                   .append(")\n");
        }
        
        message.append("\n❓ Unrecognized Faces: ").append(unrecognized.size()).append("\n");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attendance Complete")
                .setMessage(message.toString())
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();  // Return to main activity
                    }
                })
                .setNegativeButton("Take Another", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        statusText.setText("Ready to take attendance.");
                    }
                })
                .setCancelable(false)
                .show();
    }
    
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    // Class item for spinner
    private static class ClassItem {
        private String id;
        private String name;
        
        public ClassItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}
