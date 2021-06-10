package implementingServiceByTXY;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.service.UserService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserServiceImp implements UserService {
    @Override
    public void removeUser(int userId) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectUserPtmt=conn.prepareStatement("select * from tuser where userId=?");
                PreparedStatement removeUserPtmt=conn.prepareStatement("delete from tuser where userId=?");
        )
        {
            int userId_temp=-1;
            selectUserPtmt.setInt(1,userId);
            ResultSet set=selectUserPtmt.executeQuery();
            while (set.next()){
                userId_temp=set.getInt("userId");
            }
            if (userId_temp==-1){
                throw new EntityNotFoundException();
            }
            else{
                removeUserPtmt.setInt(1,userId);
                removeUserPtmt.executeUpdate();
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<User> getAllUsers() {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getAllUsersPtmt=conn.prepareStatement("select * from tuser");
        )
        {
            ArrayList<User> users=new ArrayList<>();
            ResultSet set=getAllUsersPtmt.executeQuery();
            int tos=-1;
            int userId_temp=-1;
            String fn="";
            String ln="";
            while (set.next()){
               userId_temp=set.getInt("userId");
               fn=set.getString("firstName");
               ln=set.getString("lastName");
               tos=set.getInt("tos");
               if (tos==1){
                   User ins=new Instructor();
                   ins.id=userId_temp;
                   ins.fullName=getFullName(fn,ln);
                   users.add(ins);
               }
               else{
                   User stu=new Student();
                   stu.id=userId_temp;
                   stu.fullName=getFullName(fn,ln);
                   users.add(stu);
               }
            }
            return users;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public User getUser(int userId) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectUserPtmt=conn.prepareStatement("select * from tuser where userId=?");
        )
        {
            int userId_temp=-1;
            String fn="";
            String ln="";
            int tos=-1;
            selectUserPtmt.setInt(1,userId);
            ResultSet set=selectUserPtmt.executeQuery();
            while (set.next()){
                userId_temp=set.getInt("userId");
                fn=set.getString("firstName");
                ln=set.getString("lastName");
                tos=set.getInt("tos");
            }
            if (userId_temp==-1) {
                throw new EntityNotFoundException();
            }
            if (tos==1){
                User ins=new Instructor();
                ins.id=userId_temp;
                ins.fullName=getFullName(fn,ln);
                return ins;
            }
            else{
                User stu=new Student();
                stu.id=userId_temp;
                stu.fullName=getFullName(fn,ln);
                return stu;
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public static String getFullName(String fn,String ln){
        boolean fnb=false;
        for (int i=0;i<fn.length();i++){
            fnb=(fn.charAt(i)+"").matches("[a-zA-Z\\s]");
            if (!fnb){
                return fn+ln;
            }
        }
        boolean lnb=false;
        for (int i=0;i<ln.length();i++){
            lnb=(ln.charAt(i)+"").matches("[a-zA-Z\\s]");
            if (!lnb){
                return fn+ln;
            }
        }
        return fn+" "+ln;
    }

    public static void main(String[] args) {
        /*UserServiceImp oimp=new UserServiceImp();
        List<User> list=oimp.getAllUsers();
        for (User a:list){
            System.out.println(a.fullName);
        }*/

        System.out.println(getFullName("jio ji","cher"));
    }


}
