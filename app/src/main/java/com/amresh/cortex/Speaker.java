package com.amresh.cortex;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Speaks Cortex's replies out loud (when "Spoken replies" is on in the menu).
 * Uses the application context, so it keeps talking after the invisible voice screen closes.
 */
public class Speaker {

    private static TextToSpeech tts;
    private static boolean ready = false;
    private static String pending = null;

    public static synchronized void speak(Context ctx, String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!Prefs.isSpokenOn(ctx)) return;

        if (tts != null && ready) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cortex_reply");
            return;
        }

        // Engine is still starting (or not created yet): say it as soon as it is ready.
        pending = text;
        if (tts == null) {
            tts = new TextToSpeech(ctx.getApplicationContext(), status -> {
                synchronized (Speaker.class) {
                    if (status == TextToSpeech.SUCCESS && tts != null) {
                        tts.setLanguage(Locale.getDefault());
                        ready = true;
                        if (pending != null) {
                            tts.speak(pending, TextToSpeech.QUEUE_FLUSH, null, "cortex_reply");
                            pending = null;
                        }
                    } else {
                        // No speech engine on this phone: stay silent, the text is still shown.
                        tts = null;
                        ready = false;
                        pending = null;
                    }
                }
            });
        }
    }
}
