package com.example.attendancesystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private Button btnTakeAttendance;
    private Button btnViewStudents;
    private Button btnViewAttendanceReports;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize views
        btnTakeAttendance = findViewById(R.id.btn_take_attendance);
        btnViewStudents = findViewById(R.id.btn_view_students);
        btnViewAttendanceReports = findViewById(R.id.btn_view_attendance);
        
        // Set click listeners
        btnTakeAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermissionAndOpenCamera();
            }
        });
        
        btnViewStudents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StudentListActivity.class));
            }
        });
        
        btnViewAttendanceReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AttendanceActivity.class));
            }
        });
    }
    
    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            // Permission already granted, open camera
            openCamera();
        }
    }
    
    private void openCamera() {
        startActivity(new Intent(MainActivity.this, CameraActivity.class));
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open camera
                openCamera();
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Camera permission is required to take attendance", Toast.LENGTH_LONG).show();
            }
        }
    }
}
