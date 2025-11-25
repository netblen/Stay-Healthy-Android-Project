package com.example.stayhealthyap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class LogoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);

        Button btnConfirm = findViewById(R.id.btnConfirmLogout);
        Button btnCancel = findViewById(R.id.btnCancelLogout);

        // LOGOUT BUTTON
        btnConfirm.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            Intent i = new Intent(LogoutActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        // CANCEL BUTTON
        btnCancel.setOnClickListener(v -> {
            finish(); // go back to ProfileActivity
        });
    }
}
