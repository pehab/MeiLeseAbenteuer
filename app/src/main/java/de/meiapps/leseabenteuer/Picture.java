package de.meiapps.leseabenteuer;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original code-drawn illustrations; all pictures are available offline and at any screen density. */
final class Picture extends View {
    private final Paint p=new Paint(3);private final Path path=new Path();private final String kind;
    Picture(Context context){this(context,"fox");}
    Picture(Context context,String kind){super(context);this.kind=kind;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    private void oval(Canvas c,float l,float t,float r,float b,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawOval(l,t,r,b,p);}
    private void rect(Canvas c,float l,float t,float r,float b,float radius,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,radius,radius,p);}
    private void line(Canvas c,float x,float y,float xx,float yy,int color,float width){p.setColor(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);}
    private void triangle(Canvas c,float a,float b,float d,float e,float f,float g,int color){path.rewind();path.moveTo(a,b);path.lineTo(d,e);path.lineTo(f,g);path.close();p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawPath(path,p);}
    private void eyes(Canvas c,float y){oval(c,36,y,42,y+7,0xff29453f);oval(c,60,y,66,y+7,0xff29453f);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float side=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-side)/2,(getHeight()-side)/2);c.scale(side/100,side/100);
        switch(kind){
            case "fox":
                triangle(c,20,19,48,34,28,60,0xffe88a43);triangle(c,80,19,72,60,52,34,0xffe88a43);oval(c,22,30,78,83,0xfff4a75a);
                triangle(c,25,55,50,70,50,87,0xfffff3d8);triangle(c,75,55,50,70,50,87,0xfffff3d8);eyes(c,48);triangle(c,44,66,56,66,50,73,0xff29453f);break;
            case "mouse":
                oval(c,15,19,43,49,0xffa5aaa9);oval(c,57,19,85,49,0xffa5aaa9);oval(c,21,25,37,42,0xffedb6ad);oval(c,63,25,79,42,0xffedb6ad);oval(c,26,33,74,83,0xffc9ceca);eyes(c,49);oval(c,45,65,55,74,0xffdd9e9b);line(c,18,66,39,69,0xff667a72,2);line(c,61,69,82,66,0xff667a72,2);break;
            case "monkey":
                oval(c,10,34,37,67,0xffa56b42);oval(c,63,34,90,67,0xffa56b42);oval(c,23,20,77,85,0xff975d3d);oval(c,30,37,70,79,0xffefd2a3);eyes(c,46);oval(c,36,61,64,79,0xfff6dfb6);line(c,43,70,57,70,0xff8c6546,3);break;
            case "oven":
                rect(c,20,17,80,88,7,0xff91aaa2);rect(c,25,23,75,36,3,0xffdfe7db);oval(c,31,27,37,33,0xff3e675a);oval(c,45,27,51,33,0xff3e675a);oval(c,59,27,65,33,0xff3e675a);rect(c,27,43,73,78,4,0xff35564e);line(c,32,47,68,47,0xffe7e7ce,3);oval(c,36,58,64,73,0xffdb9f5f);break;
            case "lamp":
                line(c,50,36,50,84,0xff5b7465,7);rect(c,30,82,70,88,4,0xff5b7465);triangle(c,36,20,22,54,50,54,0xffeeb55e);triangle(c,64,20,78,54,50,54,0xffeeb55e);rect(c,36,20,64,54,0,0xffffd681);oval(c,43,51,57,60,0xffffeeb0);break;
            case "hedgehog":
                for(int i=0;i<7;i++){float x=17+i*9;triangle(c,x,58,x+4,27+(i%2)*6,x+14,60,0xff876044);}oval(c,17,44,78,80,0xffad8055);oval(c,55,52,91,79,0xffe0ba83);oval(c,75,59,80,65,0xff29453f);oval(c,87,64,95,72,0xff29453f);break;
            case "nose":oval(c,24,14,76,90,0xfff3cfad);oval(c,35,38,39,43,0xff29453f);oval(c,61,38,65,43,0xff29453f);oval(c,41,46,61,68,0xffe4a389);oval(c,42,58,60,70,0xffd58b78);line(c,43,78,58,78,0xffba7a66,2);break;
            case "clock":
                rect(c,39,8,61,93,8,0xffba8052);oval(c,19,19,81,81,0xffedba66);oval(c,25,25,75,75,0xfffff5d8);line(c,50,50,50,34,0xff35574b,4);line(c,50,50,62,57,0xff35574b,4);oval(c,47,47,53,53,0xff35574b);break;
            case "fish":
                triangle(c,70,49,92,29,92,72,0xffed9e51);oval(c,13,28,80,76,0xff72aba7);oval(c,24,41,31,49,0xff24483f);triangle(c,42,31,57,17,64,33,0xff55918c);line(c,50,43,42,57,0xffe0e8c2,3);break;
            case "mother":case "grandmother":
                boolean old=kind.equals("grandmother");oval(c,25,16,75,78,old?0xffb5b8ad:0xff6f4c38);if(old)oval(c,58,8,78,29,0xffb5b8ad);
                rect(c,23,70,77,96,19,old?0xffa185b0:0xff73a78c);oval(c,31,27,69,74,0xfff2c9a1);eyes(c,43);
                if(old){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xff697a6e);c.drawCircle(39,48,9,p);c.drawCircle(63,48,9,p);p.setStyle(Paint.Style.FILL);line(c,48,48,54,48,0xff697a6e,2);}
                line(c,43,62,57,62,0xffb17e65,2);break;
            case "llama":
                rect(c,32,44,72,78,15,0xfff2e4c6);rect(c,56,21,71,64,6,0xfff2e4c6);rect(c,56,9,62,32,3,0xffd8c4a2);rect(c,69,10,75,34,3,0xffd8c4a2);oval(c,52,22,80,47,0xfff2e4c6);oval(c,70,32,74,37,0xff29453f);line(c,39,70,39,90,0xffd6bf99,6);line(c,65,70,65,90,0xffd6bf99,6);rect(c,36,49,58,66,3,0xffc98a6e);break;
            case "lemonade":
                rect(c,30,31,70,87,8,0xffa7d0c0);rect(c,34,45,66,81,5,0xffffce67);line(c,54,57,61,16,0xffe98f6a,4);line(c,61,16,74,16,0xffe98f6a,4);oval(c,17,34,41,58,0xffffdd77);oval(c,21,38,37,54,0xfffaf2b5);break;
            case "ufo":
                oval(c,31,20,69,62,0xff9dcebf);oval(c,14,44,86,69,0xff85a49a);oval(c,18,43,82,56,0xffd3c6a4);for(int i=0;i<4;i++)oval(c,27+i*13,55,33+i*13,61,0xffffd77e);triangle(c,36,69,23,94,77,94,0x44edd681);break;
            default:oval(c,25,25,75,75,0xffeeb55e);
        }
        c.restore();
    }
}
