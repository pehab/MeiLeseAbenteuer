package de.meiapps.leseabenteuer;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.WebView;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Runs the bundled chapter in Android's real WebView, without network connectivity. */
public final class SmokeInstrumentation extends Instrumentation {
    private Bundle args;
    private MainActivity activity;
    private WebView web;
    private int checks;
    @Override public void onCreate(Bundle args) { this.args = args; start(); }
    private void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private String js(String script) throws Exception {
        String[] result = new String[1]; CountDownLatch latch = new CountDownLatch(1);
        runOnMainSync(() -> web.evaluateJavascript(script, value -> { result[0] = value; latch.countDown(); }));
        if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("WebView callback timeout");
        return result[0];
    }
    private void await(String expression) throws Exception {
        for (int i = 0; i < 120; i++) { if ("true".equals(js("Boolean(" + expression + ")"))) return; Thread.sleep(100); }
        throw new AssertionError("Timed out: " + expression + "\n" + js("document.body.innerText"));
    }
    private void click(String selector) throws Exception {
        await("document.querySelector(" + org.json.JSONObject.quote(selector) + ") && !document.querySelector(" + org.json.JSONObject.quote(selector) + ").disabled");
        js("document.querySelector(" + org.json.JSONObject.quote(selector) + ").click()");
        Thread.sleep(100);
    }
    private void screenshot(String name) throws Exception {
        getUiAutomation().waitForIdle(300, 3000);
        Bitmap bitmap = getUiAutomation().takeScreenshot(); check(bitmap != null, "screenshot available");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes); bitmap.recycle();
        Bundle image = new Bundle(); image.putString("image_" + name, android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP)); sendStatus(0, image);
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            SharedPreferences legacy = getTargetContext().getSharedPreferences("learning-v1", 0);
            if ("seed".equals(args.getString("phase"))) {
                check(legacy.edit().putString("p0.name", "Upgrade-Test").putInt("p0.stars", 7).commit(), "seed legacy preferences");
                result.putString("summary", "PASS: legacy update fixture seeded"); finish(Activity.RESULT_OK, result); return;
            }
            activity = (MainActivity) startActivitySync(new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            java.lang.reflect.Field field = MainActivity.class.getDeclaredField("web"); field.setAccessible(true); web = (WebView) field.get(activity);
            await("document.getElementById('game') && document.getElementById('game').dataset.ready === 'true'");
            if ("upgrade".equals(args.getString("phase"))) {
                check("Upgrade-Test".equals(legacy.getString("p0.name", "")) && legacy.getInt("p0.stars", 0) == 7, "old progress remains unmodified");
                check("\"meeting\"".equals(js("document.getElementById('game').dataset.phase")), "new chapter starts independently from old rewards");
                result.putString("summary", "PASS: " + checks + " update assertions"); finish(Activity.RESULT_OK, result); return;
            }
            if ("reopen".equals(args.getString("phase"))) {
                check("\"complete\"".equals(js("document.getElementById('game').dataset.phase")), "chapter survives process restart");
                click("#begin"); await("!document.getElementById('ending').hidden");
                check("true".equals(js("JSON.parse(ForestHost.load()).helped.river")), "help survives process restart");
                result.putString("summary", "PASS: " + checks + " restart assertions"); finish(Activity.RESULT_OK, result); return;
            }
            screenshot("forest-native-intro.png");
            click("#begin"); await("!document.getElementById('dialogue').hidden"); click("#dialogue-next");
            check("\"river\"".equals(js("document.getElementById('game').dataset.phase")), "accepting the post starts journey");
            click("[data-secret='0']"); screenshot("forest-native-path.png");
            click("#next-place"); await("!document.getElementById('dialogue').hidden"); click("#dialogue-next");
            await("document.getElementById('game').dataset.puzzle === 'river'");
            click("[data-choice='lamp']"); check("\"river\"".equals(js("document.getElementById('game').dataset.phase")), "wrong item cannot cross river");
            click("#puzzle-repeat"); check("false".equals(js("JSON.parse(ForestHost.load()).helped.river")), "repeat does not reveal answer");
            screenshot("forest-native-river.png"); click("#help"); click("[data-choice='lemonade']");
            check("\"treehouse\"".equals(js("document.getElementById('game').dataset.phase")), "correct word unlocks crossing");
            click("#success-next"); await("!document.getElementById('dialogue').hidden"); click("#dialogue-next");
            await("document.getElementById('game').dataset.puzzle === 'house'"); screenshot("forest-native-house.png");
            for (String letter : new String[]{"M", "O", "A"}) click("[data-letter='" + letter + "']");
            check("\"treehouse\"".equals(js("document.getElementById('game').dataset.phase")), "wrong letter order cannot finish story");
            for (int i = 0; i < 3; i++) click("#undo-letter");
            for (String letter : new String[]{"O", "M", "A"}) click("[data-letter='" + letter + "']");
            check("\"complete\"".equals(js("document.getElementById('game').dataset.phase")), "Oma receives post");
            click("#success-next"); await("!document.getElementById('ending').hidden"); screenshot("forest-native-ending.png");
            check(getTargetContext().getSharedPreferences("forest-story-v1", 0).edit().commit(), "flush chapter before process death");
            result.putString("summary", "PASS: " + checks + " offline Android chapter assertions"); finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            try { if (web != null) screenshot("forest-failure.png"); } catch (Exception ignored) { }
            result.putString("error", android.util.Log.getStackTraceString(error)); finish(Activity.RESULT_CANCELED, result);
        }
    }
}
