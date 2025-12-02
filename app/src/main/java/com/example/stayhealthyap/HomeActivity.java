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
import com.google.android.material.navigation.NavigationBarView;
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
    private EditText eWeightData;
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
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        eWeightData = findViewById(R.id.eWeightData);
        btnEnterSleep = findViewById(R.id.btnEnterSleep);
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

    private void startActivitySleep() {
        Intent intent = new Intent(HomeActivity.this, SleepActivity.class);
        startActivity(intent);
    }

    private void startWorkout(String workoutType) {
        Intent intent = new Intent(HomeActivity.this, WorkoutActivity.class);
        intent.putExtra("WORKOUT_TYPE", workoutType);
        startActivity(intent);
    }

    private void loadPageData() {
        DocumentReference profileDocRef = firestore.collection("users").document(userId);

        profileDocRef.get().addOnSuccessListener(profileSnapshot -> {
            if (profileSnapshot.exists() && profileSnapshot.contains("weight")) {
                double profileWeight = profileSnapshot.getDouble("weight");
                tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", profileWeight));
            } else {
                tvWeight.setText("0.0 kg");
            }

            DocumentReference dailyDocRef = firestore.collection("users").document(userId)
                    .collection("daily_activities").document(todayDate);

            dailyDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Log.d("HomeActivity", "Daily data found for " + todayDate);

                    if (documentSnapshot.contains("weight")) {
                        double dailyWeight = documentSnapshot.getDouble("weight");
                        eWeightData.setText(String.valueOf(dailyWeight));
                        tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", dailyWeight));
                    }

                    if (documentSnapshot.contains("total_workout_seconds")) {
                        long totalSeconds = documentSnapshot.getLong("total_workout_seconds");

                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        long seconds = totalSeconds % 60;

                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

                        tvTimeMoved.setText(formattedTime);
                        tvTimeWorkedOutThisWeek.setText(formattedTime);

                    } else if (documentSnapshot.contains("total_workout_minutes")) {
                        Double minutesVal = documentSnapshot.getDouble("total_workout_minutes");
                        double minutes = (minutesVal != null) ? minutesVal : 0.0;

                        String formattedMinutes = String.format(Locale.getDefault(), "%.0f", minutes);
                        tvTimeMoved.setText(formattedMinutes);

                        String weeklyFormat = String.format(Locale.getDefault(), "%02d:%02d", (int)minutes / 60, (int)minutes % 60);
                        tvTimeWorkedOutThisWeek.setText(weeklyFormat);
                    } else {
                        // No workout data yet
                        tvTimeMoved.setText("00:00:00");
                        tvTimeWorkedOutThisWeek.setText("00:00:00");
                    }

                    if (documentSnapshot.contains("steps")) {
                        double stepsDouble = documentSnapshot.getDouble("steps");
                        tvStepsData.setText(String.valueOf((long) stepsDouble));
                    } else {
                        tvStepsData.setText("0");
                    }

                    if (documentSnapshot.contains("cals_burnt")) {
                        double calsDouble = documentSnapshot.getDouble("cals_burnt");
                        tvCalsBurnt.setText(String.valueOf((long) calsDouble));
                    } else {
                        tvCalsBurnt.setText("0");
                    }
                } else {
                    Log.d("HomeActivity", "No daily data for " + todayDate);
                    tvTimeMoved.setText("00:00:00");
                    tvTimeWorkedOutThisWeek.setText("00:00:00");
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
    private void saveWeightData() {
        String weight = eWeightData.getText().toString().trim();
        if (weight.isEmpty()) {
            Toast.makeText(this, "Please enter weight data", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> activity = new HashMap<>();
        try {
            double weightValue = Double.parseDouble(weight);

            activity.put("weight", weightValue);
            activity.put("timestamp", new Date());

            firestore.collection("users").document(userId)
                    .collection("daily_activities").document(todayDate)
                    .set(activity, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(HomeActivity.this, "Weight data saved!", Toast.LENGTH_SHORT).show();
                        tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", weightValue));
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

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
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            }
            else if (itemId == R.id.nav_communite) {
                Intent intent = new Intent(getApplicationContext(), CommunityActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}