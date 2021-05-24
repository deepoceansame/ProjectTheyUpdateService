package Tools;

import java.util.ArrayList;

public class MultiChooseIntGenerator {
    public static void main(String[] args) {
        int[] weekList1=new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
        int[] weekList2=new int[]{14,16};
        System.out.println(weekIntGenerator(weekList1));
        String s1="11-11";
        String s2="3-4";
        System.out.println(classTimeIntGenerator(s2));
    }

    public static int weekIntGenerator(int[] weekList){
        int ans=0;
        for (int i=0;i<weekList.length;i++){
            ans+=Math.pow(2,weekList[i]);
        }
        return ans;
    }

    public static int classTimeIntGenerator(String s){
        int ans=0;
        String[] ss=s.split("-");
        int a=Integer.parseInt(ss[0]);
        int b=Integer.parseInt(ss[1]);
        for (int i=a;i<b+1;i++){
            ans+=Math.pow(2,i);
        }

        return ans;
    }
}
