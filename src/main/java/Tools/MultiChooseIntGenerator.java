package Tools;

import java.util.ArrayList;
import java.util.List;

public class MultiChooseIntGenerator {
    public static void main(String[] args) {
        short a=1;
        short b=2;
        short c=3;
        short d=4;
        short e=5;
        short f=6;
        short g=7;
        short h=8;
        short i=9;
        short j=10;
        short k=11;
        short z=20;
        List<Short> list=List.of(a,b,c,d,e,f,g,h);
        System.out.println(weekIntGenerator(list));
        System.out.println(classTimeIntGenerator(z,z));
    }

    public static int weekIntGenerator(List<Short> weekList){
        int ans=0;
        for (short a:weekList){
            ans+=Math.pow(2,a);
        }
        return ans;
    }

    public static int classTimeIntGenerator(short st,short en){
        int ans=0;
        for (short i=st;i<en+1;i++){
            ans+=Math.pow(2,i);
        }
        return ans;
    }
}
