package de.meiapps.leseabenteuer;

import android.content.Context;
import android.graphics.*;
import android.view.*;

/** Original scalable game art. No downloaded illustrations or font-dependent emoji. */
final class PlayArt extends View {
    private final Paint p=new Paint(3);private final Path path=new Path();
    private final String kind;private final int value;
    PlayArt(Context context){this(context,"star",0);}
    PlayArt(Context context,String kind,int value){super(context);this.kind=kind;this.value=value;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    private void fill(int color){p.setStyle(Paint.Style.FILL);p.setColor(color);}
    private void oval(Canvas c,float x,float y,float rx,float ry,int color){fill(color);c.drawOval(x-rx,y-ry,x+rx,y+ry,p);}
    private void line(Canvas c,float x,float y,float xx,float yy,int color,float width){p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);c.drawLine(x,y,xx,yy,p);p.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String text,float x,float y,float size,int color){fill(color);p.setTypeface(Typeface.create("sans-serif-rounded",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);c.drawText(text,x,y,p);}
    private void star(Canvas c,float x,float y,float radius,int color){path.rewind();for(int n=0;n<10;n++){double a=-Math.PI/2+n*Math.PI/5;float r=n%2==0?radius:radius*.46f;float xx=x+(float)Math.cos(a)*r,yy=y+(float)Math.sin(a)*r;if(n==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}path.close();fill(color);c.drawPath(path,p);}
    private void friend(Canvas c,float x,float y,int n,float scale){c.save();c.translate(x,y);c.scale(scale,scale);int[] colors={0xffed8845,0xffa876d6,0xffe9ad57,0xff62b6de,0xffee93b4,0xff83bb67};int color=colors[n%6];
        if(n%3==0){oval(c,-13,-19,8,14,color);oval(c,13,-19,8,14,color);}else{oval(c,-18,-10,11,11,color);oval(c,18,-10,11,11,color);}
        oval(c,0,0,25,22,color);oval(c,-9,0,3,4,0xff303d56);oval(c,9,0,3,4,0xff303d56);oval(c,0,10,9,6,0xffffefd7);oval(c,0,7,3,2,0xff303d56);oval(c,-16,8,4,2,0x66eb4d72);oval(c,16,8,4,2,0x66eb4d72);c.restore();}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);Canvas c=canvas;c.save();c.scale(getWidth()/100f,getHeight()/100f);
        if(kind.equals("map")){map(c);c.restore();return;}
        if(kind.equals("count")){int count=Math.max(1,Math.min(10,value));for(int n=0;n<count;n++){float x=18+(n%5)*16,y=count<=5?52:30+(n/5)*40;oval(c,x,y,6,12,0xffef6b67);oval(c,x-2,y-3,1.5f,3,0x99ffffff);line(c,x,y-12,x+3,y-17,0xff4b9d5e,2);}c.restore();return;}
        if(kind.equals("friend")){friend(c,50,55,value,1.55f);c.restore();return;}
        int ink=0xff3b4261;
        switch(kind){
            case "listen":
                oval(c,50,49,26,32,0xfffee8bb);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);p.setColor(ink);c.drawArc(29,17,73,76,-140,260,false,p);c.drawArc(39,32,62,62,-130,230,false,p);line(c,53,62,46,79,ink,6);line(c,78,34,86,29,0xffea8b3a,4);line(c,80,50,91,50,0xffea8b3a,4);break;
            case "initial":friend(c,50,53,0,1.3f);break;
            case "blend":oval(c,33,52,23,26,0xffa3daf0);oval(c,68,52,23,26,0xfff9c66c);text(c,"M",32,63,29,ink);text(c,"A",68,63,29,ink);break;
            case "read":fill(0xfff58ba2);c.drawRoundRect(17,23,83,79,8,8,p);fill(0xfffff4d7);c.drawRoundRect(23,19,77,73,5,5,p);line(c,50,25,50,69,0xffe2c196,3);text(c,"a",37,55,29,ink);star(c,64,44,10,0xffffbf4e);break;
            case "alphabet":oval(c,33,39,23,24,0xff7ccbb4);oval(c,65,62,23,24,0xfffac566);text(c,"B",33,51,33,ink);text(c,"b",65,73,33,ink);break;
            case "number":text(c,"1",27,63,44,0xffe76c71);text(c,"2",54,59,47,0xff5b99d0);text(c,"3",80,65,40,0xff9b77c5);break;
            case "basket":for(int n=0;n<3;n++)oval(c,33+n*17,36,9,12,0xffef6b67);fill(0xffdda259);c.drawRoundRect(18,43,82,82,12,12,p);line(c,28,55,72,55,0xffffdd91,5);line(c,32,69,68,69,0xffffdd91,5);break;
            case "play":path.moveTo(35,19);path.lineTo(78,50);path.lineTo(35,81);path.close();fill(0xffffffff);c.drawPath(path,p);break;
            case "home":path.moveTo(15,46);path.lineTo(50,16);path.lineTo(85,46);path.close();fill(0xfff6ab55);c.drawPath(path,p);fill(0xfffff2d0);c.drawRoundRect(25,43,75,85,5,5,p);fill(0xff74b8b3);c.drawRoundRect(43,59,59,85,4,4,p);break;
            case "repeat":p.setColor(ink);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);c.drawArc(23,22,78,79,35,295,false,p);line(c,76,23,78,44,ink,6);line(c,78,44,58,39,ink,6);break;
            case "help":oval(c,50,42,24,27,0xffffd45b);fill(0xff8e91b1);c.drawRoundRect(39,67,61,82,5,5,p);line(c,45,46,51,56,0xffeb9a36,4);line(c,51,56,58,45,0xffeb9a36,4);line(c,18,20,11,13,0xffe4a93a,4);line(c,82,20,89,13,0xffe4a93a,4);break;
            case "next":line(c,20,50,78,50,0xffffffff,9);line(c,58,27,81,50,0xffffffff,9);line(c,81,50,58,73,0xffffffff,9);break;
            case "star":star(c,50,50,39,0xffffc64f);oval(c,41,47,2.5f,3.5f,ink);oval(c,59,47,2.5f,3.5f,ink);line(c,44,60,56,60,ink,3);break;
            case "album":fill(0xffb395e1);c.drawRoundRect(20,18,80,85,8,8,p);line(c,30,20,30,82,0xff8d6bc0,4);star(c,55,49,19,0xffffdf78);break;
            default:text(c,"?",50,67,55,ink);
        }c.restore();
    }
    private void map(Canvas c){
        fill(0xffc6edfc);c.drawRect(0,0,100,100,p);oval(c,85,12,8,7,0xffffdb70);oval(c,19,17,17,5,0xeaffffff);oval(c,68,26,19,5,0xeaffffff);
        oval(c,18,67,66,35,0xffb2d987);oval(c,93,75,64,40,0xff8dcc91);oval(c,50,98,80,27,0xff6ab887);oval(c,82,63,18,12,0xff75c9df);
        for(int n=0;n<10;n++){float x=(n*31+7)%100,y=43+(n*17)%49;line(c,x,y,x,y-12,0xff7e9961,2);oval(c,x,y-15,5,9,0xff56a278);}
        Path road=new Path();road.moveTo(14,83);road.cubicTo(95,93,5,50,77,38);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setColor(0xfff9d99b);p.setStrokeCap(Paint.Cap.ROUND);c.drawPath(road,p);p.setStyle(Paint.Style.FILL);
        float[][] stops={{14,83},{38,82},{52,71},{39,60},{48,47},{77,38}};
        int step=(value/4)%6;for(int n=0;n<6;n++){float x=stops[n][0],y=stops[n][1];oval(c,x,y,5,4,n<=step?0xfffff2ca:0xffb3b6a6);if(n<step)star(c,x,y,3,0xfff2b32f);else if(n==step){oval(c,x,y,3,2.5f,0xffe98a47);}}
        friend(c,stops[step][0],stops[step][1]-8,0,.30f);
        for(int n=0;n<Math.min(6,value/4);n++)friend(c,12+n*15,30+(n%2)*6,n,.26f);
        star(c,78,37,4,0xffffbf44);for(int n=0;n<10;n++){float x=(n*23+11)%100,y=63+(n*13)%33;oval(c,x,y,1.2f,.8f,0xffffe98a);}
    }
}
