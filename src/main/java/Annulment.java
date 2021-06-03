import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Annulment {
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getPreStringPtmt=conn.prepareStatement("select pre from course where courseId=?");
                PreparedStatement getPassedCourseListPtmt=conn.prepareStatement("select courseId " +
                        "from enroll natural join section natural join course " +
                        "where (grade=-1 or grade>=60) and studentId=?");
                PreparedStatement studentExiPtmt=conn.prepareStatement("select exists(select studentId from student where studentId=?)");
                PreparedStatement courseExiPtmt=conn.prepareStatement("select exists(select courseId from course where courseId=?)");
                PreparedStatement executeGetAnsPtmt=conn.prepareStatement("select getAns(?,?)");
                PreparedStatement getPreIdPtmt=conn.prepareStatement("select preId from course where courseId=?");
        )
        {
            studentExiPtmt.setInt(1,studentId);
            ResultSet set=studentExiPtmt.executeQuery();
            boolean pass=false;
            while (set.next()){
                pass=set.getBoolean(1);
            }
            if (!pass){
                throw new EntityNotFoundException();
            }
            courseExiPtmt.setString(1,courseId);
            set=courseExiPtmt.executeQuery();
            pass=false;
            while (set.next()){
                pass=set.getBoolean(1);
            }
            if (!pass){
                throw new EntityNotFoundException();
            }

            getPreStringPtmt.setString(1,courseId);
            set=getPreStringPtmt.executeQuery();
            String pre="";
            while (set.next()){
                pre=set.getString(1);
            }
            if (pre==null){
                return true;
            }
            ArrayList<String> coursePassed=new ArrayList<>();
            getPassedCourseListPtmt.setInt(1,studentId);
            set=getPassedCourseListPtmt.executeQuery();
            while (set.next()){
                coursePassed.add(set.getString(1));
            }
            return Tools.CanPassPre.canLearnThisCourse(pre,coursePassed);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
