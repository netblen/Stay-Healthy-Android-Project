package com.example.stayhealthyap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WorkoutActivity extends AppCompatActivity {
    private TextView tvWorkoutCat, tvTimer, tvStepsData, tvSpeedData;
    private Button btnPauseResume;
    private MaterialButton btnEndWorkout;
    private ImageView btnBack;

    //Database
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private String userId;
    private String todayDate;
    private String workoutType; //Like "Running", "Walking", etc.

    //Timer
    private Handler timerHandler; //The timer itself
    private Runnable timerRunnable;  //The code that runs every second
    private long timeInSeconds = 0;
    private boolean isTimerRunning = false;

    //Formulas (double to be more accurate since we do not use real sensors)
    private double estimatedSteps = 0.0;
    private double totalMeters = 0.0;


    private double stepsPerSecond = 0.0;
    private double metersPerSecond = 0.0;
    private double caloriesPerMinute = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //Connect to Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userId = currentUser.getUid();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        tvWorkoutCat = findViewById(R.id.tvWorkoutCat);
        tvTimer = findViewById(R.id.tvTimer);
        tvStepsData = findViewById(R.id.tvStepsData);
        tvSpeedData = findViewById(R.id.tvSpeedData);
        btnPauseResume = findViewById(R.id.btnPauseResume);
        btnEndWorkout = findViewById(R.id.btnEndWorkout);
        btnBack = findViewById(R.id.btnBack);

        //Get the workout type (like "Running") that HomeActivity sent
        workoutType = getIntent().getStringExtra("WORKOUT_TYPE");
        if (workoutType != null) {
            tvWorkoutCat.setText(workoutType);
        }

        setFormulas();

        setupTimer();

        btnPauseResume.setOnClickListener(v -> toggleTimer()); //Start/Pause button
        btnEndWorkout.setOnClickListener(v -> endWorkout()); //End button
        btnBack.setOnClickListener(v -> finish()); // Back button
    }

    //This function sets the average numbers based on the workout type
    private void setFormulas() {
        if (workoutType == null) {
            return;
        }

        switch (workoutType) {
            case "Running":
                stepsPerSecond = 2.6; //A fast pace
                metersPerSecond = 2.7;  //Averages to ~10 km/h
                caloriesPerMinute = 12.0; //A high number
                break;
            case "Walking":
                stepsPerSecond = 1.8; //A normal walk
                metersPerSecond = 1.4;  // Averages to ~5 km/h
                caloriesPerMinute = 5.0;  // A medium number
                break;
            case "Circuit":
                stepsPerSecond = 2.0;   // A guess
                metersPerSecond = 1.0;  // A guess
                caloriesPerMinute = 8.0;  // A guess
                break;
            case "Swimming":
            default:
                stepsPerSecond = 0;    // No steps
                metersPerSecond = 0;   // We aren't tracking meters
                caloriesPerMinute = 7.0;  // A medium number
                break;
        }
    }

    // This sets up our main timer to run every 1 second
    private void setupTimer() {
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isTimerRunning) {
                    return; // Stop if paused
                }

                //1. Update the Time
                timeInSeconds++;
                long hours = timeInSeconds / 3600;
                long minutes = (timeInSeconds % 3600) / 60;
                long secs = timeInSeconds % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
                tvTimer.setText(time);

                //2. Update the Steps
                if (stepsPerSecond > 0) {
                    estimatedSteps += stepsPerSecond; // Add decimal amount
                    tvStepsData.setText(String.valueOf((long) estimatedSteps)); // Show the whole number
                }

                //3. Update the Distance (km)
                if (metersPerSecond > 0) {
                    totalMeters += metersPerSecond;
                    double totalKm = totalMeters / 1000.0;
                    tvSpeedData.setText(String.format(Locale.getDefault(), "%.2f km", totalKm));
                }

                timerHandler.postDelayed(this, 1000);
            }
        };
    }

    //"Start" / "Pause" button
    private void toggleTimer() {
        if (isTimerRunning) {
            // If it's running, pause it
            isTimerRunning = false;
            btnPauseResume.setText("Resume");
            timerHandler.removeCallbacks(timerRunnable); //Stop the timer
        } else {
            // If it's paused, start it
            isTimerRunning = true;
            btnPauseResume.setText("Pause");
            timerHandler.postDelayed(timerRunnable, 1000); //Start the timer
        }
    }

    // This function runs when we press "End"
    private void endWorkout() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);

        double elapsedMinutes = timeInSeconds / 60.0;

        if (elapsedMinutes < 0.1) {
            Toast.makeText(this, "No workout time recorded", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        double totalKm = totalMeters / 1000.0;
        double caloriesBurned = elapsedMinutes * caloriesPerMinute;

        saveWorkoutData(elapsedMinutes, timeInSeconds, (long) estimatedSteps, totalKm, caloriesBurned);
    }
    //Saves all the data to the database
    private void saveWorkoutData(double elapsedMinutes, long elapsedSeconds, long totalSteps, double totalKm, double caloriesBurned) {
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();

        DocumentReference todayDocRef = firestore.collection("users").document(userId)
                .collection("daily_activities").document(todayDate);

        todayDocRef.get().addOnSuccessListener(documentSnapshot -> {

            double existingMinutes = 0.0;
            long existingSeconds = 0;
            double existingSteps = 0.0;
            double existingCals = 0.0;
            double existingKm = 0.0;

            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("total_workout_minutes")) {
                    existingMinutes = documentSnapshot.getDouble("total_workout_minutes");
                }
                if (documentSnapshot.contains("total_workout_seconds")) {
                    existingSeconds = documentSnapshot.getLong("total_workout_seconds");
                }
                if (documentSnapshot.contains("steps")) {
                    existingSteps = documentSnapshot.getDouble("steps");
                }
                if (documentSnapshot.contains("cals_burnt")) {
                    existingCals = documentSnapshot.getDouble("cals_burnt");
                }
                if (documentSnapshot.contains("total_km")) {
                    existingKm = documentSnapshot.getDouble("total_km");
                }
            }

            double newTotalMinutes = existingMinutes + elapsedMinutes;
            long newTotalSeconds = existingSeconds + elapsedSeconds;
            double newTotalSteps = existingSteps + totalSteps;
            double newTotalCals = existingCals + caloriesBurned;
            double newTotalKm = existingKm + totalKm;

            Map<String, Object> activity = new HashMap<>();
            activity.put("timestamp", new Date());
            activity.put("last_exercise_type", workoutType);

            activity.put("total_workout_minutes", newTotalMinutes);
            activity.put("total_workout_seconds", newTotalSeconds);
            activity.put("steps", newTotalSteps);
            activity.put("cals_burnt", newTotalCals);
            activity.put("total_km", newTotalKm);

            todayDocRef.set(activity, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(WorkoutActivity.this, "Workout saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(WorkoutActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // This function runs if the app is destroyed (closed)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the timer to prevent memory leaks
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}