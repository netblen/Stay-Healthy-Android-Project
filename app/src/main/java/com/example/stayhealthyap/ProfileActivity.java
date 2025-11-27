package com.example.stayhealthyap;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private TextView tvUsername;
    private Button btnEdit;
    private EditText edWeight, edHeight;
    private RadioGroup rgGoals;
    private RadioButton rbWeightLoss, rbBuildMuscle, rbStayActive;
    private Switch switchNotifs;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        tvUsername = findViewById(R.id.textView);
        btnEdit = findViewById(R.id.btnEdit);
        edWeight = findViewById(R.id.edWeight);
        edHeight = findViewById(R.id.edHeight);
        rgGoals = findViewById(R.id.rgGoals);
        rbWeightLoss = findViewById(R.id.rbWeightLoss);
        rbBuildMuscle = findViewById(R.id.BuildMuscle);
        rbStayActive = findViewById(R.id.rbStayActive);
        switchNotifs = findViewById(R.id.switchNotifs);
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // Directly open LogoutActivity
            startActivity(new Intent(ProfileActivity.this, LogoutActivity.class));
        });


        setupBottomNavigation();
        // Check if user is logged in
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadUserProfile();
        } else {
            // User is not logged in, maybe send them to LoginActivity
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        }

        //Set up the Save Button (your btnEdit)
        btnEdit.setOnClickListener(v -> saveProfileData());
    }

    private void loadUserProfile() {
        if (currentUserId == null) return;
        DocumentReference docRef = firestore.collection("users").document(currentUserId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d("ProfileActivity", "User data found!");

                //Set Username (which you save as "name" during registration)
                String name = documentSnapshot.getString("name");
                tvUsername.setText(name);

                // Set Weight
                Double weight = documentSnapshot.getDouble("weight");
                if (weight != null) {
                    edWeight.setText(String.valueOf(weight));
                } else {
                    edWeight.setText("0.0");
                }

                // Set Height
                Double height = documentSnapshot.getDouble("height");
                if (height != null) {
                    edHeight.setText(String.valueOf(height));
                } else {
                    edHeight.setText("0.0");
                }

                // Set Goal
                String goal = documentSnapshot.getString("goal");
                if (goal != null) {
                    if (goal.equals("Weight loss")) {
                        rbWeightLoss.setChecked(true);
                    } else if (goal.equals("Build up Muscle")) {
                        rbBuildMuscle.setChecked(true);
                    } else if (goal.equals("Stay Active")) {
                        rbStayActive.setChecked(true);
                    }
                } else {
                    rbStayActive.setChecked(true); // Default goal
                }

                // Set Notifications Switch
                Boolean notifications = documentSnapshot.getBoolean("notifications");
                if (notifications != null) {
                    switchNotifs.setChecked(notifications);
                } else {
                    switchNotifs.setChecked(false);
                }

            } else {
                Log.d("ProfileActivity", "No user data found in Firestore");
            }
        }).addOnFailureListener(e -> {
            Log.e("ProfileActivity", "Error loading user data", e);
            Toast.makeText(ProfileActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProfileData() {
        if (currentUserId == null) return;

        //Get values from UI elements
        double weight = 0.0;
        double height = 0.0;

        // Use try-catch to prevent crash if user enters invalid number
        try {
            weight = Double.parseDouble(edWeight.getText().toString());
            height = Double.parseDouble(edHeight.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for weight and height", Toast.LENGTH_SHORT).show();
            return; // Stop saving if numbers are invalid
        }

        //Get selected goal
        String goal = "";
        int selectedGoalId = rgGoals.getCheckedRadioButtonId();
        if (selectedGoalId == R.id.rbWeightLoss) {
            goal = "Weight loss";
        } else if (selectedGoalId == R.id.BuildMuscle) {
            goal = "Build up Muscle";
        } else if (selectedGoalId == R.id.rbStayActive) {
            goal = "Stay Active";
        }

        // Get notification preference
        boolean notificationsOn = switchNotifs.isChecked();

        // --- Create a Map to send to Firestore ---
        Map<String, Object> userData = new HashMap<>();
        userData.put("weight", weight);
        userData.put("height", height);
        userData.put("goal", goal);
        userData.put("notifications", notificationsOn);

        DocumentReference docRef = firestore.collection("users").document(currentUserId);

        docRef.set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    btnEdit.setText("Edit"); //You can change text back if you want at same time this is the save button
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    Log.e("ProfileActivity", "Error saving data", e);
                });
    }

    private void setupBottomNavigation() {
        //bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        //bottomNavigationView.setSelectedItemId(R.id.nav_profile);
       // bottomNavigationView.setSelectedItemId(R.id.nav_chats); // this line is overriding the one on top so I'll comment it out

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    overridePendingTransition(0, 0);
                    finish(); // Optional: close this activity
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    // Already on profile, do nothing or refresh
                    return true;
                }
                return false;
            }



//            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//                int itemId = item.getItemId();
//                if (itemId == R.id.nav_home) {
//                    // Go to Home
//                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
//                    overridePendingTransition(0, 0);
//                    return true;
//                } else return itemId == R.id.nav_profile;
//            }
        });
    }
}