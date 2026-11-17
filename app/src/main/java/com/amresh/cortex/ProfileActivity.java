package com.amresh.cortex;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

/**
 * Profile: the name used in the greeting, date and time of birth, and the life counter.
 * The counter only changes when the user taps Update, and it shows when it was last updated.
 */
public class ProfileActivity extends AppCompatActivity {

    private EditText etName;
    private Button btnDate, btnTime;
    private TextView tvDays, tvBreakdown, tvHours, tvStamp;

    /** Birth date chosen on screen (midnight), or 0 when none. */
    private long birthDate;
    /** Time of birth as minutes after midnight, or -1 when not given. */
    private int birthMinutes = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setTitle(R.string.profile_title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName = findViewById(R.id.etProfileName);
        btnDate = findViewById(R.id.btnBirthDate);
        btnTime = findViewById(R.id.btnBirthTime);
        tvDays = findViewById(R.id.tvLifeDays);
        tvBreakdown = findViewById(R.id.tvLifeBreakdown);
        tvHours = findViewById(R.id.tvLifeHours);
        tvStamp = findViewById(R.id.tvLifeStamp);

        etName.setText(Prefs.getName(this));

        long birth = Prefs.getBirthMillis(this);
        if (birth > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(birth);
            if (Prefs.hasBirthTime(this)) {
                birthMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
            }
            birthDate = ScheduleManager.startOfDay(birth);
        }
        showBirth();
        showLife();

        btnDate.setOnClickListener(v -> pickDate());
        btnTime.setOnClickListener(v -> pickTime());
        findViewById(R.id.btnBirthTimeClear).setOnClickListener(v -> {
            birthMinutes = -1;
            showBirth();
        });
        findViewById(R.id.btnProfileSave).setOnClickListener(v -> {
            save();
            Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
            showLife();
        });
        findViewById(R.id.btnLifeUpdate).setOnClickListener(v -> {
            save();
            if (Prefs.getBirthMillis(this) <= 0) {
                Toast.makeText(this, R.string.life_need_birth, Toast.LENGTH_LONG).show();
                return;
            }
            Prefs.setLifeUpdatedAt(this, System.currentTimeMillis());
            showLife();
        });
    }

    // ------------------------------------------------------------- birth

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        if (birthDate > 0) c.setTimeInMillis(birthDate);
        else c.set(c.get(Calendar.YEAR) - 20, Calendar.JANUARY, 1);
        DatePickerDialog d = new DatePickerDialog(this, (dp, year, month, day) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, day, 0, 0, 0);
            picked.set(Calendar.MILLISECOND, 0);
            birthDate = picked.getTimeInMillis();
            showBirth();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.getDatePicker().setMaxDate(System.currentTimeMillis());
        d.show();
    }

    private void pickTime() {
        int h = birthMinutes >= 0 ? birthMinutes / 60 : 12;
        int m = birthMinutes >= 0 ? birthMinutes % 60 : 0;
        new TimePickerDialog(this, (tp, hour, minute) -> {
            birthMinutes = hour * 60 + minute;
            showBirth();
        }, h, m, DateFormat.is24HourFormat(this)).show();
    }

    private void showBirth() {
        btnDate.setText(birthDate > 0
                ? DateFormat.format("d MMMM yyyy", birthDate).toString()
                : getString(R.string.profile_set_date));
        if (birthMinutes >= 0) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, birthMinutes / 60);
            c.set(Calendar.MINUTE, birthMinutes % 60);
            btnTime.setText(DateFormat.format("h:mm a", c).toString());
        } else {
            btnTime.setText(R.string.profile_set_time);
        }
    }

    /** The full birth moment: the chosen date plus the time of birth (midnight if no time was given). */
    private long birthMoment() {
        if (birthDate <= 0) return 0;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(birthDate);
        if (birthMinutes >= 0) {
            c.set(Calendar.HOUR_OF_DAY, birthMinutes / 60);
            c.set(Calendar.MINUTE, birthMinutes % 60);
        }
        return c.getTimeInMillis();
    }

    private void save() {
        Prefs.setName(this, etName.getText().toString());
        Prefs.setBirth(this, birthMoment(), birthMinutes >= 0);
    }

    // ------------------------------------------------------ life counter

    private void showLife() {
        long updatedAt = Prefs.getLifeUpdatedAt(this);
        long birth = Prefs.getBirthMillis(this);
        LifeCounter.Result r = (updatedAt > 0 && birth > 0) ? LifeCounter.compute(birth, updatedAt) : null;

        if (r == null) {
            tvDays.setText(R.string.life_not_updated);
            tvBreakdown.setText("");
            tvHours.setText("");
            tvStamp.setText("");
            return;
        }
        tvDays.setText(getString(R.string.life_days, r.totalDays));
        tvBreakdown.setText(getString(R.string.life_breakdown, r.years, r.months, r.days, r.hours, r.minutes));
        tvHours.setText(getString(R.string.life_hours, r.totalHours));
        tvStamp.setText(getString(R.string.life_last_updated,
                DateFormat.format("d MMM yyyy, h:mm a", updatedAt)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep the name and birth details even if the screen is left without tapping Save.
        save();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
