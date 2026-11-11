package com.amresh.cortex;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Cortex main screen: greeting on top, three tabs below (Tasks | Assistant | Memory),
 * switched by tapping the bottom bar or swiping. The top menu holds the settings.
 */
public class MainActivity extends AppCompatActivity {

    private static final int[] NIGHT_MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };

    /** Choices for the second reminder gap, in minutes. 0 means off. */
    private static final int[] SECOND_GAPS = {0, 10, 15, 30, 45, 60};

    private AppDatabase db;
    private TextView tvGreeting, tvSubtitle;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) doExport(uri);
            });

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) doImport(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.get(this);
        ReminderReceiver.createChannel(this);

        // Remove bin items that are older than 15 days.
        AppDatabase.IO.execute(() ->
                db.taskDao().purgeBinOlderThan(System.currentTimeMillis() - AppDatabase.BIN_RETENTION_MILLIS));

        tvGreeting = findViewById(R.id.tvGreeting);
        tvSubtitle = findViewById(R.id.tvSubtitle);

        // "N tasks left" line under the greeting.
        db.taskDao().observeActive().observe(this, this::updateSubtitle);

        setupTabs();
        askForPermissions();

        if (!Prefs.wasNameAsked(this)) {
            Prefs.setNameAsked(this);
            showNameDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreeting();
    }

    // ------------------------------------------------------------------ tabs

    /** Tab order from the user's side: Task (left), Assistant (middle), Memory (right). */
    private void setupTabs() {
        final ViewPager2 pager = findViewById(R.id.pager);
        final BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        pager.setAdapter(new FragmentStateAdapter((FragmentActivity) this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 1:
                        return new AssistantFragment();
                    case 2:
                        return new MemoryFragment();
                    default:
                        return new TasksFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

        // Tapping a tab moves the pager ...
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_tasks) {
                pager.setCurrentItem(0, true);
            } else if (id == R.id.nav_assistant) {
                pager.setCurrentItem(1, true);
            } else if (id == R.id.nav_memory) {
                pager.setCurrentItem(2, true);
            }
            return true;
        });

        // ... and swiping the pager moves the highlighted tab.
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int id = position == 1 ? R.id.nav_assistant
                        : position == 2 ? R.id.nav_memory : R.id.nav_tasks;
                bottomNav.setSelectedItemId(id);
            }
        });
    }

    // ---------------------------------------------------------------- menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_bin) {
            startActivity(new Intent(this, BinActivity.class));
            return true;
        } else if (id == R.id.menu_second_reminder) {
            showSecondReminderDialog();
            return true;
        } else if (id == R.id.menu_spoken) {
            showSpokenDialog();
            return true;
        } else if (id == R.id.menu_theme_mode) {
            showThemeModeDialog();
            return true;
        } else if (id == R.id.menu_accent) {
            showAccentDialog();
            return true;
        } else if (id == R.id.menu_name) {
            showNameDialog();
            return true;
        } else if (id == R.id.menu_backup) {
            String stamp = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
            exportLauncher.launch("Cortex-backup-" + stamp + ".json");
            return true;
        } else if (id == R.id.menu_restore) {
            importLauncher.launch(new String[]{"*/*"});
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------ name + greeting

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String hello;
        if (hour >= 5 && hour < 12) hello = "Good morning";
        else if (hour >= 12 && hour < 17) hello = "Good afternoon";
        else if (hour >= 17 && hour < 21) hello = "Good evening";
        else hello = "Hello";
        String name = Prefs.getName(this);
        tvGreeting.setText(name.isEmpty() ? hello : hello + ", " + name);
    }

    private void updateSubtitle(List<Task> tasks) {
        int total = tasks == null ? 0 : tasks.size();
        int done = 0;
        if (tasks != null) {
            for (Task t : tasks) if (t.done) done++;
        }
        int left = total - done;
        if (total == 0) {
            tvSubtitle.setText(R.string.subtitle_none);
        } else if (left == 0) {
            tvSubtitle.setText(R.string.subtitle_all_done);
        } else {
            tvSubtitle.setText(getResources().getQuantityString(R.plurals.subtitle_left, left, left));
        }
    }

    private void showNameDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.hint_name);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        input.setText(Prefs.getName(this));
        input.setSelection(input.getText().length());

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout box = new FrameLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.ask_name_title)
                .setView(box)
                .setPositiveButton(R.string.save, (d, w) -> {
                    Prefs.setName(this, input.getText().toString());
                    updateGreeting();
                })
                .setNegativeButton(R.string.skip, null)
                .show();
    }

    // ------------------------------------------------- reminder + voice settings

    /** Choose how long after the first reminder the second one comes (or turn it off). */
    private void showSecondReminderDialog() {
        int current = Prefs.isSecondReminderOn(this) ? Prefs.getSecondReminderGapMin(this) : 0;
        int checked = -1;
        String[] names = new String[SECOND_GAPS.length];
        for (int i = 0; i < SECOND_GAPS.length; i++) {
            int m = SECOND_GAPS[i];
            if (m == 0) {
                names[i] = getString(R.string.second_off);
            } else if (m == Prefs.DEFAULT_SECOND_GAP_MIN) {
                names[i] = getString(R.string.second_after_default, m);
            } else {
                names[i] = getString(R.string.second_after, m);
            }
            if (m == current) checked = i;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_second_reminder)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    int m = SECOND_GAPS[which];
                    if (m == 0) {
                        Prefs.setSecondReminderOn(this, false);
                        Toast.makeText(this, R.string.second_set_off, Toast.LENGTH_SHORT).show();
                    } else {
                        Prefs.setSecondReminderOn(this, true);
                        Prefs.setSecondReminderGapMin(this, m);
                        Toast.makeText(this, getString(R.string.second_set_on, m), Toast.LENGTH_SHORT).show();
                    }
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showSpokenDialog() {
        String[] names = {getString(R.string.spoken_on), getString(R.string.spoken_off)};
        int checked = Prefs.isSpokenOn(this) ? 0 : 1;
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_spoken)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    Prefs.setSpokenOn(this, which == 0);
                    d.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------------------------------------------------------------- theme

    private void showThemeModeDialog() {
        int current = Prefs.getNightMode(this);
        int checked = 0;
        for (int i = 0; i < NIGHT_MODES.length; i++) {
            if (NIGHT_MODES[i] == current) checked = i;
        }
        String[] names = {
                getString(R.string.mode_system), getString(R.string.mode_light), getString(R.string.mode_dark)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_theme_mode)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    Prefs.setNightMode(this, NIGHT_MODES[which]);
                    d.dismiss();
                    AppCompatDelegate.setDefaultNightMode(NIGHT_MODES[which]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAccentDialog() {
        String[] names = {
                getString(R.string.accent_purple), getString(R.string.accent_teal),
                getString(R.string.accent_blue), getString(R.string.accent_orange),
                getString(R.string.accent_pink)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_accent)
                .setSingleChoiceItems(names, Prefs.getAccent(this), (d, which) -> {
                    Prefs.setAccent(this, which);
                    d.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ------------------------------------------------------- backup/restore

    private void doExport(final Uri uri) {
        final Context app = getApplicationContext();
        AppDatabase.IO.execute(() -> {
            try {
                List<Task> all = db.taskDao().getAll();
                List<Memory> memories = db.memoryDao().getAll();
                String json = BackupManager.export(app, all, memories);
                try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
                    if (os == null) throw new IOException("No output stream");
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                toast(getString(R.string.backup_done_mem, all.size(), memories.size()));
            } catch (Exception e) {
                toast(getString(R.string.backup_failed));
            }
        });
    }

    /** Restores a Cortex backup, or a backup made by the old Task Reminder app. */
    private void doImport(final Uri uri) {
        final Context app = getApplicationContext();
        AppDatabase.IO.execute(() -> {
            try {
                String json = readAll(uri);
                final BackupManager.Result r = BackupManager.importJson(app, db, json);
                runOnUiThread(() -> applyRestoredSettings(r));
                toast(getString(R.string.restore_done_mem, r.added, r.skipped, r.memAdded, r.memSkipped));
            } catch (JSONException e) {
                toast(getString(R.string.restore_bad_file));
            } catch (Exception e) {
                toast(getString(R.string.restore_failed));
            }
        });
    }

    private String readAll(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("No input stream");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > 5 * 1024 * 1024) throw new IOException("File too large");
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void applyRestoredSettings(BackupManager.Result r) {
        boolean recreateNeeded = false;
        if (r.userName != null) {
            Prefs.setName(this, r.userName);
            updateGreeting();
        }
        if (r.accent != null && r.accent != Prefs.getAccent(this)) {
            Prefs.setAccent(this, r.accent);
            recreateNeeded = true;
        }
        if (r.nightMode != null && r.nightMode != Prefs.getNightMode(this)) {
            Prefs.setNightMode(this, r.nightMode);
            AppCompatDelegate.setDefaultNightMode(r.nightMode); // recreates the activity itself
            recreateNeeded = false;
        }
        if (recreateNeeded) recreate();
    }

    private void toast(final String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    // ----------------------------------------------------------- permissions

    private void askForPermissions() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                } catch (Exception ignored) {
                    // Reminders still work, just not to-the-minute.
                }
            }
        }
    }
}
