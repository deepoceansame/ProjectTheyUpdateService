package implementingServiceByTXY;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
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
    public synchronized EnrollResult enrollCourse(int studentId, int sectionId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement studentExiPtmt=conn.prepareStatement("select exists(select studentId from student where studentId=?)");
            PreparedStatement sectionExiPtmt=conn.prepareStatement("select exists(select sectionId from section where sectionId=?)");
            PreparedStatement alePtmt=conn.prepareStatement("select exists(select sectionId from enroll " +
                    "where studentId=? and sectionId=? and grade=-3 )");
            PreparedStatement alpPtmt=conn.prepareStatement("select exists(select sectionId " +
                    "from enroll where studentId=? and sectionId=? and (grade=-1 or grade>=60))");
            PreparedStatement executeNoConfClassPtmt=conn.prepareStatement("select NoConfClass(?,?)");
            PreparedStatement getLeftCapacityPtmt=conn.prepareStatement("select leftCapacity from section where sectionId=?");
            PreparedStatement setLeftCapacityPtmt=conn.prepareStatement("update section set leftCapacity=leftCapacity-1 where sectionId=?");
            PreparedStatement addEnrollPtmt=conn.prepareStatement("insert into enroll(studentId,sectionId) values(?,?)");
            PreparedStatement getCourseIdBysectionIdPtmt=conn.prepareStatement("select courseId from section where sectionId=?")
        )
        {
            boolean studentExi=false;
            studentExiPtmt.setInt(1,studentId);
            ResultSet set=studentExiPtmt.executeQuery();
            while (set.next()){
                studentExi=set.getBoolean(1);
            }
            if (!studentExi){
                throw new IntegrityViolationException();
            }

            boolean sectionExi=false;
            sectionExiPtmt.setInt(1,sectionId);
            set=sectionExiPtmt.executeQuery();
            while (set.next()){
                sectionExi=set.getBoolean(1);
            }
            if (!sectionExi){
                return EnrollResult.COURSE_NOT_FOUND;
            }

            boolean ale=false;
            alePtmt.setInt(1,studentId);
            alePtmt.setInt(2,sectionId);
            set=alePtmt.executeQuery();
            while (set.next()){
                ale=set.getBoolean(1);
            }
            if (ale){
                return EnrollResult.ALREADY_ENROLLED;
            }

            boolean alp=false;
            alpPtmt.setInt(1,studentId);
            alpPtmt.setInt(2,sectionId);
            set=alpPtmt.executeQuery();
            while (set.next()){
                alp=set.getBoolean(1);
            }
            if (alp){
                return EnrollResult.ALREADY_PASSED;
            }

            boolean satisfyPre=false;
            String courseId="";
            getCourseIdBysectionIdPtmt.setInt(1,sectionId);
            set=getCourseIdBysectionIdPtmt.executeQuery();
            while (set.next()){
                courseId=set.getString(1);
            }
            StudentServiceImp simp=new StudentServiceImp();
            satisfyPre= simp.passedPrerequisitesForCourse(studentId,courseId);
            if (!satisfyPre){
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }

            boolean noConf=false;
            executeNoConfClassPtmt.setInt(1,sectionId);
            executeNoConfClassPtmt.setInt(2,studentId);
            set=executeNoConfClassPtmt.executeQuery();
            while (set.next()){
                noConf=set.getBoolean(1);
            }
            if (!noConf){
                return EnrollResult.COURSE_CONFLICT_FOUND;
            }

            boolean sectionFull=false;
            int left=0;
            getLeftCapacityPtmt.setInt(1,sectionId);
            set=getLeftCapacityPtmt.executeQuery();
            while (set.next()){
                left=set.getInt(1);
            }
            if (left<=0){
                return EnrollResult.COURSE_IS_FULL;
            }
           /* PreparedStatement addEnrollPtmt=conn.prepareStatement(
                    "insert into enroll(studentId,sectionId) values(?,?)");*/
            addEnrollPtmt.setInt(1,studentId);
            addEnrollPtmt.setInt(2,sectionId);
            addEnrollPtmt.executeUpdate();
            setLeftCapacityPtmt.setInt(1,sectionId);
            setLeftCapacityPtmt.executeUpdate();
            return EnrollResult.SUCCESS;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        try (
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getScorePtmt=conn.prepareStatement("select grade from enroll where studentId=? and sectionId=?");
                PreparedStatement dropPtmt=conn.prepareStatement("delete from enroll where studentId=? and sectionId=?");
                PreparedStatement addLeftCapPtmt=conn.prepareStatement("update section set leftCapacity=leftCapacity+1 where sectionId=?");
                PreparedStatement getLeftCapacityPtmt=conn.prepareStatement("select totalCapacity,leftCapacity from section where sectionId=?");
                )
        {
            int score=-119;
            getScorePtmt.setInt(1,studentId);
            getScorePtmt.setInt(2,sectionId);
            ResultSet set=getScorePtmt.executeQuery();
            while (set.next()){
                score=set.getInt(1);
            }
            if (score>-3){
                throw new IllegalStateException();
            }
            if (score!=-119){
                int leftCap=0;
                int totalCap=0;
                dropPtmt.setInt(1,studentId);
                dropPtmt.setInt(2,sectionId);
                dropPtmt.executeUpdate();
                getLeftCapacityPtmt.setInt(1,sectionId);
                set=getLeftCapacityPtmt.executeQuery();
                while (set.next()){
                    leftCap=set.getInt("leftCapacity");
                    totalCap=set.getInt("totalCapacity");
                }
                if (leftCap<totalCap){
                    addLeftCapPtmt.setInt(1,sectionId);
                    addLeftCapPtmt.executeUpdate();
                }
            }
            else{
                throw new EntityNotFoundException() ;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement stuExiPtmt=conn.prepareStatement("select exists(select studentId from student where studentId=?)");
                PreparedStatement secExiPtmt=conn.prepareStatement("select exists(select sectionId from section where sectionId=? )");
                PreparedStatement getCourseIdBysectionIdPtmt=conn.prepareStatement("select courseId from section where sectionId=?");
                PreparedStatement getGradMethodPtmt=conn.prepareStatement("select courseGrading from course where courseId=?");
                PreparedStatement addEnrollPtmt=conn.prepareStatement("insert into enroll(studentId,sectionId,grade) values(?,?,?)");
                )
        {
            boolean stuExi=false;
            stuExiPtmt.setInt(1,studentId);
            ResultSet set=stuExiPtmt.executeQuery();
            while (set.next()){
                stuExi=set.getBoolean(1);
            }
            if (!stuExi){
                throw new IntegrityViolationException();
            }

            boolean secExi=false;
            secExiPtmt.setInt(1,sectionId);
            set=secExiPtmt.executeQuery();
            while (set.next()){
                secExi=set.getBoolean(1);
            }
            if (!secExi){
                throw new IntegrityViolationException();
            }

            String courseId="";
            getCourseIdBysectionIdPtmt.setInt(1,sectionId);
            set=getCourseIdBysectionIdPtmt.executeQuery();
            while (set.next()){
                courseId=set.getString(1);
            }

            int gradMe=0;
            getGradMethodPtmt.setString(1,courseId);
            set=getGradMethodPtmt.executeQuery();
            while (set.next()){
                gradMe=set.getInt(1);
            }
            addEnrollPtmt.setInt(1,studentId);
            addEnrollPtmt.setInt(2,sectionId);
            if (grade!=null){
                if (gradMe==1){
                    if (grade instanceof HundredMarkGrade){
                        throw new IntegrityViolationException();
                    }
                    if (((PassOrFailGrade)grade).toString().equals("PASS")){
                        addEnrollPtmt.setInt(3,-1);
                    }
                    else {
                        addEnrollPtmt.setInt(3,-2);
                    }
                    addEnrollPtmt.executeUpdate();
                }
                else{
                    if (grade instanceof PassOrFailGrade){
                        throw new IntegrityViolationException();
                    }
                    addEnrollPtmt.setInt(3,((HundredMarkGrade)grade).mark);
                    addEnrollPtmt.executeUpdate();
                }
            }
            else{
                addEnrollPtmt.setInt(3,-3);
                addEnrollPtmt.executeUpdate();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
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

//        System.out.println(simp.passedPrerequisitesForCourse(666,"JIM"));

//        System.out.println(simp.enrollCourse(666,11).toString());

//        System.out.println(simp.enrollCourse(666,2));

        short s=30;
        simp.addEnrolledCourseWithGrade(666,11,PassOrFailGrade.PASS);

    }
}
