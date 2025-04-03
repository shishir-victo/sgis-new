package com.example.attendancesystem;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentListActivity extends AppCompatActivity {
    
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    
    private ListView studentListView;
    private Button addStudentButton;
    private ProgressBar progressBar;
    private TextView statusText;
    
    private ApiService apiService;
    private List<ClassItem> classes = new ArrayList<>();
    private List<Map<String, Object>> students = new ArrayList<>();
    
    // New student fields
    private String newStudentName;
    private String newStudentId;
    private String newStudentClassId;
    private Bitmap newStudentPhoto;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);
        
        // Initialize views
        studentListView = findViewById(R.id.student_list);
        addStudentButton = findViewById(R.id.add_student_button);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        
        // Initialize API service
        apiService = RetrofitClient.getApiService();
        
        // Set up add student button
        addStudentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddStudentDialog();
            }
        });
        
        // Set up list item click listener
        studentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showStudentDetails(position);
            }
        });
        
        // Load classes for add student dialog
        loadClasses();
        
        // Load students
        loadStudents();
    }
    
    private void loadClasses() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiService.getClasses().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
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
                    }
                }
                
                // Continue with loading students
                progressBar.setVisibility(View.GONE);
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StudentListActivity.this, "Error loading classes: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadStudents() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Loading students...");
        
        apiService.getStudents().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseData = response.body();
                    boolean success = (boolean) responseData.get("success");
                    
                    if (success) {
                        students = (ArrayList<Map<String, Object>>) responseData.get("students");
                        
                        if (students.isEmpty()) {
                            statusText.setText("No students found. Add students using the button below.");
                        } else {
                            statusText.setText("");
                            displayStudents();
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
                statusText.setText("Error loading students: " + t.getMessage());
            }
        });
    }
    
    private void displayStudents() {
        // Prepare data for the list adapter
        List<Map<String, String>> data = new ArrayList<>();
        
        for (Map<String, Object> student : students) {
            Map<String, String> item = new HashMap<>();
            String name = (String) student.get("name");
            String studentId = (String) student.get("student_id");
            String classId = (String) student.get("class_id");
            
            // Find class name
            String className = "Unknown Class";
            for (ClassItem classItem : classes) {
                if (classItem.getId().equals(classId)) {
                    className = classItem.getName();
                    break;
                }
            }
            
            item.put("name", name);
            item.put("details", "ID: " + studentId + " | Class: " + className);
            
            data.add(item);
        }
        
        // Create and set the adapter
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                android.R.layout.simple_list_item_2,
                new String[]{"name", "details"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        
        studentListView.setAdapter(adapter);
    }
    
    private void showAddStudentDialog() {
        if (classes.isEmpty()) {
            Toast.makeText(this, "Please add classes first before adding students.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create class spinner adapter
        ArrayAdapter<ClassItem> classAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                classes
        );
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // Create the dialog with a custom layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Student");
        
        View dialogView = getLayoutInflater().inflate(R.layout.student_item, null);
        EditText nameInput = dialogView.findViewById(R.id.student_name);
        EditText idInput = dialogView.findViewById(R.id.student_id);
        Spinner classSpinner = dialogView.findViewById(R.id.class_spinner);
        Button photoButton = dialogView.findViewById(R.id.photo_button);
        
        classSpinner.setAdapter(classAdapter);
        
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        
        builder.setView(dialogView);
        
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Save inputs for use after photo capture
                newStudentName = nameInput.getText().toString().trim();
                newStudentId = idInput.getText().toString().trim();
                ClassItem selectedClass = (ClassItem) classSpinner.getSelectedItem();
                
                if (newStudentName.isEmpty() || newStudentId.isEmpty() || selectedClass == null) {
                    Toast.makeText(StudentListActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                newStudentClassId = selectedClass.getId();
                
                if (newStudentPhoto == null) {
                    dispatchTakePictureIntent();
                } else {
                    addStudent();
                }
            }
        });
        
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }
    
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            newStudentPhoto = (Bitmap) extras.get("data");
            
            if (newStudentName != null && newStudentId != null && newStudentClassId != null) {
                addStudent();
            }
        }
    }
    
    private void addStudent() {
        if (newStudentPhoto == null) {
            Toast.makeText(this, "Student photo is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Adding student...");
        
        // Convert photo to base64
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        newStudentPhoto.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        String base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
        
        // Prepare request data
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("name", newStudentName);
        requestData.put("student_id", newStudentId);
        requestData.put("class_id", newStudentClassId);
        requestData.put("photo", base64Image);
        
        // Send the request
        apiService.addStudent(requestData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseData = response.body();
                    boolean success = (boolean) responseData.get("success");
                    
                    if (success) {
                        Toast.makeText(StudentListActivity.this, "Student added successfully", Toast.LENGTH_SHORT).show();
                        
                        // Clear the fields
                        newStudentName = null;
                        newStudentId = null;
                        newStudentClassId = null;
                        newStudentPhoto = null;
                        
                        // Reload students
                        loadStudents();
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
                statusText.setText("Error adding student: " + t.getMessage());
            }
        });
    }
    
    private void showStudentDetails(int position) {
        // Get the selected student
        Map<String, Object> student = students.get(position);
        String name = (String) student.get("name");
        String studentId = (String) student.get("student_id");
        String classId = (String) student.get("class_id");
        
        // Find class name
        String className = "Unknown Class";
        for (ClassItem classItem : classes) {
            if (classItem.getId().equals(classId)) {
                className = classItem.getName();
                break;
            }
        }
        
        // Build the details message
        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(name).append("\n");
        details.append("Student ID: ").append(studentId).append("\n");
        details.append("Class: ").append(className).append("\n");
        
        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Student Details")
                .setMessage(details.toString())
                .setPositiveButton("View Attendance", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: Implement viewing individual student attendance
                        Toast.makeText(StudentListActivity.this, "Feature coming soon", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        confirmDeleteStudent(studentId);
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
    
    private void confirmDeleteStudent(String studentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this student? This action cannot be undone.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteStudent(studentId);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
    
    private void deleteStudent(String studentId) {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Deleting student...");
        
        apiService.deleteStudent(studentId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseData = response.body();
                    boolean success = (boolean) responseData.get("success");
                    
                    if (success) {
                        Toast.makeText(StudentListActivity.this, "Student deleted successfully", Toast.LENGTH_SHORT).show();
                        
                        // Reload students
                        loadStudents();
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
                statusText.setText("Error deleting student: " + t.getMessage());
            }
        });
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
