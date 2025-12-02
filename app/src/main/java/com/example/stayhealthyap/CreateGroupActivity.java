package com.example.stayhealthyap;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class CreateGroupActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etGroupName;
    private ImageView iconRun, iconSwim, iconWorkout, iconWalk;
    private Button btnCreateConfirm;
    private ImageButton btnBack;

    private String selectedIcon = "ic_run"; // Default
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etGroupName = findViewById(R.id.etGroupName);
        iconRun = findViewById(R.id.iconRun);
        iconSwim = findViewById(R.id.iconSwim);
        iconWorkout = findViewById(R.id.iconWorkout);
        iconWalk = findViewById(R.id.iconWalk);
        btnCreateConfirm = findViewById(R.id.btnCreateConfirm);
        btnBack = findViewById(R.id.btnBack);

        // Icon Selection Logic
        iconRun.setOnClickListener(v -> selectIcon("ic_run", iconRun));
        iconSwim.setOnClickListener(v -> selectIcon("ic_swimming", iconSwim));
        iconWorkout.setOnClickListener(v -> selectIcon("ic_circuit_workout", iconWorkout));
        iconWalk.setOnClickListener(v -> selectIcon("ic_walk", iconWalk));

        btnCreateConfirm.setOnClickListener(v -> createGroup());

        btnBack.setOnClickListener(v -> finish());
    }

    private void selectIcon(String iconName, ImageView selectedView) {
        this.selectedIcon = iconName;

        int darkColor = Color.parseColor("#181818");
        int selectedColor = Color.parseColor("#333333");

        iconRun.setBackgroundTintList(ColorStateList.valueOf(darkColor));
        iconSwim.setBackgroundTintList(ColorStateList.valueOf(darkColor));
        iconWorkout.setBackgroundTintList(ColorStateList.valueOf(darkColor));
        iconWalk.setBackgroundTintList(ColorStateList.valueOf(darkColor));

        selectedView.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
    }

    private void createGroup() {
        String name = etGroupName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        //Check if group name ALREADY exists
        db.collection("groups").document(name).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(this, "Group name already taken!", Toast.LENGTH_SHORT).show();
                    } else {
                        checkUserAndCreate(name, userId);
                    }
                });
    }

    private void checkUserAndCreate(String name, String userId) {
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists() && userDoc.contains("currentGroup")) {
                String currentGroup = userDoc.getString("currentGroup");

                if (currentGroup != null && !currentGroup.isEmpty()) {
                    Toast.makeText(this, "You are already in a group (" + currentGroup + "). Leave it first.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            finalizeGroupCreation(name, userId);
        });
    }

    private void finalizeGroupCreation(String name, String userId) {
        String userEmail = auth.getCurrentUser().getEmail();
        String username = (userEmail != null) ? userEmail.split("@")[0] : "User";

        // 1. Group Data
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("groupName", name);
        groupData.put("icon", selectedIcon);
        groupData.put("adminId", userId);
        groupData.put("memberCount", 1);

        // 2. Member Data
        Map<String, Object> memberData = new HashMap<>();
        memberData.put("username", username);
        memberData.put("joinedAt", System.currentTimeMillis());

        // 3. User Profile Update
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("currentGroup", name);

        // A. Save Group Details
        db.collection("groups").document(name).set(groupData)
                .addOnSuccessListener(aVoid -> {

                    // B. Add Creator to Members List
                    db.collection("groups").document(name).collection("members").document(userId).set(memberData);

                    // C. Update Creator's Profile
                    db.collection("users").document(userId).set(userUpdate, SetOptions.merge());

                    Toast.makeText(this, "Group Created!", Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error creating group", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onClick(View v) {

    }
}