package de.meiapps.leseabenteuer;
import java.util.*;
public final class LearningTest {
 static int checks;static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
 public static void main(String[] args){
  Map<String,Learning.Stats> stats=new HashMap<>();Learning.Memory memory=k->stats.computeIfAbsent(k,x->new Learning.Stats());Random r=new Random(81);
  check(Learning.known(0)==3);check(Learning.known(3)==4);check(Learning.known(999)==8);check(Learning.known(-1)==3);
  for(int progress=0;progress<22;progress++)for(Learning.Mode mode:Learning.Mode.values()){
   String previous=null;for(int n=0;n<40;n++){Learning.Question q=Learning.next(mode,progress,memory,r,previous);check(q.options.contains(q.target));check(new HashSet<>(q.options).size()==q.options.size());check(!q.target.equals(previous));
    for(String option:q.options){
     if(Learning.isNumber(mode)){int value=Integer.parseInt(option);check(value>=1&&value<=Learning.numberLimit(progress));}
     else if(mode==Learning.Mode.ALPHABET)check(option.length()==1&&Learning.ALPHABET.contains(option));
     else check(Learning.readable(option,Learning.letters(progress)));
    }
    previous=q.target;
   }
  }
  check(Learning.numberLimit(0)==5);check(Learning.numberLimit(15)==10);check(Learning.numberLimit(100)==10);
  check(Learning.ALPHABET.length()==29);check(Learning.LETTER_EXAMPLES.length==Learning.ALPHABET.length());
  Set<String> alphabet=new HashSet<>();for(int n=0;n<500;n++)alphabet.add(Learning.next(Learning.Mode.ALPHABET,0,memory,r,null).target);check(alphabet.size()==29);
  Learning.Stats s=new Learning.Stats();s.record(true);check(s.independent==0&&s.needsHelp==1);int hard=s.weight();s.record(false);check(s.independent==1&&s.weight()<hard);
  Learning.Round round=new Learning.Round();check(!round.answer(false));check(round.answer(true));check(!round.answer(true));check(round.answered==1&&round.independent==0);
  for(int i=1;i<6;i++){round.advance();check(round.answer(true));}check(round.finished());check(round.independent==5);
  System.out.println("PASS: "+checks+" content, progression and retry assertions");
 }
}
