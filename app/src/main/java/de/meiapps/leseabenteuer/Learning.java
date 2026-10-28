package de.meiapps.leseabenteuer;

import java.util.*;

/** Android-free learning content and progression. Never generates words from unknown letters. */
public final class Learning {
    private Learning() { }
    public enum Mode { LISTEN, INITIAL, BLEND, READ, ALPHABET, COUNT, NUMBER }
    public static final class Sound {
        public final String letter,example,picture;
        Sound(String letter,String example,String picture){this.letter=letter;this.example=example;this.picture=picture;}
    }
    public static final List<Sound> SOUNDS=Collections.unmodifiableList(Arrays.asList(
        new Sound("M","Maus","mouse"),new Sound("A","Affe","monkey"),new Sound("O","Ofen","oven"),
        new Sound("L","Lampe","lamp"),new Sound("I","Igel","hedgehog"),new Sound("N","Nase","nose"),
        new Sound("U","Uhr","clock"),new Sound("F","Fisch","fish")));
    public static final class Word {
        public final String text,picture;
        Word(String text,String picture){this.text=text;this.picture=picture;}
    }
    public static final List<Word> WORDS=Collections.unmodifiableList(Arrays.asList(
        new Word("Mama","mother"),new Word("Oma","grandmother"),new Word("Lama","llama"),
        new Word("Milo","fox"),new Word("Limo","lemonade"),new Word("Ufo","ufo")));
    public static final String ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ";
    public static final String[] NUMBER_NAMES={"null","eins","zwei","drei","vier","fünf","sechs","sieben","acht","neun","zehn"};
    public static final String[] LETTER_EXAMPLES={"Affe","Ball","Clown","Dach","Elefant","Fisch","Gans","Haus","Igel","Jacke","Katze","Lampe","Maus","Nase","Ofen","Panda","Qualle","Rose","Sonne","Tiger","Uhr","Vogel","Wal","Xylophon","Ypsilon","Zebra","Äpfel","Öl","Überraschung"};
    public static boolean isNumber(Mode mode){return mode==Mode.COUNT||mode==Mode.NUMBER;}
    public static int numberLimit(int completed){return Math.min(10,5+Math.max(0,completed)/3);}
    private static final String[] SYLLABLES={"MA","MO","LA","LO","MI","LI","NA","NO","NI","MU","LU","NU","FA","FO","FI","FU"};
    public static int known(int completed){return Math.min(SOUNDS.size(),3+Math.max(0,completed)/3);}
    public static String letters(int completed){StringBuilder s=new StringBuilder();for(int i=0;i<known(completed);i++)s.append(SOUNDS.get(i).letter);return s.toString();}
    public static boolean readable(String text,String known){for(char c:text.toUpperCase(Locale.GERMAN).toCharArray())if(known.indexOf(c)<0)return false;return true;}
    public static List<Word> words(int completed){List<Word> result=new ArrayList<>();for(Word w:WORDS)if(readable(w.text,letters(completed)))result.add(w);return result;}
    public static List<String> syllables(int completed){List<String> r=new ArrayList<>();for(String s:SYLLABLES)if(readable(s,letters(completed)))r.add(s);return r;}
    public static final class Stats {
        public int seen,independent,needsHelp;
        public void record(boolean assisted){seen=Math.min(100000,seen+1);if(assisted)needsHelp=Math.min(100000,needsHelp+1);else{independent=Math.min(100000,independent+1);needsHelp=Math.max(0,needsHelp-1);}}
        public int weight(){return Math.max(1,6-Math.min(5,independent))+Math.min(8,needsHelp*2);}
    }
    public interface Memory { Stats get(String key); }
    public static final class Question {
        public final Mode mode;public final String key,target,picture;public final List<String> options;
        Question(Mode mode,String target,String picture,List<String> options){this.mode=mode;this.target=target;this.picture=picture;this.options=Collections.unmodifiableList(new ArrayList<>(options));key=mode.name()+":"+target;}
    }
    public static Question next(Mode mode,int completed,Memory memory,Random random,String previous){
        List<String> candidates=new ArrayList<>();Map<String,String> pictures=new HashMap<>();
        if(isNumber(mode)){for(int n=1;n<=numberLimit(completed);n++)candidates.add(String.valueOf(n));}
        else if(mode==Mode.ALPHABET){for(char letter:ALPHABET.toCharArray())candidates.add(String.valueOf(letter));}
        else if(mode==Mode.BLEND)candidates.addAll(syllables(completed));
        else if(mode==Mode.READ){for(Word w:words(completed)){candidates.add(w.text);pictures.put(w.text,w.picture);}}
        else for(int i=0;i<known(completed);i++){Sound s=SOUNDS.get(i);candidates.add(s.letter);pictures.put(s.letter,s.picture);}
        if(candidates.isEmpty())throw new IllegalArgumentException("No content for current progress");
        List<String> targets=new ArrayList<>(candidates);if(targets.size()>1)targets.remove(previous);
        int total=0;for(String c:targets)total+=memory.get(mode.name()+":"+c).weight();int pick=random.nextInt(total);String target=targets.get(0);
        for(String c:targets){pick-=memory.get(mode.name()+":"+c).weight();if(pick<0){target=c;break;}}
        List<String> options=new ArrayList<>(candidates);options.remove(target);Collections.shuffle(options,random);if(options.size()>2)options=new ArrayList<>(options.subList(0,2));options.add(target);Collections.shuffle(options,random);
        return new Question(mode,target,pictures.get(target),options);
    }
    public static Sound sound(String letter){for(Sound s:SOUNDS)if(s.letter.equals(letter))return s;throw new IllegalArgumentException(letter);}
    public static Word word(String text){for(Word w:WORDS)if(w.text.equals(text))return w;throw new IllegalArgumentException(text);}
    public static final class Round {
        public static final int LENGTH=6;
        public int answered,independent;public boolean assisted,locked;
        public boolean answer(boolean correct){if(locked)return false;if(!correct){assisted=true;return false;}locked=true;answered++;if(!assisted)independent++;return true;}
        public void advance(){locked=false;assisted=false;}
        public boolean finished(){return answered>=LENGTH;}
    }
}
