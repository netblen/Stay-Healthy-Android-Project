package com.example.stayhealthyap;

import android.content.Intent;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private TextView tvUsername;
    private Button btnEdit, btnLogout;
    private EditText edWeight, edHeight;
    private RadioGroup rgGoals;
    private RadioButton rbWeightLoss, rbBuildMuscle, rbStayActive;
    private Switch switchNotifs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // SAFE INSETS LISTENER: Checks if 'main' exists before using it
        if (findViewById(R.id.main) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // Initialize Views
        tvUsername = findViewById(R.id.textView);
        btnEdit = findViewById(R.id.btnEdit);
        btnLogout = findViewById(R.id.btnLogout);
        edWeight = findViewById(R.id.edWeight);
        edHeight = findViewById(R.id.edHeight);
        rgGoals = findViewById(R.id.rgGoals);
        rbWeightLoss = findViewById(R.id.rbWeightLoss);
        rbBuildMuscle = findViewById(R.id.BuildMuscle);
        rbStayActive = findViewById(R.id.rbStayActive);
        switchNotifs = findViewById(R.id.switchNotifs);

        setupBottomNavigation();

        // Check if user is logged in
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadUserProfile();
        } else {
            // Redirect to Login if no user found
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> saveProfileData());
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                auth.signOut();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadUserProfile() {
        if (currentUserId == null) return;
        DocumentReference docRef = firestore.collection("users").document(currentUserId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d("ProfileActivity", "User data found!");

                String name = documentSnapshot.getString("name");
                if (tvUsername != null) tvUsername.setText(name != null ? name : "User");

                Double weight = documentSnapshot.getDouble("weight");
                if (edWeight != null) edWeight.setText(String.valueOf(weight != null ? weight : 0.0));

                Double height = documentSnapshot.getDouble("height");
                if (edHeight != null) edHeight.setText(String.valueOf(height != null ? height : 0.0));

                String goal = documentSnapshot.getString("goal");
                if (goal != null) {
                    if (goal.equals("Weight loss") && rbWeightLoss != null) {
                        rbWeightLoss.setChecked(true);
                    } else if (goal.equals("Build up Muscle") && rbBuildMuscle != null) {
                        rbBuildMuscle.setChecked(true);
                    } else if (goal.equals("Stay Active") && rbStayActive != null) {
                        rbStayActive.setChecked(true);
                    }
                } else if (rbStayActive != null) {
                    rbStayActive.setChecked(true);
                }

                Boolean notifications = documentSnapshot.getBoolean("notifications");
                if (switchNotifs != null) switchNotifs.setChecked(notifications != null && notifications);

                // Enable the edit button once data is loaded
                if (btnEdit != null) btnEdit.setEnabled(true);

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
            Toast.makeText(this, "Please enter valid numbers for weight and height", Toast.LENGTH_SHORT).show();
            return;
        }

        String goal = "Stay Active"; // Default
        if (rgGoals != null) {
            int selectedGoalId = rgGoals.getCheckedRadioButtonId();
            if (selectedGoalId == R.id.rbWeightLoss) {
                goal = "Weight loss";
            } else if (selectedGoalId == R.id.BuildMuscle) {
                goal = "Build up Muscle";
            }
        }

        boolean notificationsOn = switchNotifs != null && switchNotifs.isChecked();

        Map<String, Object> userData = new HashMap<>();
        userData.put("weight", weight);
        userData.put("height", height);
        userData.put("goal", goal);
        userData.put("notifications", notificationsOn);

        DocumentReference docRef = firestore.collection("users").document(currentUserId);

        docRef.set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    Log.e("ProfileActivity", "Error saving data", e);
                });
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView == null) return; // Prevent crash if view is missing

        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                    // Prevent infinite stack loop
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_communite) {
                    Intent intent = new Intent(getApplicationContext(), CommunityActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    return true;
                }
                return false;
            }
        });
    }
}