package de.meiapps.leseabenteuer;

import android.app.*;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int INK=0xff343b5a,GREEN=0xff398e90,MUTED=0xff6e728c,CREAM=0xfffff6df,GOLD=0xffffd35e;
    private Progress progress;private Narrator voice;private de.meiapps.updates.GitHubUpdates updates;private LinearLayout content;private ScrollView scroll;
    private Learning.Mode mode;private Learning.Question question;private Learning.Round round;
    private final Random random=new Random();private String screen="home";private boolean credited,hint;
    private int counted;private TextView feedback;private Button next;private final List<View> answers=new ArrayList<>();

    @Override public void onCreate(Bundle state){
        super.onCreate(state);progress=new Progress(this);voice=new Narrator(this);updates=de.meiapps.updates.GitHubUpdates.attach(this,"pehab/MeiLeseAbenteuer",voice::stop);
        voice.changed(()->{if(!isFinishing()&&screen.equals("home")){home(false);if(voice.ready())voice.say("Hallo! Ich bin Milo. Tippe auf den großen Pfeil. Wir helfen den Waldtieren!");}});
        voice.muted(getPreferences(0).getBoolean("muted",false));
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);
        if(state!=null&&state.containsKey("mode")){
            try{mode=Learning.Mode.valueOf(state.getString("mode"));round=new Learning.Round();round.answered=state.getInt("answered");round.independent=state.getInt("independent");round.assisted=state.getBoolean("assisted");round.locked=state.getBoolean("locked");credited=state.getBoolean("credited");hint=state.getBoolean("hint");
                String target=state.getString("target");ArrayList<String> options=state.getStringArrayList("options");question=new Learning.Question(mode,target,"",options);
                if(state.getBoolean("finished"))celebrate();else exercise(false);return;
            }catch(IllegalArgumentException|NullPointerException ignored){round=null;}
        }
        home(true);
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(round!=null&&question!=null&&(screen.equals("game")||screen.equals("done"))){out.putString("mode",mode.name());out.putInt("answered",round.answered);out.putInt("independent",round.independent);out.putBoolean("assisted",round.assisted);out.putBoolean("locked",round.locked);out.putBoolean("credited",credited);out.putBoolean("hint",hint);out.putString("target",question.target);out.putStringArrayList("options",new ArrayList<>(question.options));out.putBoolean("finished",screen.equals("done"));}}
    @Override protected void onResume(){super.onResume();voice.refresh();if(screen.equals("parents"))updates.resume();}
    @Override protected void onPause(){updates.pause();voice.stop();super.onPause();}
    @Override protected void onDestroy(){updates.destroy();voice.close();super.onDestroy();}
    @Override public boolean onKeyUp(int code,KeyEvent event){if(code==KeyEvent.KEYCODE_BACK&&Build.VERSION.SDK_INT<33){back();return true;}return super.onKeyUp(code,event);}
    private void back(){voice.stop();if(screen.equals("home")){moveTaskToBack(true);return;}if(screen.equals("game")&&!round.finished())new AlertDialog.Builder(this).setTitle("Zur Lichtung zurück?").setMessage("Deine bisherigen Antworten sind gespeichert. Für diese Runde gibt es noch keinen Stern.").setPositiveButton("Zur Lichtung",(d,n)->home(false)).setNegativeButton("Weiterspielen",null).show();else home(false);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private void page(String name){
        screen=name;updates.pause();answers.clear();voice.stop();scroll=new ScrollView(this);scroll.setBackgroundColor(CREAM);scroll.setFillViewport(true);
        content=column();content.setPadding(dp(20),dp(18),dp(20),dp(24));scroll.addView(content);
        LinearLayout shell=column();shell.setBackgroundColor(CREAM);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(!name.equals("home")&&!name.equals("done")){
            LinearLayout navigation=row();navigation.setGravity(Gravity.CENTER);navigation.setPadding(0,dp(4),0,dp(4));
            navigation.addView(symbol("Zurück zur Abenteuerkarte","home",this::back,0xffc9e9e0),new LinearLayout.LayoutParams(dp(84),dp(58)));shell.addView(navigation);
        }setContentView(shell);
        shell.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else legacyInsets(v,insets);return insets;});shell.requestApplyInsets();
    }
    @SuppressWarnings("deprecation") private void legacyInsets(View v,WindowInsets insets){v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());}
    private TextView label(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");return t;}
    private void bold(TextView t){t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));}
    private GradientDrawable bg(int color,int stroke){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(20));if(stroke!=0)b.setStroke(dp(2),stroke);return b;}
    private Button button(String text,Runnable action,boolean primary){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(17);bold(b);b.setTextColor(primary?Color.WHITE:INK);b.setBackground(bg(primary?GREEN:0xffe6ecd9,0));b.setMinHeight(dp(58));b.setPadding(dp(12),dp(10),dp(12),dp(10));b.setOnClickListener(v->action.run());return b;}
    private void addButton(String text,Runnable action,boolean primary){Button b=button(text,action,primary);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(10);content.addView(b,p);}
    private void title(String title,String subtitle){TextView t=label(title,28,INK);bold(t);content.addView(t);TextView sub=label(subtitle,15,MUTED);sub.setPadding(0,dp(6),0,dp(16));content.addView(sub);}
    private void header(String subtitle){TextView name=label(subtitle,16,MUTED);name.setGravity(Gravity.END);content.addView(name);gap(12);}
    private void gap(int height){content.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private android.graphics.drawable.Drawable icon(String kind,int size){
        int px=dp(size);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(px,px,android.graphics.Bitmap.Config.ARGB_8888);
        PlayArt art=new PlayArt(this,kind,0);art.layout(0,0,px,px);art.draw(new android.graphics.Canvas(bitmap));
        return new android.graphics.drawable.BitmapDrawable(getResources(),bitmap);
    }
    private Button symbol(String description,String kind,Runnable action,int color){
        Button b=new Button(this){@Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);Object value=getTag();if(value instanceof android.graphics.drawable.Drawable){android.graphics.drawable.Drawable d=(android.graphics.drawable.Drawable)value;int side=dp(38),x=(getWidth()-side)/2,y=(getHeight()-side)/2;d.setBounds(x,y,x+side,y+side);d.draw(c);}}};
        b.setText("");b.setContentDescription(description);b.setBackground(bg(color,0));b.setTag(icon(kind,38));b.setOnClickListener(v->action.run());b.setMinHeight(dp(64));return b;
    }
    private void startAdventure(){start(Learning.Mode.values()[progress.rounds()%Learning.Mode.values().length]);}
    private void home(boolean greet){
        page("home");round=null;question=null;content.setPadding(dp(16),dp(10),dp(16),dp(24));
        LinearLayout top=row();TextView brand=label("MILOS ABENTEUER",15,INK);bold(brand);top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button grown=button("Für Große",this::parentGate,false);grown.setTextSize(12);grown.setMinHeight(dp(48));top.addView(grown,new LinearLayout.LayoutParams(dp(100),dp(48)));content.addView(top);gap(10);
        TextView hello=label("Hallo, "+progress.name()+"!",24,INK);bold(hello);content.addView(hello);gap(10);
        PlayArt map=new PlayArt(this,"map",progress.stars());map.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);map.setContentDescription("Abenteuerkarte. Mit Milo weiterspielen.");map.setOnClickListener(v->startAdventure());content.addView(map,new LinearLayout.LayoutParams(-1,dp(250)));
        LinearLayout status=row();TextView stars=label("★ "+progress.stars()+"  ·  "+(progress.stars()%4)+"/4",20,0xffb57822);bold(stars);status.addView(stars,new LinearLayout.LayoutParams(0,dp(45),1));
        TextView chapter=label(new String[]{"Blumenwiese","Waldpfad","Glitzersee","Sternenhügel","Wolkenbrücke","Baumhaus"}[(progress.stars()/4)%6],17,INK);status.addView(chapter);content.addView(status);
        Button play=button("Los geht’s!",this::startAdventure,true);play.setCompoundDrawablesWithIntrinsicBounds(icon("play",38),null,null,null);play.setContentDescription("Mit Milo losziehen");content.addView(play,new LinearLayout.LayoutParams(-1,dp(76)));gap(16);
        LinearLayout first=row();toy(first,"Laute lauschen","Hören","listen",0xffffdfa0,()->start(Learning.Mode.LISTEN));toy(first,"Bildersuche","Bilder","initial",0xffffcfba,()->start(Learning.Mode.INITIAL));content.addView(first);gap(10);
        LinearLayout second=row();toy(second,"Laute verbinden","Verbinden","blend",0xffc5e6fa,()->start(Learning.Mode.BLEND));toy(second,"Wörter entdecken","Wörter","read",0xfff8cfe0,()->start(Learning.Mode.READ));content.addView(second);gap(10);
        LinearLayout third=row();toy(third,"Mengen zählen","Zählen","basket",0xffd2ecb4,()->start(Learning.Mode.COUNT));toy(third,"Zahlen finden","Zahlen","number",0xffffe8ae,()->start(Learning.Mode.NUMBER));content.addView(third);gap(10);
        LinearLayout fourth=row();toy(fourth,"Buchstabenpaare","Aa · Bb","alphabet",0xffc9e9e0,()->start(Learning.Mode.ALPHABET));toy(fourth,"Meine Waldtiere","Waldtiere","album",0xffe1d4fa,this::album);content.addView(fourth);
        addButton("ABC entdecken",this::explore,false);
        if(greet)voice.say("Hallo "+progress.name()+"! Ich bin Milo. Tippe auf den großen Pfeil. Wir helfen den Waldtieren!");
    }
    private void toy(LinearLayout parent,String description,String caption,String art,int color,Runnable action){
        LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(8),dp(8),dp(8),dp(10));tile.setBackground(bg(color,0));
        tile.addView(new PlayArt(this,art,0),new LinearLayout.LayoutParams(dp(80),dp(72)));TextView text=label(caption,18,INK);bold(text);tile.addView(text);
        tile.setContentDescription(description);tile.setFocusable(true);tile.setOnClickListener(v->{tile.animate().scaleX(.94f).scaleY(.94f).setDuration(80).withEndAction(action).start();});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(116),1);if(parent.getChildCount()>0)lp.setMarginStart(dp(10));parent.addView(tile,lp);
    }
    private void album(){
        page("album");header("Deine Waldtiere");title("Neue Freunde!","Vier Sterne retten ein Waldtier.");
        String[] names={"Milo","Luna, das Häschen","Bruno, der Bär","Pia, der Vogel","Rosa, das Eichhörnchen","Fritzi, der Frosch"};
        for(int i=0;i<6;i++){final int n=i;boolean found=progress.stars()>=(i+1)*4;LinearLayout tile=row();tile.setPadding(dp(8),dp(8),dp(8),dp(8));tile.setBackground(bg(found?0xffffe7ac:0xffe1e2e9,0));
            PlayArt art=new PlayArt(this,found?"friend":"star",i);tile.addView(art,new LinearLayout.LayoutParams(dp(92),dp(82)));TextView text=label(found?names[i]:"Noch "+((i+1)*4-progress.stars())+" Runde(n)",19,INK);bold(text);tile.addView(text);tile.setContentDescription(found?names[i]:"Noch verborgen");tile.setOnClickListener(v->{voice.say(found?"Du hast "+names[n]+" gefunden. Toll, dass du hilfst!":"Spiele weiter und finde einen neuen Freund.");art.animate().rotation(12).setDuration(130).withEndAction(()->art.animate().rotation(-12).setDuration(130).withEndAction(()->art.animate().rotation(0).start()).start()).start();});content.addView(tile);gap(10);
        }voice.say("Das sind deine Waldtiere. Vier Abenteuersterne befreien jeweils einen Freund. Hilf allen sechs Waldtieren!");
    }
    private void start(Learning.Mode target){if(voice.muted()&&target!=Learning.Mode.READ&&target!=Learning.Mode.ALPHABET&&target!=Learning.Mode.COUNT){new AlertDialog.Builder(this).setTitle("Dieses Spiel braucht Ton").setMessage("Bitte einen Erwachsenen, den Ton unter Für Große einzuschalten.").setPositiveButton("Zurück",null).show();return;}mode=target;round=new Learning.Round();credited=false;question=Learning.next(mode,Learning.isNumber(mode)?progress.numberCompleted():progress.completed(),progress,random,null);hint=false;exercise(true);}
    private void exercise(boolean announce){
        page("game");counted=0;header("●●●●●●".substring(0,Math.min(6,round.answered))+"○○○○○○".substring(0,Math.max(0,6-round.answered)));
        String heading=mode==Learning.Mode.COUNT?"Füll Milos Korb!":mode==Learning.Mode.NUMBER?"Zahlenzauber!":mode==Learning.Mode.ALPHABET?"Groß und klein!":mode==Learning.Mode.LISTEN?"Spitz die Ohren!":mode==Learning.Mode.INITIAL?"Welches Bild passt?":mode==Learning.Mode.BLEND?"Zusammen klingt’s!":"Lies und entdecke!";
        String instruction=mode==Learning.Mode.COUNT?"Zähle die Beeren. Wie viele sind es?":mode==Learning.Mode.NUMBER?"Hör die Zahl und tippe sie an.":mode==Learning.Mode.ALPHABET?"Finde den kleinen Buchstaben zum großen.":mode==Learning.Mode.LISTEN?"Hör den Laut. Tippe auf den Buchstaben.":mode==Learning.Mode.INITIAL?"Was beginnt mit diesem Laut?":mode==Learning.Mode.BLEND?"Zieh die Laute zusammen. Finde das passende Stück.":"Lies das Wort. Welches Bild passt dazu?";
        title(heading,instruction);
        if(mode==Learning.Mode.COUNT){
            PlayArt berries=new PlayArt(this,"count",Integer.parseInt(question.target));berries.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);berries.setContentDescription("Beeren im Korb. Antippen zum gemeinsamen Zählen.");berries.setBackground(bg(0xffe4f1cd,0));berries.setOnClickListener(v->{round.assisted=true;counted=counted%Integer.parseInt(question.target)+1;voice.say(Learning.NUMBER_NAMES[counted]);feedback.setText(String.valueOf(counted));});content.addView(berries,new LinearLayout.LayoutParams(-1,dp(146)));
        }else{
            String show=mode==Learning.Mode.LISTEN||mode==Learning.Mode.NUMBER?"♪":mode==Learning.Mode.BLEND?question.target.substring(0,1)+"  +  "+question.target.substring(1):question.target;
            TextView main=label(show,mode==Learning.Mode.READ?48:68,GREEN);bold(main);main.setGravity(Gravity.CENTER);main.setBackground(bg(0xffd3ebf5,0));main.setContentDescription("Aufgabe noch einmal anhören");main.setOnClickListener(v->prompt());content.addView(main,new LinearLayout.LayoutParams(-1,dp(126)));
        }
        if(mode==Learning.Mode.BLEND){SeekBar bridge=new SeekBar(this);bridge.setMax(100);bridge.setContentDescription("Laute verbinden: nach rechts schieben");bridge.setPadding(dp(18),dp(12),dp(18),dp(12));bridge.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){int part=-1;public void onStartTrackingTouch(SeekBar seek){part=-1;}public void onStopTrackingTouch(SeekBar seek){if(seek.getProgress()>85)voice.say(question.target.toLowerCase(Locale.GERMAN));}public void onProgressChanged(SeekBar seek,int value,boolean fromUser){if(!fromUser)return;int n=value<50?0:1;if(n!=part){part=n;voice.sound(question.target.substring(n,n+1));}}});content.addView(bridge,new LinearLayout.LayoutParams(-1,dp(58)));}
        LinearLayout audio=row();Button repeat=symbol("Aufgabe noch einmal hören","repeat",this::prompt,0xffc8e7f8);repeat.setTextSize(14);audio.addView(repeat,new LinearLayout.LayoutParams(0,dp(54),1));Button help=symbol("Hilf mir: gemeinsam lösen","help",this::help,0xffffe1a0);help.setTextSize(14);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,dp(54),1);hp.setMarginStart(dp(10));audio.addView(help,hp);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(10);content.addView(audio,ap);gap(16);
        LinearLayout choices=row();
        for(String option:question.options){
            boolean picture=mode==Learning.Mode.INITIAL||mode==Learning.Mode.READ;
            View choice;
            if(picture){String kind=mode==Learning.Mode.INITIAL?Learning.sound(option).picture:Learning.word(option).picture;LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(6),dp(10),dp(6),dp(10));tile.setBackground(bg(Color.WHITE,0xffe3e7d8));tile.addView(new Picture(this,kind),new LinearLayout.LayoutParams(-1,dp(100)));tile.setContentDescription(mode==Learning.Mode.INITIAL?Learning.sound(option).example:Learning.word(option).text);tile.setFocusable(true);tile.setOnClickListener(v->answer(option,tile));choice=tile;}
            else{Button letter=button(mode==Learning.Mode.ALPHABET?option.toLowerCase(Locale.GERMAN):option,()->{},false);letter.setContentDescription("Antwort "+option);letter.setTextSize(32);letter.setBackground(bg(Color.WHITE,0xffe3e7d8));letter.setOnClickListener(v->answer(option,letter));choice=letter;}
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,picture?dp(124):dp(76),1);if(!answers.isEmpty())cp.setMarginStart(dp(8));choices.addView(choice,cp);answers.add(choice);
        }
        content.addView(choices);
        feedback=label(round.locked?"Das hast du geschafft!":"Du darfst dir Zeit lassen.",17,MUTED);feedback.setPadding(0,dp(8),0,dp(5));content.addView(feedback);
        next=symbol(round.answered>=6?"Mein Abenteuerstern":"Weiter →",round.answered>=6?"star":"next",this::advance,GREEN);next.setVisibility(round.locked?View.VISIBLE:View.GONE);content.addView(next);
        if(round.locked)for(View answer:answers)answer.setEnabled(false);if(hint&&!round.locked)highlight();
        if(announce){if(mode==Learning.Mode.READ||mode==Learning.Mode.COUNT)voice.say(instruction);else voice.say(instruction,()->{if(!screen.equals("game"))return;prompt();});}
    }
    private void prompt(){
        if(question==null||!screen.equals("game"))return;
        if(mode==Learning.Mode.READ){voice.say("Lies das Wort. Welches Bild passt dazu?");return;}
        if(mode==Learning.Mode.COUNT){voice.say("Zähle die Beeren im Korb. Tippe unten auf die passende Zahl.");return;}
        if(mode==Learning.Mode.NUMBER){voice.say("Finde die "+Learning.NUMBER_NAMES[Integer.parseInt(question.target)]+".");return;}
        if(mode==Learning.Mode.ALPHABET){voice.say("Das große "+question.target+" sucht seinen kleinen Freund.");return;}
        if(mode==Learning.Mode.BLEND){voice.sound(question.target.substring(0,1),()->voice.sound(question.target.substring(1)));return;}
        voice.sound(question.target);
    }
    private void help(){
        if(round.locked)return;round.assisted=true;hint=true;highlight();feedback.setText(R.string.help_together);
        for(int i=0;i<question.options.size();i++)if(question.options.get(i).equals(question.target)){
            View tile=answers.get(i);tile.animate().scaleX(1.08f).scaleY(1.08f).setDuration(200).withEndAction(()->tile.animate().scaleX(1).scaleY(1).setDuration(200).start()).start();
        }
        if(mode==Learning.Mode.READ)voice.say(question.target+". Schau auf das leuchtende Bild.");
        else if(mode==Learning.Mode.BLEND)voice.sound(question.target.substring(0,1),()->voice.sound(question.target.substring(1),()->voice.say("Zusammen: "+question.target.toLowerCase(Locale.GERMAN)+". Tippe auf das leuchtende Feld.")));
        else if(Learning.isNumber(mode))voice.say("Es ist die "+Learning.NUMBER_NAMES[Integer.parseInt(question.target)]+". Schau auf das leuchtende Feld.");
        else if(mode==Learning.Mode.ALPHABET)voice.say("Schau, hier ist das kleine "+question.target+". Die beiden gehören zusammen.");
        else voice.sound(question.target);
    }
    private void highlight(){for(int i=0;i<question.options.size();i++)if(question.options.get(i).equals(question.target))answers.get(i).setBackground(bg(0xffffe5a1,0xffd7aa52));}
    private void answer(String option,View tile){
        if(round.locked)return;boolean correct=option.equals(question.target);if(!round.answer(correct)){tile.setBackground(bg(0xffffead9,0xffe8ba8d));feedback.setText(R.string.try_together);voice.say("Fast! Wir probieren es noch einmal.");return;}
        progress.record(question.key,round.assisted);tile.setBackground(bg(0xffd3e8c7,GREEN));for(View answer:answers)answer.setEnabled(false);feedback.setText(round.assisted?"Gemeinsam geschafft!":"Prima, das passt!");feedback.setTextColor(GREEN);next.setContentDescription(round.finished()?"Mein Abenteuerstern":"Weiter →");next.setTag(icon(round.finished()?"star":"next",38));next.invalidate();next.setVisibility(View.VISIBLE);
        String result=mode==Learning.Mode.READ?question.target+". Das hast du geschafft!":mode==Learning.Mode.INITIAL?Learning.sound(question.target).example+". Das passt!":"Prima, das passt!";voice.say(result);next.requestFocus();scroll.post(()->scroll.smoothScrollTo(0,content.getHeight()));
    }
    private void advance(){if(!round.locked)return;if(round.finished()){if(!credited){progress.finish(mode,round.independent);credited=true;}celebrate();return;}String previous=question.target;round.advance();hint=false;question=Learning.next(mode,Learning.isNumber(mode)?progress.numberCompleted():progress.completed(),progress,random,previous);exercise(true);}
    private void celebrate(){
        page("done");title("Geschafft!","Du hilfst den Waldtieren!");
        PlayArt reward=new PlayArt(this,"friend",Math.max(0,progress.stars()/4-1)%6);content.addView(reward,new LinearLayout.LayoutParams(-1,dp(200)));reward.setScaleX(.25f);reward.setScaleY(.25f);reward.animate().scaleX(1).scaleY(1).rotation(360).setDuration(650).start();
        PlayArt star=new PlayArt(this,"star",0);content.addView(star,new LinearLayout.LayoutParams(-1,dp(120)));TextView total=label("★ "+progress.stars(),30,INK);total.setGravity(Gravity.CENTER);content.addView(total);
        Button home=symbol("Zur Lichtung","home",()->home(false),0xffbde6d6);content.addView(home,new LinearLayout.LayoutParams(-1,dp(74)));gap(12);
        Button again=symbol("Noch eine kleine Runde","play",this::startAdventure,GREEN);content.addView(again,new LinearLayout.LayoutParams(-1,dp(74)));
        voice.say(progress.stars()<=24&&progress.stars()%4==0?"Geschafft! Du hast einen neuen Waldtier-Sticker gefunden! Ein Stern für dich. Du darfst auch eine Pause machen.":"Geschafft! Dein Abenteuer geht weiter. Ein Stern für dich. Du darfst auch eine Pause machen.");
    }
    private void explore(){
        page("explore");header("Das ganze ABC");title("Deine Buchstaben","Tippe und hör zu. Groß und klein gehören zusammen.");
        String abc=Learning.ALPHABET;for(int i=0;i<abc.length();i+=3){LinearLayout line=row();for(int j=i;j<Math.min(i+3,abc.length());j++){
            final String letter=abc.substring(j,j+1);final String example=Learning.LETTER_EXAMPLES[j];Button chip=button(letter+" "+letter.toLowerCase(Locale.GERMAN),()->voice.say(letter+". Wie in "+example),false);chip.setTextSize(28);chip.setContentDescription(letter+", "+example);chip.setBackground(bg(new int[]{0xffffdfa0,0xffc9e9e0,0xffe1d4fa}[j%3],0));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(80),1);lp.setMarginEnd(dp(6));line.addView(chip,lp);
        }content.addView(line);gap(8);}TextView info=label("ß: kommt im Deutschen nicht am Wortanfang vor.",15,MUTED);content.addView(info);addButton("ß wie in Fuß",()->voice.say("Eszett. Wie in Fuß."),false);
        voice.say("Das ganze Alphabet! Tippe auf einen Buchstaben, dann höre ich ihn mit dir an.");
    }
    private void parentGate(){voice.stop();int a=7+random.nextInt(5),b=6+random.nextInt(6);EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setHint("Ergebnis");input.setPadding(dp(20),dp(12),dp(20),dp(12));AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Für Erwachsene").setMessage("Wie viel ist "+a+" + "+b+"?").setView(input).setPositiveButton("Öffnen",null).setNegativeButton("Zurück",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(input.getText().toString().trim().equals(String.valueOf(a+b))){dialog.dismiss();parents();}else input.setError("Bitte noch einmal rechnen.");}));dialog.show();}
    private void parents(){page("parents");updates.resume();header("Für Erwachsene");title("Mit Freude, ohne Eile","Die App ist ein spielerischer Einstieg. Begleitet die ersten Runden gemeinsam.");
        TextView info=label("Zahlenrunden: "+progress.numberCompleted()+" · Zahlenraum bis "+Learning.numberLimit(progress.numberCompleted())+"\nAlphabet: 26 Buchstaben und Ä, Ö, Ü, ß frei erkunden. Hier werden Buchstabennamen gesprochen; die Lautspiele nutzen die acht aufgenommenen Laute.\n\nAktuelles Profil: "+progress.name()+"\nAbgeschlossene Runden: "+progress.rounds()+"\nAbenteuersterne: "+progress.stars()+"\nEingeführte Buchstaben: "+Learning.letters(progress.completed())+"\n\nNeue Buchstaben kommen nach jeweils drei Runden mit mindestens vier selbstständig gelösten Aufgaben hinzu. Ein gelöstes Rätsel nach einem Tipp zählt als Übung, aber nicht als selbstständig gelöst.\n\nDie Sternbelohnung gibt es für jede vollständige Runde. Keine Zeitlimits, täglichen Serien oder verlorenen Leben.",16,INK);content.addView(info);
        addButton("Kinderprofil wechseln",()->new AlertDialog.Builder(this).setTitle("Welches Profil?").setItems(new String[]{"Profil 1","Profil 2","Profil 3"},(d,n)->{progress.profile(n);parents();}).setNegativeButton("Zurück",null).show(),false);
        addButton("Namen ändern",()->{EditText field=new EditText(this);field.setSingleLine(true);field.setText(progress.name());field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});new AlertDialog.Builder(this).setTitle("Vorname oder Spitzname").setView(field).setPositiveButton("Speichern",(d,n)->{progress.name(field.getText().toString());parents();}).setNegativeButton("Abbrechen",null).show();},false);
        addButton(voice.muted()?"Ton einschalten":"Ton ausschalten",()->{voice.muted(!voice.muted());getPreferences(0).edit().putBoolean("muted",voice.muted()).apply();parents();},false);
        TextView speech=label(voice.ready()?"✓ Deutsche Offline-Stimme verfügbar.":"Noch keine deutsche Offline-Stimme gefunden. Bitte in den Android-Spracheinstellungen eine deutsche Stimme herunterladen. Die App verwendet keine Netzwerkstimme.",15,MUTED);speech.setPadding(0,dp(20),0,0);content.addView(speech);
        addButton("Sprachausgabe testen",()->voice.say("Hallo! Ich bin Milo. Wir entdecken gemeinsam die Buchstaben."),false);
        addButton("Android-Spracheinstellungen",()->{try{startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}catch(android.content.ActivityNotFoundException e){new AlertDialog.Builder(this).setMessage("Öffne in den Android-Einstellungen die Text-in-Sprache-Ausgabe und lade eine deutsche Offline-Stimme herunter.").setPositiveButton("OK",null).show();}},false);
        addButton("Fortschritt dieses Profils löschen",()->new AlertDialog.Builder(this).setTitle("Fortschritt löschen?").setMessage("Nur das aktuelle Profil wird zurückgesetzt. Andere Kinderprofile bleiben erhalten.").setPositiveButton("Löschen",(d,n)->{progress.reset();parents();}).setNegativeButton("Abbrechen",null).show(),false);
        addButton("Updates suchen & einstellen",updates::settings,false);
        addButton("Über die App & Audioquellen",this::about,false);
    }
    private String version(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException e){return "";}}
    private void about(){String audio="";try(InputStream in=getAssets().open("AUDIO-CREDITS.txt")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] data=new byte[2048];int n;while((n=in.read(data))!=-1)out.write(data,0,n);audio=out.toString("UTF-8");}catch(IOException ignored){}
        new AlertDialog.Builder(this).setTitle("MeiLeseAbenteuer · "+version()).setMessage("Ein Leseabenteuer von meiApps.\n\nAlle Profile bleiben auf diesem Gerät. Keine Werbung, Käufe, Konten, Kamera oder Mikrofon. Lautaufnahmen sind enthalten; gesprochene Anleitungen und Wörter nutzen eine installierte deutsche Offline-Stimme.\n\n"+audio).setPositiveButton("Zurück",null).show();}
}
