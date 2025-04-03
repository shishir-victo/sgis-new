package com.example.attendancesystem;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceActivity extends AppCompatActivity {
    
    private Spinner classSpinner;
    private Button dateButton;
    private ListView attendanceListView;
    private ProgressBar progressBar;
    private TextView statusText;
    
    private ApiService apiService;
    private List<ClassItem> classes = new ArrayList<>();
    private String selectedDate;
    private Calendar calendar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        
        // Initialize views
        classSpinner = findViewById(R.id.class_spinner);
        dateButton = findViewById(R.id.date_button);
        attendanceListView = findViewById(R.id.attendance_list);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        
        // Initialize API service
        apiService = RetrofitClient.getApiService();
        
        // Initialize calendar with current date
        calendar = Calendar.getInstance();
        updateDateButton();
        
        // Set up date button click listener
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
        
        // Load classes for spinner
        loadClasses();
        
        // Set up class spinner selection listener
        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadAttendanceReport();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void updateDateButton() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = dateFormat.format(calendar.getTime());
        dateButton.setText(selectedDate);
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateButton();
                        loadAttendanceReport();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
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
                            // Set up spinner adapter
                            ArrayAdapter<ClassItem> adapter = new ArrayAdapter<>(
                                    AttendanceActivity.this,
                                    android.R.layout.simple_spinner_item,
                                    classes
                            );
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            classSpinner.setAdapter(adapter);
                            
                            // Load attendance report for the first class
                            loadAttendanceReport();
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
    
    private void loadAttendanceReport() {
        if (classes.isEmpty()) {
            statusText.setText("No classes available.");
            return;
        }
        
        ClassItem selectedClass = (ClassItem) classSpinner.getSelectedItem();
        if (selectedClass == null) {
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Loading attendance report...");
        
        // Clear current list
        attendanceListView.setAdapter(null);
        
        apiService.getAttendanceReport(selectedClass.getId(), selectedDate).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseData = response.body();
                    boolean success = (boolean) responseData.get("success");
                    
                    if (success) {
                        ArrayList<Map<String, Object>> records = (ArrayList<Map<String, Object>>) responseData.get("attendance_records");
                        
                        if (records.isEmpty()) {
                            statusText.setText("No attendance records found for this date.");
                        } else {
                            statusText.setText("");
                            displayAttendanceRecords(records);
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
                statusText.setText("Error loading attendance: " + t.getMessage());
            }
        });
    }
    
    private void displayAttendanceRecords(ArrayList<Map<String, Object>> records) {
        // Prepare data for the list adapter
        List<Map<String, String>> data = new ArrayList<>();
        
        int presentCount = 0;
        int absentCount = 0;
        
        for (Map<String, Object> record : records) {
            Map<String, String> item = new HashMap<>();
            String name = (String) record.get("student_name");
            String studentId = (String) record.get("student_id");
            boolean status = (boolean) record.get("status");
            
            item.put("name", name);
            item.put("id", "ID: " + studentId);
            item.put("status", status ? "✅ Present" : "❌ Absent");
            
            if (status) {
                presentCount++;
            } else {
                absentCount++;
            }
            
            data.add(item);
        }
        
        // Update status text with summary
        ClassItem selectedClass = (ClassItem) classSpinner.getSelectedItem();
        String summary = String.format("Class: %s | Date: %s | Present: %d | Absent: %d",
                selectedClass.getName(), selectedDate, presentCount, absentCount);
        statusText.setText(summary);
        
        // Create and set the adapter
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                android.R.layout.simple_list_item_2,
                new String[]{"name", "status"},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        
        attendanceListView.setAdapter(adapter);
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
