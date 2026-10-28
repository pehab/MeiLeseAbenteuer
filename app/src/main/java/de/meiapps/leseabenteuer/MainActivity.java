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
    private static final int INK=0xff29463f,GREEN=0xff277a63,MUTED=0xff697d70,CREAM=0xfff8f4e8,GOLD=0xffffd778;
    private Progress progress;private Narrator voice;private de.meiapps.updates.GitHubUpdates updates;private LinearLayout content;private ScrollView scroll;
    private Learning.Mode mode;private Learning.Question question;private Learning.Round round;
    private final Random random=new Random();private String screen="home";private boolean credited,hint;
    private TextView feedback;private Button next;private final List<View> answers=new ArrayList<>();

    @Override public void onCreate(Bundle state){
        super.onCreate(state);progress=new Progress(this);voice=new Narrator(this);updates=de.meiapps.updates.GitHubUpdates.attach(this,"pehab/MeiLeseAbenteuer",voice::stop);
        voice.changed(()->{if(!isFinishing()&&screen.equals("home")){home(false);if(voice.ready())voice.say("Hallo! Ich bin Milo. Komm mit ins Leseabenteuer.");}});
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
        content=column();content.setPadding(dp(20),dp(18),dp(20),dp(24));scroll.addView(content);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}else legacyInsets(v,insets);return insets;});scroll.requestApplyInsets();
    }
    @SuppressWarnings("deprecation") private void legacyInsets(View v,WindowInsets insets){v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());}
    private TextView label(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");return t;}
    private void bold(TextView t){t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));}
    private GradientDrawable bg(int color,int stroke){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(20));if(stroke!=0)b.setStroke(dp(2),stroke);return b;}
    private Button button(String text,Runnable action,boolean primary){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(17);bold(b);b.setTextColor(primary?Color.WHITE:INK);b.setBackground(bg(primary?GREEN:0xffe6ecd9,0));b.setMinHeight(dp(58));b.setPadding(dp(12),dp(10),dp(12),dp(10));b.setOnClickListener(v->action.run());return b;}
    private void addButton(String text,Runnable action,boolean primary){Button b=button(text,action,primary);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(10);content.addView(b,p);}
    private void title(String title,String subtitle){TextView t=label(title,28,INK);bold(t);content.addView(t);TextView sub=label(subtitle,15,MUTED);sub.setPadding(0,dp(6),0,dp(16));content.addView(sub);}
    private void header(String subtitle){LinearLayout row=row();Button back=button("‹ Zurück",this::back,false);back.setTextSize(14);back.setMinHeight(dp(48));row.addView(back,new LinearLayout.LayoutParams(dp(106),dp(48)));TextView name=label(subtitle,14,MUTED);name.setGravity(Gravity.END);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));content.addView(row);gap(14);}
    private void gap(int height){content.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private void home(boolean greet){
        page("home");round=null;question=null;
        LinearLayout top=row();TextView brand=label("MEI LESEABENTEUER",12,GREEN);brand.setLetterSpacing(.13f);bold(brand);top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));Button grown=button("Für Große",this::parentGate,false);grown.setTextSize(12);grown.setMinHeight(dp(48));top.addView(grown,new LinearLayout.LayoutParams(dp(112),dp(48)));content.addView(top);gap(16);
        LinearLayout welcome=row();Picture fox=new Picture(this,"fox");welcome.addView(fox,new LinearLayout.LayoutParams(dp(106),dp(118)));LinearLayout words=column();TextView hi=label("Hallo, "+progress.name()+"!",26,INK);bold(hi);words.addView(hi);TextView text=label("Ich bin Milo.\nKomm mit ins Leseabenteuer!",16,MUTED);text.setPadding(0,dp(7),0,0);words.addView(text);welcome.addView(words,new LinearLayout.LayoutParams(0,-2,1));content.addView(welcome);
        TextView journey=label("DEINE LESELICHTUNG",11,GREEN);journey.setLetterSpacing(.12f);journey.setPadding(0,dp(16),0,dp(7));content.addView(journey);
        LinearLayout map=column();map.setPadding(dp(17),dp(17),dp(17),dp(17));map.setBackground(bg(0xffe6ecd9,0));
        TextView place=label(progress.completed()<3?"Die kleine Lichtung":progress.completed()<9?"Am glitzernden See":"Das Baumhaus",21,INK);bold(place);map.addView(place);
        TextView stars=label("★ "+progress.stars()+" Abenteuersterne   ·   "+Learning.known(progress.completed())+" Buchstaben",14,GREEN);stars.setPadding(0,dp(7),0,dp(10));map.addView(stars);
        LinearLayout letters=row();for(int i=0;i<Learning.known(progress.completed());i++){String letter=Learning.SOUNDS.get(i).letter;Button chip=button(letter,()->voice.sound(letter),false);chip.setTextSize(21);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(50),1);p.setMarginEnd(dp(3));letters.addView(chip,p);}map.addView(letters);content.addView(map);
        addButton("▶  Mit Milo losziehen",()->start(Learning.Mode.values()[progress.rounds()%4]),true);
        TextView choose=label("Oder such dir ein Spiel aus",15,MUTED);choose.setPadding(0,dp(22),0,dp(10));content.addView(choose);
        gameRow(Learning.Mode.LISTEN,"Laute lauschen","Hören und Buchstaben finden",Learning.Mode.INITIAL,"Bildersuche","Was klingt am Anfang gleich?");
        gameRow(Learning.Mode.BLEND,"Laute verbinden","Zwei Laute werden ein Team",Learning.Mode.READ,"Wörter entdecken","Lesen und das Bild finden");
        addButton("Meine Buchstaben entdecken",this::explore,false);
        if(!voice.ready()){TextView notice=label("Für gesprochene Anleitungen bitte einmal mit einem Erwachsenen die deutsche Offline-Stimme unter „Für Große“ prüfen. Die Lautaufnahmen sind schon dabei.",13,MUTED);notice.setPadding(0,dp(16),0,0);content.addView(notice);}
        if(greet)voice.say("Hallo! Ich bin Milo. Komm mit ins Leseabenteuer. Tippe auf Losziehen.");
    }
    private void gameRow(Learning.Mode a,String titleA,String subA,Learning.Mode b,String titleB,String subB){LinearLayout row=row();gameTile(row,a,titleA,subA,false);gameTile(row,b,titleB,subB,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);content.addView(row,p);}
    private void gameTile(LinearLayout parent,Learning.Mode target,String text,String sub,boolean right){LinearLayout tile=column();tile.setPadding(dp(14),dp(17),dp(14),dp(17));tile.setBackground(bg(Color.WHITE,0xffe3e7d8));TextView t=label(text,17,INK);bold(t);tile.addView(t);TextView s=label(sub,13,MUTED);s.setPadding(0,dp(6),0,0);tile.addView(s);tile.setMinimumHeight(dp(116));tile.setFocusable(true);tile.setOnClickListener(v->start(target));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(right)p.setMarginStart(dp(10));parent.addView(tile,p);}
    private void start(Learning.Mode target){if(voice.muted()&&target!=Learning.Mode.READ){new AlertDialog.Builder(this).setTitle("Dieses Spiel braucht Ton").setMessage("Bitte einen Erwachsenen, den Ton unter Für Große einzuschalten.").setPositiveButton("Zurück",null).show();return;}mode=target;round=new Learning.Round();credited=false;question=Learning.next(mode,progress.completed(),progress,random,null);hint=false;exercise(true);}
    private void exercise(boolean announce){
        page("game");header("Aufgabe "+Math.min(6,round.answered+(round.locked?0:1))+" von 6");
        String heading=mode==Learning.Mode.LISTEN?"Spitz die Ohren!":mode==Learning.Mode.INITIAL?"Welches Bild passt?":mode==Learning.Mode.BLEND?"Zusammen klingt’s!":"Lies und entdecke!";
        String instruction=mode==Learning.Mode.LISTEN?"Hör den Laut. Tippe auf den Buchstaben.":mode==Learning.Mode.INITIAL?"Was beginnt mit diesem Laut?":mode==Learning.Mode.BLEND?"Zieh die Laute zusammen. Finde das passende Stück.":"Lies das Wort. Welches Bild passt dazu?";
        title(heading,instruction);
        TextView main=label(mode==Learning.Mode.LISTEN?"♪":mode==Learning.Mode.BLEND?question.target.substring(0,1)+"  +  "+question.target.substring(1):question.target,mode==Learning.Mode.READ?48:72,GREEN);bold(main);main.setGravity(Gravity.CENTER);main.setPadding(dp(10),dp(10),dp(10),dp(10));main.setBackground(bg(0xffe6ecd9,0));main.setContentDescription(mode==Learning.Mode.LISTEN?"Laut noch einmal hören":question.target);main.setOnClickListener(v->prompt());content.addView(main,new LinearLayout.LayoutParams(-1,dp(126)));
        if(mode==Learning.Mode.BLEND){SeekBar bridge=new SeekBar(this);bridge.setMax(100);bridge.setContentDescription("Laute verbinden: nach rechts schieben");bridge.setPadding(dp(18),dp(12),dp(18),dp(12));bridge.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){int part=-1;public void onStartTrackingTouch(SeekBar seek){part=-1;}public void onStopTrackingTouch(SeekBar seek){if(seek.getProgress()>85)voice.say(question.target.toLowerCase(Locale.GERMAN));}public void onProgressChanged(SeekBar seek,int value,boolean fromUser){if(!fromUser)return;int n=value<50?0:1;if(n!=part){part=n;voice.sound(question.target.substring(n,n+1));}}});content.addView(bridge,new LinearLayout.LayoutParams(-1,dp(58)));}
        LinearLayout audio=row();Button repeat=button("♪ Noch mal hören",this::prompt,false);repeat.setTextSize(14);audio.addView(repeat,new LinearLayout.LayoutParams(0,dp(54),1));Button help=button("Hilf mir",this::help,false);help.setTextSize(14);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,dp(54),1);hp.setMarginStart(dp(10));audio.addView(help,hp);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(10);content.addView(audio,ap);gap(16);
        LinearLayout choices=row();
        for(String option:question.options){
            boolean picture=mode==Learning.Mode.INITIAL||mode==Learning.Mode.READ;
            View choice;
            if(picture){String kind=mode==Learning.Mode.INITIAL?Learning.sound(option).picture:Learning.word(option).picture;LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setPadding(dp(6),dp(10),dp(6),dp(10));tile.setBackground(bg(Color.WHITE,0xffe3e7d8));tile.addView(new Picture(this,kind),new LinearLayout.LayoutParams(-1,dp(100)));tile.setContentDescription(mode==Learning.Mode.INITIAL?Learning.sound(option).example:Learning.word(option).text);tile.setFocusable(true);tile.setOnClickListener(v->answer(option,tile));choice=tile;}
            else{Button letter=button(option,()->{},false);letter.setTextSize(27);letter.setBackground(bg(Color.WHITE,0xffe3e7d8));letter.setOnClickListener(v->answer(option,letter));choice=letter;}
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,picture?dp(124):dp(76),1);if(!answers.isEmpty())cp.setMarginStart(dp(8));choices.addView(choice,cp);answers.add(choice);
        }
        content.addView(choices);
        feedback=label(round.locked?"Das hast du geschafft!":"Du darfst dir Zeit lassen.",17,MUTED);feedback.setPadding(0,dp(8),0,dp(5));content.addView(feedback);
        next=button(round.answered>=6?"Mein Abenteuerstern":"Weiter →",this::advance,true);next.setVisibility(round.locked?View.VISIBLE:View.GONE);content.addView(next);
        if(round.locked)for(View answer:answers)answer.setEnabled(false);if(hint&&!round.locked)highlight();
        if(announce)voice.say(instruction,()->{if(!screen.equals("game"))return;if(mode==Learning.Mode.LISTEN||mode==Learning.Mode.INITIAL)voice.sound(question.target);});
    }
    private void prompt(){if(question==null||!screen.equals("game"))return;if(mode==Learning.Mode.READ){help();return;}if(mode==Learning.Mode.BLEND){voice.sound(question.target.substring(0,1),()->voice.sound(question.target.substring(1),()->voice.say(question.target.toLowerCase(Locale.GERMAN))));}else voice.sound(question.target);}
    private void help(){if(round.locked)return;round.assisted=true;hint=true;highlight();feedback.setText("Wir machen es zusammen. Schau auf das goldene Feld.");if(mode==Learning.Mode.READ)voice.say(question.target);else if(mode==Learning.Mode.BLEND)prompt();else voice.sound(question.target);}
    private void highlight(){for(int i=0;i<question.options.size();i++)if(question.options.get(i).equals(question.target))answers.get(i).setBackground(bg(0xffffe5a1,0xffd7aa52));}
    private void answer(String option,View tile){
        if(round.locked)return;boolean correct=option.equals(question.target);if(!round.answer(correct)){tile.setBackground(bg(0xffffead9,0xffe8ba8d));feedback.setText("Hör noch einmal hin. Wir probieren es zusammen.");voice.say("Fast! Wir probieren es noch einmal.");return;}
        progress.record(question.key,round.assisted);tile.setBackground(bg(0xffd3e8c7,GREEN));for(View answer:answers)answer.setEnabled(false);feedback.setText(round.assisted?"Gemeinsam geschafft!":"Prima, das passt!");feedback.setTextColor(GREEN);next.setText(round.finished()?"Mein Abenteuerstern":"Weiter →");next.setVisibility(View.VISIBLE);
        String result=mode==Learning.Mode.READ?question.target+". Das hast du geschafft!":mode==Learning.Mode.INITIAL?Learning.sound(question.target).example+". Das passt!":"Prima, das passt!";voice.say(result);next.requestFocus();scroll.post(()->scroll.smoothScrollTo(0,content.getHeight()));
    }
    private void advance(){if(!round.locked)return;if(round.finished()){if(!credited){progress.finish(round.independent);credited=true;}celebrate();return;}String previous=question.target;round.advance();hint=false;question=Learning.next(mode,progress.completed(),progress,random,previous);exercise(true);}
    private void celebrate(){page("done");title("Ein Stern für dich!","Du hast Milo bei seinem Abenteuer geholfen.");Picture fox=new Picture(this,"fox");content.addView(fox,new LinearLayout.LayoutParams(-1,dp(190)));TextView star=label("★",96,0xffe5ad36);star.setGravity(Gravity.CENTER);content.addView(star);TextView note=label("Jeder Versuch zählt.\nJetzt ist auch ein guter Moment für eine Pause.",20,INK);note.setGravity(Gravity.CENTER);content.addView(note);addButton("Zur Lichtung",()->home(false),true);addButton("Noch eine kleine Runde",()->start(mode),false);voice.say("Geschafft! Ein Abenteuerstern für dich. Du kannst jetzt auch eine Pause machen.");}
    private void explore(){page("explore");header("Deine Buchstaben");title("Kleine Zeichen, große Welt","Tippe auf einen Buchstaben und hör seinen Laut.");for(int i=0;i<Learning.known(progress.completed());i++){Learning.Sound sound=Learning.SOUNDS.get(i);LinearLayout tile=row();tile.setPadding(dp(12),dp(9),dp(12),dp(9));tile.setBackground(bg(Color.WHITE,0));tile.addView(new Picture(this,sound.picture),new LinearLayout.LayoutParams(dp(75),dp(75)));TextView t=label(sound.letter+"  "+sound.letter.toLowerCase(Locale.GERMAN),38,GREEN);bold(t);tile.addView(t);tile.setFocusable(true);tile.setContentDescription(sound.letter+", "+sound.example);tile.setOnClickListener(v->voice.sound(sound.letter,()->voice.say("Wie in "+sound.example)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);content.addView(tile,p);}}
    private void parentGate(){voice.stop();int a=7+random.nextInt(5),b=6+random.nextInt(6);EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setHint("Ergebnis");input.setPadding(dp(20),dp(12),dp(20),dp(12));AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Für Erwachsene").setMessage("Wie viel ist "+a+" + "+b+"?").setView(input).setPositiveButton("Öffnen",null).setNegativeButton("Zurück",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(input.getText().toString().trim().equals(String.valueOf(a+b))){dialog.dismiss();parents();}else input.setError("Bitte noch einmal rechnen.");}));dialog.show();}
    private void parents(){page("parents");updates.resume();header("Für Erwachsene");title("Mit Freude, ohne Eile","Die App ist ein spielerischer Einstieg. Begleitet die ersten Runden gemeinsam.");
        TextView info=label("Aktuelles Profil: "+progress.name()+"\nAbgeschlossene Runden: "+progress.rounds()+"\nAbenteuersterne: "+progress.stars()+"\nEingeführte Buchstaben: "+Learning.letters(progress.completed())+"\n\nNeue Buchstaben kommen nach jeweils drei Runden mit mindestens vier selbstständig gelösten Aufgaben hinzu. Ein gelöstes Rätsel nach einem Tipp zählt als Übung, aber nicht als selbstständig gelöst.\n\nDie Sternbelohnung gibt es für jede vollständige Runde. Keine Zeitlimits, täglichen Serien oder verlorenen Leben.",16,INK);content.addView(info);
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
