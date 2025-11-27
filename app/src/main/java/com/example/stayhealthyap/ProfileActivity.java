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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String currentUserId;

    // UI Elements
    private TextView tvUsername, tvCurrentGroupProfile;
    private Button btnEdit, btnLogout; // Added Logout
    private EditText edWeight, edHeight;
    private RadioGroup rgGoals;
    private RadioButton rbWeightLoss, rbBuildMuscle, rbStayActive;
    private Switch switchNotifs;

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

        // 1. Find all UI Elements
        tvUsername = findViewById(R.id.textView);
        tvCurrentGroupProfile = findViewById(R.id.tvCurrentGroupProfile);
        btnEdit = findViewById(R.id.btnEdit);
        btnLogout = findViewById(R.id.btnLogout); // Find Logout
        edWeight = findViewById(R.id.edWeight);
        edHeight = findViewById(R.id.edHeight);
        rgGoals = findViewById(R.id.rgGoals);
        rbWeightLoss = findViewById(R.id.rbWeightLoss);
        rbBuildMuscle = findViewById(R.id.BuildMuscle);
        rbStayActive = findViewById(R.id.rbStayActive);
        switchNotifs = findViewById(R.id.switchNotifs);

        btnEdit.setEnabled(true);

        setupBottomNavigation();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadUserProfile();
        } else {
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        }

        // Save Button Listener
        btnEdit.setOnClickListener(v -> saveProfileData());

        // LOGOUT LOGIC
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            // Clear back stack so user can't press back to return
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        if (currentUserId == null) return;

        DocumentReference docRef = firestore.collection("users").document(currentUserId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d("ProfileActivity", "User data found!");

                // 1. Set Username
                String name = documentSnapshot.getString("name");
                if (name != null && !name.isEmpty()) {
                    tvUsername.setText(name);
                } else if (auth.getCurrentUser().getEmail() != null) {
                    tvUsername.setText(auth.getCurrentUser().getEmail().split("@")[0]);
                }

                // 2. Set Group Status
                String group = documentSnapshot.getString("currentGroup");
                if (group != null && !group.isEmpty()) {
                    tvCurrentGroupProfile.setText("Member of " + group);
                    tvCurrentGroupProfile.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                } else {
                    tvCurrentGroupProfile.setText("Not in a group");
                    tvCurrentGroupProfile.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }

                // 3. Set Weight
                Double weight = documentSnapshot.getDouble("weight");
                if (weight != null) {
                    edWeight.setText(String.valueOf(weight));
                } else {
                    edWeight.setText("0.0");
                }

                // 4. Set Height
                Double height = documentSnapshot.getDouble("height");
                if (height != null) {
                    edHeight.setText(String.valueOf(height));
                } else {
                    edHeight.setText("0.0");
                }

                // 5. Set Goal
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
                    rbStayActive.setChecked(true);
                }

                // 6. Set Notifications
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

        double weight = 0.0;
        double height = 0.0;

        try {
            weight = Double.parseDouble(edWeight.getText().toString());
            height = Double.parseDouble(edHeight.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        String goal = "";
        int selectedGoalId = rgGoals.getCheckedRadioButtonId();
        if (selectedGoalId == R.id.rbWeightLoss) {
            goal = "Weight loss";
        } else if (selectedGoalId == R.id.BuildMuscle) {
            goal = "Build up Muscle";
        } else if (selectedGoalId == R.id.rbStayActive) {
            goal = "Stay Active";
        }

        boolean notificationsOn = switchNotifs.isChecked();

        Map<String, Object> userData = new HashMap<>();
        userData.put("weight", weight);
        userData.put("height", height);
        userData.put("goal", goal);
        userData.put("notifications", notificationsOn);

        firestore.collection("users").document(currentUserId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                else if (itemId == R.id.nav_communite) {
                    startActivity(new Intent(getApplicationContext(), CommunityActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                else if (itemId == R.id.nav_profile) {
                    return true;
                }
                return false;
            }
        });
    }
}