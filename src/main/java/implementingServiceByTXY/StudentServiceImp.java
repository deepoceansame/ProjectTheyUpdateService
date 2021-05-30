package implementingServiceByTXY;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentServiceImp implements StudentService {
    @Override
    public synchronized void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement ptmt=conn.prepareStatement("insert into student(studentId,majorId,firstName,lastName,enrolleddate) " +
                        "values(?,?,?,?,?)");
                PreparedStatement uptmt=conn.prepareStatement("insert into tuser(userId,firstName,lastName,tos) values(?,?,?,?)");
        )
        {
            uptmt.setInt(1,userId);
            uptmt.setString(2,firstName);
            uptmt.setString(3,lastName);
            uptmt.setInt(4,2);
            uptmt.executeUpdate();
            ptmt.setInt(1,userId);
            ptmt.setInt(2,majorId);
            ptmt.setString(3,firstName);
            ptmt.setString(4,lastName);
            ptmt.setDate(5,enrolledDate);
            ptmt.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }

    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        return null;
    }

    @Override
    //COURSE_NOT_FOUND > ALREADY_ENROLLED > ALREADY_PASSED >
    // PREREQUISITES_NOT_FULFILLED > COURSE_CONFLICT_FOUND > COURSE_IS_FULL > UNKNOWN_ERROR
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement sectionExiPtmt=conn.prepareStatement("select exists(select sectionId from section where sectionId=sectionId)");
            PreparedStatement alePtmt=conn.prepareStatement("select exists(select sectionId from enroll " +
                    "where studentId=? and sectionId=? and grade=-3 )");
            PreparedStatement alpPtmt=conn.prepareStatement("select exists(select sectionId " +
                    "from enroll where studentId=? and sectionId=? and (grade=-1 or grade>=60))");

        )
        {

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {

    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {

    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {

    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        return null;
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        return null;
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getPreStringPtmt=conn.prepareStatement("select pre from course where courseId=?");
                PreparedStatement getPassedCourseListPtmt=conn.prepareStatement("select courseId " +
                        "from enroll natural join section natural join course " +
                        "where (grade=-1 or grade>=60) and studentId=?");
                PreparedStatement studentExiPtmt=conn.prepareStatement("select exists(select studentId from student where studentId=?)");
                PreparedStatement courseExiPtmt=conn.prepareStatement("select exists(select courseId from course where courseId=?)")
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

    @Override
    public Major getStudentMajor(int studentId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getMajorIdPtmt=conn.prepareStatement("select majorId from student where studentId=?");
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
            int majorId=-1;
            getMajorIdPtmt.setInt(1,studentId);
            ResultSet set=getMajorIdPtmt.executeQuery();
            while (set.next()){
                majorId=set.getInt(1);
            }
            if (majorId==-1){
                throw new EntityNotFoundException();
            }
            selectMajorPtmt.setInt(1,majorId);
            set=selectMajorPtmt.executeQuery();
            Major ma=new Major();
            ma.id=majorId;
            Department dep=new Department();
            while (set.next()){
                ma.name=set.getString("majorName");
                dep.id=set.getInt("departmentId");
                dep.name=set.getString("departmentName");
            }
            ma.department=dep;
            return ma;
        }
        catch (SQLException e){
            e.printStackTrace();
            return new Major();
        }

    }

    public static void main(String[] args) {
        StudentServiceImp simp=new StudentServiceImp();
//        simp.addStudent(666,1,"Tang","Xinyu",new Date(2021,1,1));

//        Major ma=simp.getStudentMajor(666);
//        System.out.println(ma.id+" "+ma.name+" "+ma.department.id+" "+ma.department.name);

        System.out.println(simp.passedPrerequisitesForCourse(666,"JIM"));
    }
}
