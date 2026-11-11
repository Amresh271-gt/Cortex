package com.amresh.cortex;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Shows deleted tasks as a record. Items older than 15 days are removed automatically. */
public class BinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bin);
        setTitle(R.string.bin_title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        final AppDatabase db = AppDatabase.get(this);
        AppDatabase.IO.execute(() ->
                db.taskDao().purgeBinOlderThan(System.currentTimeMillis() - AppDatabase.BIN_RETENTION_MILLIS));

        final TextView empty = findViewById(R.id.tvBinEmpty);
        RecyclerView rv = findViewById(R.id.rvBin);
        rv.setLayoutManager(new LinearLayoutManager(this));
        final BinAdapter adapter = new BinAdapter();
        rv.setAdapter(adapter);

        db.taskDao().observeBin().observe(this, tasks -> {
            adapter.setTasks(tasks);
            empty.setVisibility(tasks == null || tasks.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
