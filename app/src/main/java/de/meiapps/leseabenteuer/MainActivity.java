package de.meiapps.leseabenteuer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;
import de.meiapps.updates.GitHubUpdates;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Offline Android shell for the new illustrated chapter. No legacy learning screens are used. */
public final class MainActivity extends Activity {
    static final String ORIGIN = "https://appassets.androidplatform.net/";
    private WebView web;
    private SharedPreferences preferences;
    private TextToSpeech speech;
    private GitHubUpdates updates;
    private boolean voiceReady, destroyed, paused, inParents;
    private String pendingSpeech;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        preferences = getSharedPreferences("forest-story-v1", MODE_PRIVATE);
        updates = GitHubUpdates.attach(this, "pehab/MeiLeseAbenteuer", this::stopSpeech);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(32,59,48));
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setBlockNetworkLoads(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(true);
        web.addJavascriptInterface(new ForestBridge(), "ForestHost");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getPath();
                if ("https".equals(uri.getScheme()) && "appassets.androidplatform.net".equals(uri.getHost())
                        && path != null && path.startsWith("/forest/") && !path.contains("..") && !path.contains("\\")) {
                    String mime = path.endsWith(".html") ? "text/html" : path.endsWith(".css") ? "text/css" : path.endsWith(".js") ? "application/javascript" : path.endsWith(".webp") ? "image/webp" : null;
                    if (mime != null) try { return new WebResourceResponse(mime, "UTF-8", getAssets().open(path.substring(1))); }
                    catch (IOException ignored) { /* Missing bundled asset is reported by the game's loading screen. */ }
                }
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", java.util.Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
            }
        });
        setContentView(web);
        speech = new TextToSpeech(this, status -> runOnUiThread(() -> configureSpeech(status)));
        web.loadUrl(ORIGIN + "forest/index.html");
        if (android.os.Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);
        immersive();
    }
    private void immersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else legacyImmersive();
    }
    @SuppressWarnings("deprecation")
    private void legacyImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if (focus) immersive(); }
    private void configureSpeech(int status) {
        if (destroyed || speech == null || status != TextToSpeech.SUCCESS) return;
        voiceReady = false;
        Set<Voice> voices = speech.getVoices();
        Voice best = null;
        if (voices != null) for (Voice voice : voices) {
            if ("de".equals(voice.getLocale().getLanguage()) && !voice.isNetworkConnectionRequired()
                    && (voice.getFeatures() == null || !voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
                    && (best == null || voice.getQuality() > best.getQuality())) best = voice;
        }
        if (best != null) voiceReady = speech.setVoice(best) == TextToSpeech.SUCCESS;
        speech.setSpeechRate(.84f);
        speech.setPitch(1.02f);
        if (voiceReady && pendingSpeech != null && !paused) { String text = pendingSpeech; pendingSpeech = null; speak(text); }
    }
    private void speak(String text) {
        if (destroyed || paused || preferences.getBoolean("muted", false)) return;
        if (!voiceReady) { pendingSpeech = text; return; }
        speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "forest-story");
    }
    private void stopSpeech() { pendingSpeech = null; if (speech != null) speech.stop(); }
    private void script(String code) { if (!destroyed && web != null) web.evaluateJavascript(code, null); }
    private void back() {
        web.evaluateJavascript("window.forestBack ? window.forestBack() : false", handled -> {
            if (!"true".equals(handled) && !destroyed) new AlertDialog.Builder(this).setTitle("Den Wald verlassen?")
                    .setMessage("Dein Weg bleibt gespeichert.").setNegativeButton("Weiterspielen", null)
                    .setPositiveButton("Schließen", (dialog, which) -> finish()).show();
        });
    }
    // API 33+ uses the registered native OnBackInvokedCallback; this is only the API 26–32 fallback.
    @SuppressLint("GestureBackNavigation")
    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() { back(); }
    @Override protected void onPause() {
        paused = true; stopSpeech(); script("window.forestPaused && window.forestPaused(true)");
        web.onPause(); web.pauseTimers(); updates.pause(); super.onPause();
    }
    @Override protected void onResume() {
        super.onResume(); paused = false;
        if (web != null) { web.resumeTimers(); web.onResume(); script("window.forestPaused && window.forestPaused(false)"); }
        if (inParents) updates.resume();
        if (speech != null) configureSpeech(TextToSpeech.SUCCESS);
    }
    @Override protected void onDestroy() {
        destroyed = true; stopSpeech(); if (speech != null) speech.shutdown(); updates.destroy();
        web.removeJavascriptInterface("ForestHost"); web.destroy(); super.onDestroy();
    }
    private void parentGate() {
        stopSpeech();
        EditText answer = new EditText(this); answer.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); answer.setHint("Antwort");
        new AlertDialog.Builder(this).setTitle("Für Große: Was ist 7 + 5?").setView(answer)
                .setNegativeButton("Zurück", null).setPositiveButton("Öffnen", (d, n) -> {
                    if ("12".equals(answer.getText().toString().trim())) parentSettings();
                    else Toast.makeText(this, "Das war noch nicht die richtige Antwort.", Toast.LENGTH_SHORT).show();
                }).show();
    }
    private void parentSettings() {
        inParents = true; updates.resume();
        AlertDialog menu = new AlertDialog.Builder(this).setTitle("MeiLeseAbenteuer · Für Große")
                .setItems(new String[]{"Erster Waldabschnitt · erste Wörter", voiceReady ? "Deutsche Offline-Stimme testen" : "Deutsche Offline-Stimme einrichten", "Updates", "Diesen Waldabschnitt neu beginnen"}, null)
                .setNegativeButton("Zurück in den Wald", null).create();
        menu.setOnDismissListener(d -> { inParents = false; updates.pause(); });
        menu.show();
        // Keep the gated menu open underneath subdialogs and the update/install flow.
        menu.getListView().setOnItemClickListener((parent, row, n, id) -> {
                    if (n == 0) new AlertDialog.Builder(this).setTitle("Die erste Sternenpost")
                            .setMessage("Dieser erste spielbare Abschnitt erprobt die neue Spielwelt. Er enthält die Wörter LIMO und OMA sowie drei versteckte Glühwürmchen. Eine vollständige Einführung der Buchstaben folgt später. Hilf mir spricht das gesuchte Wort vor. Dafür ist eine installierte deutsche Offline-Stimme nötig. Ohne Stimme bitte gemeinsam spielen. Alte Lernstände werden nicht in die neue Geschichte übernommen und bleiben unberührt.")
                            .setPositiveButton("Verstanden", null).show();
                    else if (n == 1) {
                        if (voiceReady) speak("Willkommen im Geschichtenwald. Nila möchte die Sternenpost zum Baumhaus bringen.");
                        else try { startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)); }
                        catch (android.content.ActivityNotFoundException e) { Toast.makeText(this, "Bitte eine deutsche Offline-Stimme in den Android-Spracheinstellungen installieren.", Toast.LENGTH_LONG).show(); }
                    } else if (n == 2) updates.settings();
                    else new AlertDialog.Builder(this).setTitle("Waldabschnitt neu beginnen?").setMessage("Die Sternenpost und die drei Glühwürmchen beginnen von vorn.")
                            .setNegativeButton("Abbrechen", null).setPositiveButton("Neu beginnen", (x, y) -> { preferences.edit().remove("chapter").apply(); web.reload(); }).show();
                });
    }
    final class ForestBridge {
        @JavascriptInterface public String load() { return preferences.getString("chapter", "{}"); }
        @JavascriptInterface public void save(String value) {
            if (value == null || value.length() > 4096) return;
            try {
                JSONObject input = new JSONObject(value);
                String phase = input.getString("phase");
                if (input.optInt("version") != 1 || !Arrays.asList("meeting", "river", "treehouse", "complete").contains(phase)) return;
                JSONArray found = input.optJSONArray("found"); JSONArray clean = new JSONArray();
                boolean[] included = new boolean[3];
                if (found != null) for (int i = 0; i < found.length(); i++) { int id = found.optInt(i, -1); if (id >= 0 && id < 3 && !included[id]) { clean.put(id); included[id] = true; } }
                JSONObject sourceHelp = input.optJSONObject("helped");
                JSONObject help = new JSONObject().put("river", sourceHelp != null && sourceHelp.optBoolean("river")).put("treehouse", sourceHelp != null && sourceHelp.optBoolean("treehouse"));
                String normalized = new JSONObject().put("version", 1).put("phase", phase).put("found", clean).put("helped", help).toString();
                preferences.edit().putString("chapter", normalized).apply();
            } catch (JSONException ignored) { /* An invalid document never replaces a valid save. */ }
        }
        @JavascriptInterface public boolean isMuted() { return preferences.getBoolean("muted", false); }
        @JavascriptInterface public void setMuted(boolean value) { preferences.edit().putBoolean("muted", value).apply(); if (value) runOnUiThread(MainActivity.this::stopSpeech); }
        @JavascriptInterface public void speak(String text) { if (text != null && text.length() <= 1500) runOnUiThread(() -> MainActivity.this.speak(text)); }
        @JavascriptInterface public void stopSpeech() { runOnUiThread(MainActivity.this::stopSpeech); }
        @JavascriptInterface public void openParents() { runOnUiThread(MainActivity.this::parentGate); }
    }
}
