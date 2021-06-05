package implementingServiceByTXY;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import javax.swing.text.StyleContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SemesterServiceImp implements SemesterService {

    @Override
    public int addSemester(String name, Date begin, Date end) {
       try(
               Connection conn= SQLDataSource.getInstance().getSQLConnection();
               PreparedStatement addSemesterPtmt=conn.prepareStatement("insert into semester(name,semBegin,semEnd) " +
                       "values(?,?,?)");
               PreparedStatement idptmt=conn.prepareStatement("select nextval('semester_semesterid_seq')");
               PreparedStatement setptmt=conn.prepareStatement("select setval('semester_semesterid_seq',?)");
               PreparedStatement noSemesterOverlapPtmt=conn.prepareStatement("select not exists(select null from semester where not (semBegin>? or semEnd<?)  )")
               )
       {
            if (end.before(begin)){
                throw new IntegrityViolationException();
            }

            boolean noOverlap=false;
            noSemesterOverlapPtmt.setDate(1,end);
            noSemesterOverlapPtmt.setDate(2,begin);
            ResultSet set=noSemesterOverlapPtmt.executeQuery();
            while (set.next()){
                noOverlap=set.getBoolean(1);
            }
            if (!noOverlap){
                throw new IntegrityViolationException();
            }
            addSemesterPtmt.setString(1,name);
            addSemesterPtmt.setDate(2,begin);
            addSemesterPtmt.setDate(3,end);
            addSemesterPtmt.executeUpdate();
           int id=0;
           ResultSet nowid=idptmt.executeQuery();
           while (nowid.next()){
               id=nowid.getInt(1);
           }
           setptmt.setInt(1,id-1);
           setptmt.executeQuery();
           return id-1;
       }
       catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
       }
    }

    @Override
    public void removeSemester(int semesterId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectSemesterPtmt=conn.prepareStatement("select * from semester where semesterId=?");
                PreparedStatement removeSemesterPtmt=conn.prepareStatement("delete from semester where semesterId=?");
                )
        {
            selectSemesterPtmt.setInt(1,semesterId);
            int semesterid=-1;
            ResultSet set=selectSemesterPtmt.executeQuery();
            while (set.next()){
                semesterid=set.getInt(1);
            }

            if (semesterid==-1){
                throw new EntityNotFoundException();
            }
            else{
                removeSemesterPtmt.setInt(1,semesterId);
                removeSemesterPtmt.executeUpdate();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Semester> getAllSemesters() {
        try (
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getAllSemestersPtmt=conn.prepareStatement("select * from semester");
                )
        {
            ArrayList<Semester> semesters=new ArrayList();
            int semesterId=-1;
            String name="";
            Date semBegin=null;
            Date semEnd=null;
            ResultSet set=getAllSemestersPtmt.executeQuery();
            while (set.next()){
                semesterId=set.getInt("semesterId");
                name=set.getString("name");
                semBegin=set.getDate("semBegin");
                semEnd=set.getDate("semEnd");
                Semester semester=new Semester();
                semester.id=semesterId;
                semester.name=name;
                semester.begin=semBegin;
                semester.end=semEnd;
                semesters.add(semester);
            }
            return semesters;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Semester getSemester(int semesterId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectSemesterPtmt=conn.prepareStatement("select * from semester where semesterId=?");
            )
        {
            Semester semester=new Semester();
            semester.id=-1;
            selectSemesterPtmt.setInt(1,semesterId);
            ResultSet set=selectSemesterPtmt.executeQuery();
            while (set.next()){
                semester.id=set.getInt(1);
                semester.name=set.getString(2);
                semester.begin=set.getDate(3);
                semester.end=set.getDate(4);
            }
            if (semester.id==-1){
                throw new EntityNotFoundException();
            }
            else{
                return semester;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        SemesterServiceImp aimp=new SemesterServiceImp();
       aimp.addSemester("x1",new Date(123),new Date(123113));
        aimp.addSemester("x2",new Date(1443534534),new Date(1731920139));
        List<Semester> list=aimp.getAllSemesters();
        for (Semester s:list){
            System.out.println(s.name);
        }
//        System.out.println(aimp.addSemester("s4",new Date(10,3,1),
//                new Date(11,9,3)));


        /*List<Semester> list=aimp.getAllSemesters();
        for (Semester s:list){
            System.out.println(   (s.end.getTime()-s.begin.getTime())/1000/3600/24 );
            System.out.println(s.end.getDate());
        }*/

//       aimp.addSemester("lop",new Date(15,1,1),new Date(16,1,1));
//        CourseServiceImp cimp=new CourseServiceImp();
//        cimp.addCourseSection("F",5,"fsec",100);

//        aimp.removeSemester(5);
    }
}
