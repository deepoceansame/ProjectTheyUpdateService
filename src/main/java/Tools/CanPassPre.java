package Tools;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CanPassPre {
    public static void main(String[] args) {
        String[] s=getPreName("(((I+J)*F)*((K*L)+H)*D)");
        for (String ss:s){
            System.out.print(ss+" ");
        }
    }

    //"(高等数学（上）A 或者 高等数学（上） 或者 数学分析I) 并且 (线性代数I-A 或者 线性代数I) 并且 电路基础"
    //(高等数学（下）A 或者 高等数学（下） 或者 数学分析II) 并且 (大学物理A(下) 或者 大学物理 B(下) 或者 大学物理A（下）) 并且 (化学原理 A 或者 化学原理 B 或者 化学原理)
    /*
    遇到左括号
    遇到右括号
    遇到空格
    遇到字
     */
    public static String[] getPreName(String s){
        s=s.replaceAll("（","(");
        s=s.replaceAll("）",")");
        s=s.replaceAll("或者","+");
        s=s.replaceAll("并且","*");
        ArrayList<String> preName=new ArrayList<>();
        char a=' ';
        boolean frontIsWord=false;
        boolean paraInName=false;
        StringBuilder sBuilder = new StringBuilder("");
        for (int i = 0; i< s.length(); i++) {
            a= s.charAt(i);
            if (a=='('){
                if (frontIsWord){
                    sBuilder.append(a);
                    paraInName=true;
                    frontIsWord=false;
                }
            }
            else if (a==')'){
                if (paraInName){
                    sBuilder.append(a);
                    paraInName=false;
                    frontIsWord=true;
                }
            }
            else if (a==' '){
                if (Character.isUpperCase(s.charAt(i+1))){
                    sBuilder.append(a);
                    frontIsWord=true;
                }
                else {
                    frontIsWord=false;
                }
            }
            else if (a=='*' || a=='+'){
                preName.add(sBuilder.toString());
                sBuilder=new StringBuilder("");
                frontIsWord=false;
            }
            else{
                frontIsWord=true;
                sBuilder.append(a);
            }
        }
        if (!sBuilder.toString().equals(""))
        preName.add(sBuilder.toString());
        String[] arr=new String[preName.size()];
        for (int i=0;i<preName.size();i++){
            arr[i]=preName.get(i);
        }
        return arr;
    }

    //
    public static boolean canLearnThisCourse(String pre,ArrayList<String> courseLearned){
        String[] preName=getPreName(pre);
        boolean[] eachPreAns=new boolean[preName.length];
        for (int i=0;i< eachPreAns.length;i++){
            if(courseLearned.contains(preName[i]))
            eachPreAns[i]=true;
        }
        for (int i=0;i< eachPreAns.length;i++){
            if (eachPreAns[i]){
                pre=pre.replace(preName[i],"1");
            }
            else{
                pre=pre.replace(preName[i],"0");
            }
        }
        String postfix=toPostfix(pre);
        Stack<Integer> stack=new Stack<>();
        char c=' ';
        for (int i=0;i<postfix.length();i++){
            c=postfix.charAt(i);
            if (c=='1'||c=='0'){
                stack.push(c-48);
            }
            else if (c=='+'){
                stack.push(stack.pop()|stack.pop());
            }
            else if (c=='*'){
                stack.push(stack.pop()&stack.pop());
            }
        }
        if (stack.pop()==0)
        return false;
        else
            return true;
    }

    public static String toPostfix(String s){
        s=s.replaceAll("或者","+");
        s=s.replaceAll("并且","*");
        s=s.replaceAll("（","(");
        s=s.replaceAll("）",")");
        Stack<Character> stack=new Stack<>();
        StringBuilder stringBuilder=new StringBuilder();
        char c=' ';
        for (int i=0;i<s.length();i++){
            c=s.charAt(i);
            if (c=='('){
                stack.push(c);
            }
            else if (c=='1'||c=='0'){
                stringBuilder.append(c);
            }
            else if (c=='+'||c=='*'){
                if (!stack.isEmpty()&&stack.peek()==c){
                    stringBuilder.append(stack.pop());
                }
                stack.push(c);
            }
            else if (c==')'){
                while(stack.peek()!='('){
                    stringBuilder.append(stack.pop());
                }
                stack.pop();
            }
        }
        while (!stack.isEmpty()){
            stringBuilder.append(stack.pop());
        }
        return stringBuilder.toString();
    }



    //如果是一个汉字返回true，否则返回false
    public static boolean checkCharCN(char c){
        String s = String.valueOf(c);
        String  regex = "[\u4e00-\u9fa5]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);
        return m.matches();
    }
}
