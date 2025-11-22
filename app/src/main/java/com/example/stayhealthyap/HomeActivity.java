package com.example.stayhealthyap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private EditText edSleepData, eWeightData;
    private Button btnEnterSleep, btnEnterWeight;
    private TextView tvWeight;
    private ImageButton imgBtnRunning, imgButtonSwimming, imgBtnWalking, imgBtnCircuit;

    private TextView tvStepsData, tvTimeMoved, tvCalsBurnt;
    private TextView tvTimeWorkedOutThisWeek;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private String userId;
    private String todayDate;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            // No user logged in
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        //edSleepData = findViewById(R.id.edSleepData);
        eWeightData = findViewById(R.id.eWeightData);
        btnEnterSleep = findViewById(R.id.btnEnterSleep); //change thias change this to open the sleep actoivity
        btnEnterWeight = findViewById(R.id.btnEnterWeight);
        tvWeight = findViewById(R.id.tvWeight);
        imgBtnRunning = findViewById(R.id.imgBtnRunning);
        imgButtonSwimming = findViewById(R.id.imgButtonSwimming);
        imgBtnWalking = findViewById(R.id.imgBtnWalking);
        imgBtnCircuit = findViewById(R.id.imgBtnCircuit);

        tvStepsData = findViewById(R.id.tvStepsData);
        tvTimeMoved = findViewById(R.id.tvTimeMoved);
        tvCalsBurnt = findViewById(R.id.textView4);
        tvTimeWorkedOutThisWeek = findViewById(R.id.tvTimeWorkedOutThisWeek);

        //btnEnterSleep.setOnClickListener(v -> saveSleepData()); //nopt gonna have any data to push to the other page now
        btnEnterSleep.setOnClickListener(v -> startActivitySleep());
        btnEnterWeight.setOnClickListener(v -> saveWeightData());
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        imgBtnRunning.setOnClickListener(v -> startWorkout("Running"));
        imgButtonSwimming.setOnClickListener(v -> startWorkout("Swimming"));
        imgBtnWalking.setOnClickListener(v -> startWorkout("Walking"));
        imgBtnCircuit.setOnClickListener(v -> startWorkout("Circuit"));

        setupBottomNavigation();
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadPageData();

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    // new one ima try and dop
    private void startActivitySleep() {
        Intent intent = new Intent(HomeActivity.this, SleepActivity.class);
        startActivity(intent);
    }

    //starts the WorkoutActivity page
    private void startWorkout(String workoutType) {
        Intent intent = new Intent(HomeActivity.this, WorkoutActivity.class);
        intent.putExtra("WORKOUT_TYPE", workoutType);
        startActivity(intent);
    }

    private void loadPageData() {
        // Step 1: Get the main profile document
        DocumentReference profileDocRef = firestore.collection("users").document(userId);

        profileDocRef.get().addOnSuccessListener(profileSnapshot -> {
            if (profileSnapshot.exists() && profileSnapshot.contains("weight")) {
                // We found the main profile weight, so set it as the default
                double profileWeight = profileSnapshot.getDouble("weight");
                tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", profileWeight));
            } else {
                // User has no weight set in their profile yet
                tvWeight.setText("0.0 kg");
            }

            // Step 2: NOW, get the daily activity document
            // This will overwrite the profile weight if a daily weight exists
            DocumentReference dailyDocRef = firestore.collection("users").document(userId)
                    .collection("daily_activities").document(todayDate);

            dailyDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Log.d("HomeActivity", "Daily data found for " + todayDate);
                    // Fill in Sleep data
                    if (documentSnapshot.contains("sleep")) {
                        double sleep = documentSnapshot.getDouble("sleep");
                        edSleepData.setText(String.valueOf((long)sleep));
                    }

                    // Fill in Weight data (This will overwrite the profile weight)
                    if (documentSnapshot.contains("weight")) {
                        double dailyWeight = documentSnapshot.getDouble("weight");
                        eWeightData.setText(String.valueOf(dailyWeight));
                        tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", dailyWeight));
                    }

                    // Fill in Workout Time
                    if (documentSnapshot.contains("total_workout_minutes")) {
                        double minutes = documentSnapshot.getDouble("total_workout_minutes");
                        String formattedMinutes = String.format(Locale.getDefault(), "%.0f", minutes);
                        tvTimeMoved.setText(formattedMinutes);
                        // Show weekly time in 00:00 format
                        String weeklyFormat = String.format(Locale.getDefault(), "%02d:%02d", (int)minutes / 60, (int)minutes % 60);
                        tvTimeWorkedOutThisWeek.setText(weeklyFormat);
                    } else {
                        tvTimeMoved.setText("0");
                        tvTimeWorkedOutThisWeek.setText("00:00");
                    }

                    // Fill in Steps data
                    if (documentSnapshot.contains("steps")) {
                        double stepsDouble = documentSnapshot.getDouble("steps");
                        tvStepsData.setText(String.valueOf((long) stepsDouble));
                    } else {
                        tvStepsData.setText("0");
                    }

                    // Fill in Calories data
                    if (documentSnapshot.contains("cals_burnt")) {
                        double calsDouble = documentSnapshot.getDouble("cals_burnt");
                        tvCalsBurnt.setText(String.valueOf((long) calsDouble));
                    } else {
                        tvCalsBurnt.setText("0");
                    }
                } else {
                    // No daily document exists for today
                    Log.d("HomeActivity", "No daily data for " + todayDate);
                    tvTimeMoved.setText("0");
                    tvTimeWorkedOutThisWeek.setText("00:00");
                    tvStepsData.setText("0");
                    tvCalsBurnt.setText("0");
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(HomeActivity.this, "Error loading daily data", Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(HomeActivity.this, "Error loading profile data", Toast.LENGTH_SHORT).show();
        });
    }

    //Saves the user's sleep data
    private void saveSleepData() {
        String sleep = edSleepData.getText().toString().trim();
        if (sleep.isEmpty()) {
            Toast.makeText(this, "Please enter sleep data", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> activity = new HashMap<>();
        try {
            double sleepValue = Double.parseDouble(sleep);
            activity.put("sleep", sleepValue);
            activity.put("timestamp", new Date()); //Save the current time

            // Save the data
            firestore.collection("users").document(userId)
                    .collection("daily_activities").document(todayDate)
                    .set(activity, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(HomeActivity.this, "Sleep data saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }

    //Saves the user weight data
    private void saveWeightData() {
        String weight = eWeightData.getText().toString().trim();
        if (weight.isEmpty()) {
            Toast.makeText(this, "Please enter weight data", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> activity = new HashMap<>();
        try {
            double weightValue = Double.parseDouble(weight);

            // 1. Save to the daily log
            activity.put("weight", weightValue);
            activity.put("timestamp", new Date());

            firestore.collection("users").document(userId)
                    .collection("daily_activities").document(todayDate)
                    .set(activity, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(HomeActivity.this, "Weight data saved!", Toast.LENGTH_SHORT).show();
                        // Also update the text on the screen
                        tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", weightValue));
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            // 2. Also update the main profile document
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("weight", weightValue);

            firestore.collection("users").document(userId)
                    .set(profileData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d("HomeActivity", "Main profile weight updated.");
                    })
                    .addOnFailureListener(e -> {
                        Log.w("HomeActivity", "Error updating main profile weight", e);
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home); // Set Home as selected

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    // Go to Profile
                    startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }

                return false;
            }
        });
    }
}