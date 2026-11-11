package com.amresh.cortex;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Invisible screen opened by the Quick Settings tile. It shows only the system voice
 * prompt on top of whatever app you are in, hands the result to CommandHandler
 * (which decides: task, memory or assistance), shows and speaks the answer, and closes itself.
 */
public class VoiceCommandActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                String spoken = null;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> texts =
                            result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (texts != null && !texts.isEmpty()) spoken = texts.get(0);
                }
                if (spoken != null && !spoken.trim().isEmpty()) {
                    CommandHandler.Result r = CommandHandler.handle(this, spoken.trim());
                    // Application context, so the message stays after this screen closes.
                    Toast.makeText(getApplicationContext(), r.message, Toast.LENGTH_LONG).show();
                    Speaker.speak(getApplicationContext(), r.message);
                }
                finish();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Only start listening on a fresh launch, not when Android recreates this screen.
        if (savedInstanceState == null) startListening();
    }

    private void startListening() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_prompt_cortex));
        try {
            speechLauncher.launch(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_speech, Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
