package implementingServiceByTXY;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MajorServiceImp implements MajorService {
    @Override
    public synchronized int addMajor(String name, int departmentId) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement idptmt=conn.prepareStatement("select nextval('major_majorid_seq')");
                PreparedStatement insptmt=conn.prepareStatement("insert into major " +
                        "(majorName,departmentId) VALUES(?,?)");
                PreparedStatement setptmt=conn.prepareStatement("select setval('major_majorid_seq',?)");
                )
        {
            insptmt.setString(1,name);
            insptmt.setInt(2,departmentId);
            insptmt.executeUpdate();
            int id=0;
            ResultSet nowid=idptmt.executeQuery();
            while (nowid.next()){
                id=nowid.getInt(1);
            }
            setptmt.setInt(1,id-1);
            setptmt.executeQuery();
            return id-1;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeMajor(int majorId) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectMajorPtmt=conn.prepareStatement("select * from major where majorId=?");
                PreparedStatement removeMajorPtmt=conn.prepareStatement("delete from major where majorId=?");
        )
        {
            int majorId_temp=-1;
            selectMajorPtmt.setInt(1,majorId);
            ResultSet set=selectMajorPtmt.executeQuery();
            while (set.next()){
                majorId_temp=set.getInt("majorId");
            }
            if (majorId_temp==-1){
                throw new EntityNotFoundException();
            }
            else{
                removeMajorPtmt.setInt(1,majorId);
                removeMajorPtmt.executeUpdate();
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<Major> getAllMajors() {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectAllMajorPtmt=conn.prepareStatement(
                        "select *" +
                                "from major natural join department"
                )
                )
        {
            ArrayList<Major> list=new ArrayList<>();
            ResultSet set=selectAllMajorPtmt.executeQuery();
            while(set.next()){
                Major ma=new Major();
                Department dep=new Department();
                ma.id=set.getInt("majorId");
                ma.name=set.getString("majorName");
                dep.id=set.getInt("departmentId");
                dep.name=set.getString("departmentName");
                ma.department=dep;
                list.add(ma);
            }
            return list;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public Major getMajor(int majorId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectMajorPtmt=conn.prepareStatement(
                        "select *\n" +
                        "from ((\n" +
                        "        select *\n" +
                        "        from major\n" +
                        "        where majorId=?\n" +
                        "    ) as major_info natural join department ) as T"
                );
        )
        {
            int majorId_temp=-1;
            Major ma=new Major();
            Department dep=new Department();
            selectMajorPtmt.setInt(1,majorId);
            ResultSet set=selectMajorPtmt.executeQuery();
            while (set.next()){
                majorId_temp=set.getInt("majorId");
                ma.name=set.getString("majorName");
                dep.id=set.getInt("departmentId");
                dep.name=set.getString("departmentName");
            }
            if (majorId_temp==-1){
                throw new EntityNotFoundException();
            }
            ma.id=majorId_temp;
            ma.department=dep;
            return ma;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement instPtmt=conn.prepareStatement("insert into core(majorId,courseId,coe) values(?,?,1)");
                )
        {
            instPtmt.setInt(1,majorId);
            instPtmt.setString(2,courseId);
            instPtmt.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }

    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement instPtmt=conn.prepareStatement("insert into core(majorId,courseId,coe) values(?,?,2)");
        )
        {
            instPtmt.setInt(1,majorId);
            instPtmt.setString(2,courseId);
            instPtmt.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    public static void main(String[] args) {
        MajorServiceImp imp=new MajorServiceImp();
        imp.addMajor("火焰魔法",1);
        imp.addMajor("黑魔法",1);
        imp.addMajor("治疗魔法",1);
      /*  List<Major> list=imp.getAllMajors();
        for (Major a:list){
            System.out.println(a.id+" "+a.name+" "+a.department.id+" "+a.department.name);
        }

        Major b=imp.getMajor(3);
        System.out.println(b.id+" "+b.name+" "+b.department.id+" "+b.department.name);*/

        CourseServiceImp cimp=new CourseServiceImp();
        cimp.addCourse("MA1","火焰魔法导论",3,
                16, Course.CourseGrading.HUNDRED_MARK_SCORE,null);
        cimp.addCourse("MA3","治疗魔法导论",3,
                20, Course.CourseGrading.HUNDRED_MARK_SCORE,null);


       /* cimp.addCourse("MA15","火焰魔法最高阶",2,
                30, Course.CourseGrading.HUNDRED_MARK_SCORE,null);*/
        imp.addMajorElectiveCourse(1,"MA3");
    }
}
