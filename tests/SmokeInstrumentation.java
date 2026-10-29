package de.meiapps.leseabenteuer;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.WebView;
import android.view.MotionEvent;
import android.os.SystemClock;
import org.json.JSONArray;
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
        await("document.querySelector(" + org.json.JSONObject.quote(selector) + ") && !document.querySelector(" + org.json.JSONObject.quote(selector) + ").disabled && document.querySelector(" + org.json.JSONObject.quote(selector) + ").getClientRects().length > 0");
        js("document.querySelector(" + org.json.JSONObject.quote(selector) + ").click()");
        Thread.sleep(100);
    }
    private void traceLetter(boolean lower) throws Exception {
        String form = lower ? "lowerStrokes" : "upperStrokes";
        String script = "(() => {const b=document.getElementById('trace-board'); const r=" + web.getWidth() + "/document.documentElement.clientWidth; const item=ForestLetters.letters[Number(document.getElementById('lesson').dataset.index)];return item." + form + ".map(s=>ForestLetters.sample(s,8).map(v=>{const p=b.createSVGPoint();p.x=v[0]+18;p.y=v[1]+17;const q=p.matrixTransform(b.getScreenCTM());return [q.x*r,q.y*r];}));})()";
        JSONArray strokes = new JSONArray(js(script));
        int[] location = new int[2]; runOnMainSync(() -> web.getLocationOnScreen(location));
        for (int s = 0; s < strokes.length(); s++) {
            JSONArray points = strokes.getJSONArray(s); long down = SystemClock.uptimeMillis();
            for (int i = 0; i < points.length(); i++) {
                JSONArray point = points.getJSONArray(i);
                MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), i == 0 ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_MOVE,
                        (float) point.getDouble(0) + location[0], (float) point.getDouble(1) + location[1], 0);
                sendPointerSync(event); event.recycle(); Thread.sleep(4);
            }
            JSONArray point = points.getJSONArray(points.length() - 1);
            MotionEvent up = MotionEvent.obtain(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP,
                    (float) point.getDouble(0) + location[0], (float) point.getDouble(1) + location[1], 0);
            sendPointerSync(up); up.recycle(); Thread.sleep(80);
        }
        check("true".equals(js("!document.getElementById('lesson-next').disabled")), "imperfect Android touch completes " + form);
    }
    private void learnLetters() throws Exception {
        click("#begin"); await("!document.getElementById('lesson').hidden");
        check("\"meeting\"".equals(js("document.getElementById('game').dataset.phase")), "word story stays locked during letter learning");
        click("#lesson-path"); check("true".equals(js("document.querySelector('[data-chapter=\"1\"]').disabled")), "later letter chapters are locked");
        click("[data-chapter='0']");
        String[] upper = {"A", "E", "I", "O", "U", "M", "N", "L"};
        for (int i = 0; i < upper.length; i++) {
            check(Integer.toString(i).equals(js("Number(document.getElementById('lesson').dataset.index)")), "correct letter chapter");
            click("#letter-sound"); click("#lesson-next");
            if (i == 0) {
                js("document.querySelector('#letter-options button:not([data-answer=A])').click()");
                check("\"find\"".equals(js("document.getElementById('lesson').dataset.step")), "wrong letter cannot advance");
                screenshot("letters-native-find.jpg");
            }
            click("[data-answer='" + upper[i] + "']");
            click("[data-answer='" + upper[i].toLowerCase(java.util.Locale.ROOT) + "']");
            check("true".equals(js("document.getElementById('lesson-next').disabled")), "writing requires actual finger input");
            if (i == 0) screenshot("letters-native-writing.jpg");
            traceLetter(false); click("#lesson-next"); traceLetter(true); click("#lesson-next");
            check(Integer.toString(i + 1).equals(js("JSON.parse(ForestHost.loadLetters()).completed")), "completed letter persisted");
            click("#lesson-next");
        }
        check("8".equals(js("JSON.parse(ForestHost.loadLetters()).completed")), "all letters precede words");
    }
    private void learnLanterns() throws Exception {
        click("#start-lanterns"); await("!document.getElementById('lantern').hidden");
        check("0".equals(js("new ForestLanterns.Lanterns(JSON.parse(ForestHost.loadLanterns())).completed")), "new chapter starts independently");
        for (int i = 0; i < 9; i++) {
            check(Integer.toString(i).equals(js("Number(document.getElementById('lantern').dataset.round)")), "lantern round in order");
            if (i < 6) {
                js("(() => { const r=ForestLanterns.rounds[" + i + "];document.querySelector('#lantern-options [data-sound=\"'+r.choices.find(v=>v!==r.answer)+'\"]').click(); })()");
                check(Integer.toString(i).equals(js("new ForestLanterns.Lanterns(JSON.parse(ForestHost.loadLanterns())).completed")), "wrong syllable never lights a lantern");
                click("#lantern-help");
                js("(() => { const r=ForestLanterns.rounds[" + i + "];document.querySelector('#lantern-options [data-sound=\"'+r.answer+'\"]').click(); })()");
            } else {
                if (i == 6) screenshot("lanterns-native-word.jpg");
                js("(() => {const r=ForestLanterns.rounds[" + i + "];for(const piece of r.solution)document.querySelector('#lantern-options [data-sound=\"'+piece+'\"]:enabled').click();})()");
            }
            check(Integer.toString(i + 1).equals(js("JSON.parse(ForestHost.loadLanterns()).completed")), "lantern persisted before reward Next");
            if (i == 0) {
                js("ForestHost.saveLanterns('{\"version\":1,\"completed\":99}')");
                check("1".equals(js("JSON.parse(ForestHost.loadLanterns()).completed")), "invalid native save preserves last valid progress");
                check("true".equals(js("window.forestBack()")), "Android Back closes lantern reward safely");
                click("#start-lanterns");
            } else click("#lantern-next");
        }
        await("!document.getElementById('lantern-finale').hidden"); screenshot("lanterns-native-complete.jpg");
        click("#lantern-finish");
        check("8".equals(js("JSON.parse(ForestHost.loadLetters()).completed")), "lanterns preserve learned letters");
        check("\"complete\"".equals(js("document.getElementById('game').dataset.phase")), "lanterns preserve completed post");
    }
    private void screenshot(String name) throws Exception {
        CountDownLatch painted = new CountDownLatch(1);
        runOnMainSync(() -> web.postVisualStateCallback(System.nanoTime(), new WebView.VisualStateCallback() {
            @Override public void onComplete(long requestId) { painted.countDown(); }
        }));
        check(painted.await(5, TimeUnit.SECONDS), "WebView painted the current DOM");
        Thread.sleep(250);
        getUiAutomation().waitForIdle(300, 3000);
        Bitmap bitmap = getUiAutomation().takeScreenshot(); check(bitmap != null, "screenshot available");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, 86, bytes); bitmap.recycle();
        Bundle image = new Bundle(); image.putString("image_" + name, android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP)); sendStatus(0, image);
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            SharedPreferences legacy = getTargetContext().getSharedPreferences("learning-v1", 0);
            if ("seed".equals(args.getString("phase"))) {
                check(legacy.edit().putString("p0.name", "Upgrade-Test").putInt("p0.stars", 7).commit(), "seed legacy preferences");
                check(getTargetContext().getSharedPreferences("forest-story-v1", 0).edit().putString("chapter", "{\"version\":1,\"phase\":\"complete\",\"found\":[0,1,2]}").commit(), "seed the finished 0.4 prototype independently");
                result.putString("summary", "PASS: legacy update fixture seeded"); finish(Activity.RESULT_OK, result); return;
            }
            activity = (MainActivity) startActivitySync(new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            java.lang.reflect.Field field = MainActivity.class.getDeclaredField("web"); field.setAccessible(true); web = (WebView) field.get(activity);
            await("document.getElementById('game') && document.getElementById('game').dataset.ready === 'true'");
            check("true".equals(js("document.getElementById('viewport').getBoundingClientRect().width > 100 && document.getElementById('viewport').getBoundingClientRect().height > 100 && document.querySelector('.landscape').naturalWidth > 100")), "the illustrated world has a visible nonzero viewport");
            if ("upgrade".equals(args.getString("phase"))) {
                check("Upgrade-Test".equals(legacy.getString("p0.name", "")) && legacy.getInt("p0.stars", 0) == 7, "old progress remains unmodified");
                check("\"meeting\"".equals(js("document.getElementById('game').dataset.phase")), "new chapter starts independently from old rewards");
                check(getTargetContext().getSharedPreferences("forest-story-v1", 0).getString("chapter", "").contains("complete"), "the earlier prototype save stays preserved");
                check("0".equals(js("new ForestLetters.Lessons(JSON.parse(ForestHost.loadLetters())).completed")), "previous prototype cannot skip the letter introduction");
                result.putString("summary", "PASS: " + checks + " update assertions"); finish(Activity.RESULT_OK, result); return;
            }
            if ("reopen".equals(args.getString("phase"))) {
                check("\"complete\"".equals(js("document.getElementById('game').dataset.phase")), "chapter survives process restart");
                check("8".equals(js("JSON.parse(ForestHost.loadLetters()).completed")), "letter chapters survive process restart");
                check("9".equals(js("JSON.parse(ForestHost.loadLanterns()).completed")), "lantern chapter survives process restart");
                click("#begin"); await("!document.getElementById('ending').hidden");
                check("true".equals(js("JSON.parse(ForestHost.load()).helped.river")), "help survives process restart");
                result.putString("summary", "PASS: " + checks + " restart assertions"); finish(Activity.RESULT_OK, result); return;
            }
            screenshot("forest-native-intro.jpg");
            learnLetters(); await("!document.getElementById('dialogue').hidden"); click("#dialogue-next");
            check("\"river\"".equals(js("document.getElementById('game').dataset.phase")), "accepting the post starts journey");
            click("[data-secret='0']"); screenshot("forest-native-path.jpg");
            click("#next-place"); await("!document.getElementById('dialogue').hidden"); click("#dialogue-next");
            await("document.getElementById('game').dataset.puzzle === 'river'");
            click("[data-choice='lamp']"); check("\"river\"".equals(js("document.getElementById('game').dataset.phase")), "wrong item cannot cross river");
            click("#puzzle-repeat"); check("false".equals(js("JSON.parse(ForestHost.load()).helped.river")), "repeat does not reveal answer");
            screenshot("forest-native-river.jpg"); click("#help"); click("[data-choice='lemonade']");
            check("\"treehouse\"".equals(js("document.getElementById('game').dataset.phase")), "correct word unlocks crossing");
            click("#success-next"); await("!document.getElementById('dialogue').hidden"); click("#dialogue-next");
            await("document.getElementById('game').dataset.puzzle === 'house'"); screenshot("forest-native-house.jpg");
            for (String letter : new String[]{"M", "O", "A"}) click("[data-letter='" + letter + "']");
            check("\"treehouse\"".equals(js("document.getElementById('game').dataset.phase")), "wrong letter order cannot finish story");
            for (int i = 0; i < 3; i++) click("#undo-letter");
            for (String letter : new String[]{"O", "M", "A"}) click("[data-letter='" + letter + "']");
            check("\"complete\"".equals(js("document.getElementById('game').dataset.phase")), "Oma receives post");
            click("#success-next"); await("!document.getElementById('ending').hidden"); screenshot("forest-native-ending.jpg");
            learnLanterns();
            check(getTargetContext().getSharedPreferences("forest-story-v1", 0).edit().commit(), "flush chapter before process death");
            result.putString("summary", "PASS: " + checks + " offline Android chapter assertions"); finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            try { if (web != null) screenshot("forest-failure.jpg"); } catch (Exception ignored) { }
            result.putString("error", android.util.Log.getStackTraceString(error)); finish(Activity.RESULT_CANCELED, result);
        }
    }
}
