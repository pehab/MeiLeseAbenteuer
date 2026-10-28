package de.meiapps.leseabenteuer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;
import java.util.*;

/** Device-level checks use real taps and the native accessibility tree, including scrollable layouts. */
public final class SmokeInstrumentation extends Instrumentation {
    private Bundle args;private MainActivity activity;private int checks;
    @Override public void onCreate(Bundle args){this.args=args;start();}
    private void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    private Object field(String name)throws Exception{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private void call(String name)throws Exception{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);runOnMainSync(()->{try{m.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();}
    private void tap(String text)throws Exception{tap(text,false,false);}
    private void tap(String text,boolean picture)throws Exception{tap(text,picture,true);}
    private void tap(String text,boolean picture,boolean choice)throws Exception{
        for(int attempt=0;attempt<35;attempt++){
            AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();List<AccessibilityNodeInfo> nodes=new ArrayList<>();if(root!=null)nodes.add(root);
            for(int i=0;i<nodes.size();i++){AccessibilityNodeInfo n=nodes.get(i);for(int j=0;j<n.getChildCount();j++){AccessibilityNodeInfo c=n.getChild(j);if(c!=null)nodes.add(c);}
                if(!text.contentEquals(n.getText()==null?"":n.getText())&&!text.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))continue;
                if(picture&&!"android.widget.LinearLayout".contentEquals(n.getClassName()))continue;
                if(choice&&!picture&&!"android.widget.Button".contentEquals(n.getClassName()))continue;
                AccessibilityNodeInfo target=n;while(target!=null&&!target.isClickable())target=target.getParent();
                if(target!=null&&target.isEnabled()&&target.isVisibleToUser()&&target.performAction(AccessibilityNodeInfo.ACTION_CLICK)){waitForIdleSync();Thread.sleep(100);return;}
            }
            boolean scrolled=false;for(AccessibilityNodeInfo n:nodes)if(n.isScrollable()&&n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)){scrolled=true;break;}
            Thread.sleep(scrolled?200:100);
        }throw new AssertionError("Action not reachable: "+text);
    }
    private String choice(Learning.Question q,String option){return q.mode==Learning.Mode.INITIAL?Learning.sound(option).example:q.mode==Learning.Mode.READ?Learning.word(option).text:option;}
    private void screenshot(String name)throws Exception{android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();if(bitmap!=null){ByteArrayOutputStream bytes=new ByteArrayOutputStream();bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes);bitmap.recycle();Bundle snapshot=new Bundle();snapshot.putString("image_"+name,android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP));sendStatus(0,snapshot);}}
    @Override public void onStart(){Bundle result=new Bundle();try{
        Context context=getTargetContext();activity=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();Thread.sleep(1000);
        Progress progress=(Progress)field("progress");
        if("seed".equals(args.getString("phase"))){runOnMainSync(()->{progress.name("Update-Kind");progress.record("LISTEN:M",false);progress.finish(5);});check(context.getSharedPreferences("learning-v1",0).edit().commit(),"upgrade fixture flushed to disk before stopping instrumentation");result.putString("summary","PASS: seeded existing child profile for upgrade");finish(Activity.RESULT_OK,result);return;}
        if("upgrade".equals(args.getString("phase"))){check(progress.name().equals("Update-Kind"),"child name survives APK update (actual: "+progress.name()+", rounds: "+progress.rounds()+")");check(progress.stars()==1&&progress.completed()==1,"progress survives APK update");check(progress.get("LISTEN:M").independent==1,"individual skill history survives update");result.putString("summary","PASS: "+checks+" upgrade preservation assertions");finish(Activity.RESULT_OK,result);return;}
        if("reopen".equals(args.getString("phase"))){check(progress.profile()==0,"selected profile persists");check(progress.stars()==4&&progress.rounds()==4,"completed rounds and rewards persist after process restart");check(progress.completed()==4,"independent rounds persist");check(Learning.known(progress.completed())==4,"letter progression persists");result.putString("summary","PASS: "+checks+" restart assertions");finish(Activity.RESULT_OK,result);return;}
        check(progress.stars()==0,"fresh installation has no rewards");screenshot("home.png");
        String[] names={"Laute lauschen","Bildersuche","Laute verbinden","Wörter entdecken"};
        for(int mode=0;mode<4;mode++){
            tap(names[mode]);Learning.Question q=(Learning.Question)field("question");check(q.mode.ordinal()==mode,"requested game opens");
            Learning.Round round=(Learning.Round)field("round");String wrong=null;for(String option:q.options)if(!option.equals(q.target)){wrong=option;break;}tap(choice(q,wrong),q.mode==Learning.Mode.INITIAL||q.mode==Learning.Mode.READ);check(round.assisted&&round.answered==0,"wrong answer helps instead of advancing");
            for(int n=0;n<6;n++){
                q=(Learning.Question)field("question");String label=choice(q,q.target);tap(label,q.mode==Learning.Mode.INITIAL||q.mode==Learning.Mode.READ);check(round.answered==n+1,"real tap advances exactly once");
                if(n==0)check(round.independent==0,"retry is not counted as independent");
                if(n==0)screenshot("game-"+mode+".png");
                tap(n==5?"Mein Abenteuerstern":"Weiter →");
            }
            check(progress.stars()==mode+1,"one reward per finished round");tap("Zur Lichtung");
        }
        check(progress.completed()==4,"eligible rounds unlock one additional letter");
        runOnMainSync(()->progress.profile(1));check(progress.stars()==0&&progress.completed()==0,"other child has independent progress");
        runOnMainSync(()->progress.profile(0));check(progress.stars()==4,"original child progress preserved");java.lang.reflect.Method home=MainActivity.class.getDeclaredMethod("home",boolean.class);home.setAccessible(true);runOnMainSync(()->{try{home.invoke(activity,false);}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();
        screenshot("home-progress.png");check(context.getSharedPreferences("learning-v1",0).edit().commit(),"profile data flushed before restart check");result.putString("summary","PASS: "+checks+" Android interaction assertions");finish(Activity.RESULT_OK,result);
    }catch(Throwable error){try{screenshot("failure.png");}catch(Exception ignored){}result.putString("error",android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
}
