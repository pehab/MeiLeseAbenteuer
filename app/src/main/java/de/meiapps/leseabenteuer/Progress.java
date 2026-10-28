package de.meiapps.leseabenteuer;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

final class Progress implements Learning.Memory {
    private final SharedPreferences prefs;private final Map<String,Learning.Stats> cache=new HashMap<>();
    private int profile;
    Progress(Context context){prefs=context.getSharedPreferences("learning-v1",0);profile=Math.max(0,Math.min(2,prefs.getInt("active",0)));}
    int profile(){return profile;}
    void profile(int id){if(id<0||id>2)throw new IllegalArgumentException();profile=id;cache.clear();prefs.edit().putInt("active",id).apply();}
    String prefix(){return "p"+profile+".";}
    String name(){return prefs.getString(prefix()+"name","Leseheld "+(profile+1));}
    void name(String value){String n=value.trim();if(!n.isEmpty())prefs.edit().putString(prefix()+"name",n.substring(0,Math.min(24,n.length()))).apply();}
    int rounds(){return Math.max(0,prefs.getInt(prefix()+"rounds",0));}
    int completed(){return Math.max(0,prefs.getInt(prefix()+"independentRounds",0));}
    int stars(){return Math.max(0,prefs.getInt(prefix()+"stars",0));}
    public Learning.Stats get(String key){Learning.Stats stats=cache.get(key);if(stats!=null)return stats;stats=new Learning.Stats();stats.seen=Math.max(0,prefs.getInt(prefix()+key+".seen",0));stats.independent=Math.max(0,prefs.getInt(prefix()+key+".independent",0));stats.needsHelp=Math.max(0,prefs.getInt(prefix()+key+".help",0));cache.put(key,stats);return stats;}
    void record(String key,boolean assisted){Learning.Stats s=get(key);s.record(assisted);prefs.edit().putInt(prefix()+key+".seen",s.seen).putInt(prefix()+key+".independent",s.independent).putInt(prefix()+key+".help",s.needsHelp).apply();}
    void finish(int independent){prefs.edit().putInt(prefix()+"rounds",rounds()+1).putInt(prefix()+"stars",stars()+1).putInt(prefix()+"independentRounds",completed()+(independent>=4?1:0)).apply();}
    int numberCompleted(){return Math.max(0,prefs.getInt(prefix()+"numberRounds",0));}
    void finish(Learning.Mode mode,int independent){
        if(mode.ordinal()<4){finish(independent);return;}
        SharedPreferences.Editor edit=prefs.edit().putInt(prefix()+"rounds",rounds()+1).putInt(prefix()+"stars",stars()+1);
        if(Learning.isNumber(mode)&&independent>=4)edit.putInt(prefix()+"numberRounds",numberCompleted()+1);
        edit.apply();
    }
    void reset(){SharedPreferences.Editor e=prefs.edit();for(String key:prefs.getAll().keySet())if(key.startsWith(prefix()))e.remove(key);e.apply();cache.clear();}
}
