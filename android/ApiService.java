package com.example.attendancesystem;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    
    @GET("api/students")
    Call<Map<String, Object>> getStudents();
    
    @POST("api/students")
    Call<Map<String, Object>> addStudent(@Body Map<String, Object> student);
    
    @DELETE("api/students/{student_id}")
    Call<Map<String, Object>> deleteStudent(@Path("student_id") String studentId);
    
    @GET("api/classes")
    Call<Map<String, Object>> getClasses();
    
    @POST("api/classes")
    Call<Map<String, Object>> addClass(@Body Map<String, Object> classData);
    
    @POST("api/take_attendance")
    Call<Map<String, Object>> takeAttendance(@Body Map<String, Object> attendanceData);
    
    @POST("api/manual_attendance")
    Call<Map<String, Object>> manualAttendance(@Body Map<String, Object> attendanceData);
    
    @GET("api/attendance_report")
    Call<Map<String, Object>> getAttendanceReport(
            @Query("class_id") String classId,
            @Query("date") String date
    );
    
    @GET("api/student_attendance_report")
    Call<Map<String, Object>> getStudentAttendanceReport(
            @Query("student_id") String studentId
    );
}
