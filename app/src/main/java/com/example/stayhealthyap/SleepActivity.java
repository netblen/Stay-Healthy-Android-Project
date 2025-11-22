package com.example.stayhealthyap;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

public class SleepActivity extends AppCompatActivity implements View.OnClickListener {

    Button  btnChooseDay, btnEnterSleep;

    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sleep);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initialize();

    }

    private void initialize() {
        btnChooseDay = findViewById(R.id.btnChooseDay);
        btnChooseDay.setOnClickListener(this);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish()); // Back button

    }

/// get the time from the tv hrs ands mins and when they click enter data u display it where  they enter it
/// user acan change what day they want to enter the sleep for by the dialog calaender popup by clicking today



    @Override
    public void onClick(View v) {
        //on btn click btnChooseDay
        if(v.getId() == R.id.btnChooseDay){

            //Snackbar.make(v, "clicked today btn shoud be showingf u the dialog now", Snackbar.LENGTH_LONG).show();
            showCalenderDialog();

        }
    }

    private void showCalenderDialog() {
        BottomSheetDialog btmSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.calender_popup, null);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btmSheetDialog.dismiss(); //pretty sure we can just close it
            }
        });

        Button btnSet = view.findViewById(R.id.btnSet);
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ///change it to the right day

            }
        });

        btmSheetDialog.setContentView(view);
        btmSheetDialog.show();

    }
}