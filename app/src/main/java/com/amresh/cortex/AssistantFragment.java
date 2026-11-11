package com.amresh.cortex;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Middle tab: talk or type to Cortex. Every request goes through CommandHandler, so
 * "remind me ..." saves a task, "remember ..." saves a memory, and anything else is an
 * Assistance request (open an app, go home). Replies are shown, and spoken when the
 * request was made by voice.
 */
public class AssistantFragment extends Fragment {

    /** Conversation shown on screen. Kept while the app process lives. */
    private static final StringBuilder LOG = new StringBuilder();

    private EditText etInput;
    private TextView tvHint, tvLog;
    private ScrollView scroll;

    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> texts =
                            result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (texts != null && !texts.isEmpty() && !texts.get(0).trim().isEmpty()) {
                        submit(texts.get(0).trim(), true);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etInput = view.findViewById(R.id.etAssistant);
        tvHint = view.findViewById(R.id.tvAssistantHint);
        tvLog = view.findViewById(R.id.tvAssistantLog);
        scroll = view.findViewById(R.id.scrollAssistant);
        Button btnSend = view.findViewById(R.id.btnAssistantSend);
        Button btnMic = view.findViewById(R.id.btnAssistantMic);

        btnSend.setOnClickListener(v -> sendTyped());
        btnMic.setOnClickListener(v -> startVoiceInput());
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                sendTyped();
                return true;
            }
            return false;
        });

        showLog();
    }

    private void sendTyped() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        etInput.setText("");
        submit(text, false);
    }

    /** Runs the request and shows (and, for voice requests, speaks) the answer. */
    private void submit(String text, boolean viaVoice) {
        append(getString(R.string.assistant_you), text);
        CommandHandler.Result r = CommandHandler.handle(requireContext(), text);
        append(getString(R.string.assistant_cortex), r.message);
        if (viaVoice) Speaker.speak(requireContext().getApplicationContext(), r.message);
    }

    private void startVoiceInput() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_prompt_cortex));
        try {
            speechLauncher.launch(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.no_speech, Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------ log

    private void append(String who, String message) {
        if (LOG.length() > 8000) LOG.delete(0, LOG.length() - 4000);
        LOG.append(who).append(": ").append(message).append("\n\n");
        showLog();
    }

    private void showLog() {
        if (tvLog == null || tvHint == null || scroll == null) return;
        if (LOG.length() == 0) {
            tvHint.setVisibility(View.VISIBLE);
            tvLog.setVisibility(View.GONE);
            return;
        }
        tvHint.setVisibility(View.GONE);
        tvLog.setVisibility(View.VISIBLE);
        tvLog.setText(LOG.toString().trim());
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }
}
