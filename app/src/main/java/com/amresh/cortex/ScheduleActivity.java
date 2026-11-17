package com.amresh.cortex;

import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * The 7-day schedule. Opens on today; swipe left/right or tap a day tab to jump.
 * Opened from the floating button and from the menu.
 */
public class ScheduleActivity extends AppCompatActivity {

    private long todayStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        setTitle(R.string.schedule_title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        todayStart = ScheduleManager.startOfDay(System.currentTimeMillis());

        TabLayout tabs = findViewById(R.id.scheduleTabs);
        ViewPager2 pager = findViewById(R.id.schedulePager);
        pager.setAdapter(new FragmentStateAdapter((FragmentActivity) this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return ScheduleDayFragment.newInstance(ScheduleManager.addDays(todayStart, position));
            }

            @Override
            public int getItemCount() {
                return ScheduleManager.DAYS;
            }
        });

        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.schedule_today);
            } else if (position == 1) {
                tab.setText(R.string.schedule_tomorrow);
            } else {
                tab.setText(DateFormat.format("EEE d", ScheduleManager.addDays(todayStart, position)));
            }
        }).attach();

        // Opens on today (position 0).
        pager.setCurrentItem(0, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If the app stayed open past midnight, rebuild so "today" is correct again.
        if (ScheduleManager.startOfDay(System.currentTimeMillis()) != todayStart) {
            recreate();
            return;
        }
        // Pull in new tasks, drop removed ones and move past days to the bin.
        final android.content.Context app = getApplicationContext();
        AppDatabase.IO.execute(() -> ScheduleManager.sync(app));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
