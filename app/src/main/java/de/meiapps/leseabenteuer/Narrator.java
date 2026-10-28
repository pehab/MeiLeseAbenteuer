package de.meiapps.leseabenteuer;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.*;
import java.util.*;

/** Bundled speech sounds never go through letter-name TTS. Narration uses an installed offline German voice. */
final class Narrator {
    private final Activity activity;private final Handler main=new Handler(Looper.getMainLooper());
    private TextToSpeech tts;private MediaPlayer player;private boolean ready,closed,muted,initialized;private long generation;
    private Runnable after;private String utterance;private Runnable changed=()->{};
    Narrator(Activity activity){this.activity=activity;tts=new TextToSpeech(activity,status->main.post(()->configure(status)));}
    void changed(Runnable task){changed=task;}
    boolean ready(){return ready;}
    void muted(boolean value){muted=value;stop();}
    boolean muted(){return muted;}
    void refresh(){if(initialized&&!closed)configure(TextToSpeech.SUCCESS);}
    private void configure(int status){
        initialized=status==TextToSpeech.SUCCESS;
        if(closed||status!=TextToSpeech.SUCCESS){changed.run();return;}
        boolean wasReady=ready;ready=false;Set<Voice> voices=tts.getVoices();Voice best=null;
        if(voices!=null)for(Voice v:voices)if(v.getLocale().getLanguage().equals("de")&&!v.isNetworkConnectionRequired()&&(v.getFeatures()==null||!v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)))if(best==null||v.getQuality()>best.getQuality())best=v;
        if(best!=null){ready=tts.setVoice(best)==TextToSpeech.SUCCESS;tts.setSpeechRate(.83f);tts.setPitch(1.03f);}
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
            @Override public void onStart(String id) { }
            @Override public void onDone(String id){main.post(()->done(id));}
            @SuppressWarnings("deprecation") @Override public void onError(String id){main.post(()->done(id));}
            @Override public void onError(String id,int error){main.post(()->done(id));}
        });if(wasReady!=ready)changed.run();
    }
    private void done(String id){if(closed||!id.equals(utterance))return;Runnable task=after;after=null;utterance=null;if(task!=null)task.run();}
    void stop(){generation++;after=null;utterance=null;if(tts!=null)tts.stop();if(player!=null){player.release();player=null;}}
    void say(String text,Runnable callback){
        stop();if(closed)return;
        if(muted||!ready){if(callback!=null)callback.run();return;}
        utterance="speech-"+generation;after=callback;
        if(tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,utterance)==TextToSpeech.ERROR)done(utterance);
    }
    void say(String text){say(text,null);}
    void sound(String letter,Runnable callback){
        stop();if(closed)return;if(muted){if(callback!=null)callback.run();return;}
        final long expected=generation;
        try(AssetFileDescriptor asset=activity.getAssets().openFd("sounds/"+letter.toLowerCase(Locale.ROOT)+".ogg")){
            player=new MediaPlayer();player.setDataSource(asset.getFileDescriptor(),asset.getStartOffset(),asset.getLength());
            player.setOnCompletionListener(p->{if(generation!=expected)return;player.release();player=null;if(callback!=null)callback.run();});
            player.setOnErrorListener((p,what,extra)->{if(generation==expected){player.release();player=null;changed.run();}return true;});player.prepare();player.start();
        }catch(Exception e){if(player!=null){player.release();player=null;}changed.run();}
    }
    void sound(String letter){sound(letter,null);}
    void close(){closed=true;stop();if(tts!=null)tts.shutdown();}
}
