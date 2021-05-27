package Tools;

import java.util.ArrayList;
import java.util.List;

public class IntTox {
    public static void main(String[] args) {
        short a=1;
        short b=2;
        short c=3;
        short d=4;
        short e=5;
        short f=6;

        /*int tt=MultiChooseIntGenerator.weekIntGenerator(List.of(a,c,d,e,f));
        System.out.println(Integer.toBinaryString(tt));
        List<Short> list=intToList(tt);
        for (short ss:list){
            System.out.println(ss);
        }*/

        int aat=MultiChooseIntGenerator.classTimeIntGenerator(c,f);
        System.out.println(getBegin(aat));
        System.out.println(getEnd(aat));
    }

    public static List<Short> intToList(int a){
        String bin=Integer.toBinaryString(a);
        List<Short> list=new ArrayList<>();
        for (int i=bin.length()-2;i>=0;i--){
            if (bin.charAt(i)=='1'){
                list.add((short)(bin.length()-i-1));
            }
        }
        return list;
    }

    public static short getBegin(int a){
        String bin=Integer.toBinaryString(a);
        int len=bin.length();
        short ans=0;
        for (short i=0;i<bin.length();i++){
            if (bin.charAt(len-1-i)=='1'){
                ans= i;
                break;
            }
        }
        return ans;
    }

    public static short getEnd(int a){
        String bin=Integer.toBinaryString(a);
        return (short)(bin.length()-1);
    }
}
