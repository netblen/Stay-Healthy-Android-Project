package com.example.stayhealthyap;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SleepActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnChooseDay, btnEnterSleep, btnSave;
    private ImageView btnBack;
    private TextView tvSleepHrs, tvSleepMins, tvSleepStatus, tvSleepTitle;
    private TableLayout tableSleepHistory;

    private Calendar selectedCalendar;
    private Calendar sleepTimeCalendar;
    private Calendar wakeTimeCalendar;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

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
        setupFirebase();
        setupDefaultDate();
        loadSleepDataForSelectedDate();
        loadWeeklyHistory(); //Load the table data on start
    }

    private void initialize() {
        btnChooseDay = findViewById(R.id.btnChooseDay);
        btnChooseDay.setOnClickListener(this);

        btnEnterSleep = findViewById(R.id.btnEnterSleep);
        btnEnterSleep.setOnClickListener(this);

        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvSleepHrs = findViewById(R.id.tvSleepHrs);
        tvSleepMins = findViewById(R.id.tvSleepMins);
        tvSleepStatus = findViewById(R.id.tvSleepStatus);
        tvSleepTitle = findViewById(R.id.tvSleepTitle);
        tableSleepHistory = findViewById(R.id.tableSleepHistory);

        selectedCalendar = Calendar.getInstance();
        sleepTimeCalendar = Calendar.getInstance();
        wakeTimeCalendar = Calendar.getInstance();

        resetToDefaultTimes();
        updateSelectedDateDisplay();
    }

    private void setupFirebase() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private void setupDefaultDate() {
        updateSelectedDateDisplay();
    }

    private void updateSelectedDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        if (selectedCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            btnChooseDay.setText("Today");
        } else {
            btnChooseDay.setText(dateFormat.format(selectedCalendar.getTime()));
        }
    }

    private void updateSleepDurationDisplay() {
        long durationMillis = calculateSleepDuration();
        int hours = (int) (durationMillis / (1000 * 60 * 60));
        int minutes = (int) ((durationMillis % (1000 * 60 * 60)) / (1000 * 60));

        tvSleepHrs.setText(hours + " h");
        tvSleepMins.setText(minutes + " m");
    }

    private long calculateSleepDuration() {
        Calendar wakeTime = (Calendar) wakeTimeCalendar.clone();
        if (wakeTime.before(sleepTimeCalendar)) {
            wakeTime.add(Calendar.DATE, 1);
        }
        return wakeTime.getTimeInMillis() - sleepTimeCalendar.getTimeInMillis();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnChooseDay) {
            showCalenderDialog();
        } else if (id == R.id.btnEnterSleep) {
            showSleepTimePicker();
        } else if (id == R.id.btnSave) {
            saveSleepData();
        }
    }

    private void showSleepTimePicker() {
        TimePickerDialog sleepTimePicker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    sleepTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    sleepTimeCalendar.set(Calendar.MINUTE, minute);
                    showWakeTimePicker();
                },
                sleepTimeCalendar.get(Calendar.HOUR_OF_DAY),
                sleepTimeCalendar.get(Calendar.MINUTE),
                false);
        sleepTimePicker.setTitle("Select Sleep Time");
        sleepTimePicker.show();
    }

    private void showWakeTimePicker() {
        TimePickerDialog wakeTimePicker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    wakeTimeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    wakeTimeCalendar.set(Calendar.MINUTE, minute);

                    updateSleepDurationDisplay();
                    tvSleepStatus.setText("Review time and click Save");
                    btnSave.setVisibility(View.VISIBLE);
                },
                wakeTimeCalendar.get(Calendar.HOUR_OF_DAY),
                wakeTimeCalendar.get(Calendar.MINUTE),
                false);
        wakeTimePicker.setTitle("Select Wake Time");
        wakeTimePicker.show();
    }

    private void showCalenderDialog() {
        BottomSheetDialog btmSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.calender_popup, null);

        CalendarView calendarView = view.findViewById(R.id.calendarView);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSet = view.findViewById(R.id.btnSet);

        final Calendar tempSelectedCal = (Calendar) selectedCalendar.clone();

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            tempSelectedCal.set(Calendar.YEAR, year);
            tempSelectedCal.set(Calendar.MONTH, month);
            tempSelectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        });

        btnCancel.setOnClickListener(v -> btmSheetDialog.dismiss());
        btnSet.setOnClickListener(v -> {
            selectedCalendar.setTime(tempSelectedCal.getTime());
            updateSelectedDateDisplay();
            loadSleepDataForSelectedDate();
            btmSheetDialog.dismiss();
            Snackbar.make(findViewById(android.R.id.content), "Date updated", Snackbar.LENGTH_SHORT).show();
        });

        btmSheetDialog.setContentView(view);
        btmSheetDialog.show();
    }

    private void saveSleepData() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.getTime());
        String sleepTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(sleepTimeCalendar.getTime());
        String wakeTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(wakeTimeCalendar.getTime());
        long duration = calculateSleepDuration();

        Map<String, Object> sleepData = new HashMap<>();
        sleepData.put("date", date);
        sleepData.put("sleepTime", sleepTime);
        sleepData.put("wakeTime", wakeTime);
        sleepData.put("duration", duration);

        DocumentReference ref = firestore.collection("sleepData")
                .document(uid)
                .collection("dates")
                .document(date);

        ref.set(sleepData).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Sleep data saved!", Toast.LENGTH_SHORT).show();
            tvSleepStatus.setText("Data saved successfully.");
            btnSave.setVisibility(View.GONE);
            loadWeeklyHistory(); //Refresh the table after saving
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show());
    }

    private void loadSleepDataForSelectedDate() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCalendar.getTime());

        btnSave.setVisibility(View.GONE);

        firestore.collection("sleepData")
                .document(uid)
                .collection("dates")
                .document(date)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String sleepTime = doc.getString("sleepTime");
                        String wakeTime = doc.getString("wakeTime");
                        Long durationObj = doc.getLong("duration");
                        long duration = (durationObj != null) ? durationObj : 0;

                        try {
                            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            sleepTimeCalendar.setTime(fmt.parse(sleepTime));
                            wakeTimeCalendar.setTime(fmt.parse(wakeTime));
                        } catch (Exception ignored) {}

                        updateSleepDurationDisplay();
                        tvSleepStatus.setText("Sleep recorded for this day");
                    } else {
                        resetToDefaultTimes();
                        tvSleepStatus.setText("No data recorded for this day");
                    }
                });
    }


    private void loadWeeklyHistory() {
        if (auth.getCurrentUser() == null) return;

        if (tableSleepHistory == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        int childCount = tableSleepHistory.getChildCount();
        if (childCount > 1) {
            tableSleepHistory.removeViews(1, childCount - 1);
        }

        firestore.collection("sleepData")
                .document(uid)
                .collection("dates")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(7)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String dateStr = doc.getString("date");
                        Long durationVal = doc.getLong("duration");
                        String sleepT = doc.getString("sleepTime");
                        String wakeT = doc.getString("wakeTime");
                        long duration = (durationVal != null) ? durationVal : 0;

                        int hours = (int) (duration / (1000 * 60 * 60));
                        int minutes = (int) ((duration % (1000 * 60 * 60)) / (1000 * 60));
                        String durationText = hours + "h " + minutes + "m";

                        String shortDate = dateStr;
                        try {
                            SimpleDateFormat dbFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            SimpleDateFormat displayFmt = new SimpleDateFormat("MMM d", Locale.getDefault());
                            shortDate = displayFmt.format(dbFmt.parse(dateStr));
                        } catch (Exception ignored) {}

                        TableRow row = new TableRow(this);
                        row.setPadding(0, 15, 0, 15);

                        TextView tvDate = new TextView(this);
                        tvDate.setText(shortDate);
                        tvDate.setTextColor(Color.WHITE);
                        tvDate.setTextSize(14);
                        row.addView(tvDate);

                        TextView tvDur = new TextView(this);
                        tvDur.setText(durationText);
                        tvDur.setTextColor(Color.WHITE);
                        tvDur.setTextSize(14);
                        tvDur.setGravity(Gravity.CENTER);
                        row.addView(tvDur);

                        TextView tvTime = new TextView(this);
                        tvTime.setText(sleepT + " - " + wakeT);
                        tvTime.setTextColor(Color.LTGRAY);
                        tvTime.setTextSize(12);
                        tvTime.setGravity(Gravity.END);
                        row.addView(tvTime);

                        tableSleepHistory.addView(row);
                    }
                });
    }

    private void resetToDefaultTimes() {
        sleepTimeCalendar.set(Calendar.HOUR_OF_DAY, 22);
        sleepTimeCalendar.set(Calendar.MINUTE, 0);
        wakeTimeCalendar.set(Calendar.HOUR_OF_DAY, 6);
        wakeTimeCalendar.set(Calendar.MINUTE, 0);
        tvSleepHrs.setText("__ h");
        tvSleepMins.setText("__ m");
        if (tvSleepStatus != null) {
            tvSleepStatus.setText("Record your sleep to see patterns");
        }
    }
}