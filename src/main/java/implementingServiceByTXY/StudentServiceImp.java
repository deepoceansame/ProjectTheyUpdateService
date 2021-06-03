package implementingServiceByTXY;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;
import org.postgresql.jdbc.PgArray;

import javax.annotation.Nullable;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

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
    public synchronized List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement ptmt=conn.prepareStatement("");
                )
        {

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
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
                    "where studentId=? and courseId=? and grade=-3 )");
            PreparedStatement alpPtmt=conn.prepareStatement("select exists(select * " +
                    "from courseGrade where studentId=? and courseId=? and (grade=-1 or grade>=60))");
            PreparedStatement executeNoConfClassPtmt=conn.prepareStatement("select NoConfClass(?,?)");
            PreparedStatement getLeftCapacityPtmt=conn.prepareStatement("select leftCapacity from section where sectionId=?");
            PreparedStatement setLeftCapacityPtmt=conn.prepareStatement("update section set leftCapacity=leftCapacity-1 where sectionId=?");
            PreparedStatement addEnrollPtmt=conn.prepareStatement("insert into enroll(studentId,sectionId) values(?,?)");
            PreparedStatement getCourseIdBysectionIdPtmt=conn.prepareStatement("select courseId from section where sectionId=?");
            PreparedStatement insertCourseGradePtmt=conn.prepareStatement("insert into courseGrade(studentId,courseId,grade) values(?,?,?)");
            PreparedStatement courseGradeExiPtmt=conn.prepareStatement("select exists(select * from courseGrade where studentId=? and courseId=?)");
            PreparedStatement setCourseGradePtmt=conn.prepareStatement("update courseGrade set grade=? where studentId=? and courseId=?");
        )
        {
            String courseId="";
            getCourseIdBysectionIdPtmt.setInt(1,sectionId);
            ResultSet set=getCourseIdBysectionIdPtmt.executeQuery();
            while (set.next()){
                courseId=set.getString(1);
            }

            boolean studentExi=false;
            studentExiPtmt.setInt(1,studentId);
            set=studentExiPtmt.executeQuery();
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
            alpPtmt.setString(2,courseId);
            set=alpPtmt.executeQuery();
            while (set.next()){
                alp=set.getBoolean(1);
            }
            if (alp){
                return EnrollResult.ALREADY_PASSED;
            }

            boolean satisfyPre=false;
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

            boolean courseGradeExi=false;
            courseGradeExiPtmt.setInt(1,studentId);
            courseGradeExiPtmt.setString(2,courseId);
            set=courseGradeExiPtmt.executeQuery();
            while (set.next()){
                courseGradeExi=set.getBoolean(1);
            }
            if (!courseGradeExi){
                insertCourseGradePtmt.setInt(1,studentId);
                insertCourseGradePtmt.setString(2,courseId);
                insertCourseGradePtmt.setInt(3,-3);
                insertCourseGradePtmt.executeUpdate();
            }
            return EnrollResult.SUCCESS;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }
    }

    @Override
    public synchronized void dropCourse(int studentId, int sectionId) throws IllegalStateException {
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
    public synchronized void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement stuExiPtmt=conn.prepareStatement("select exists(select studentId from student where studentId=?)");
                PreparedStatement secExiPtmt=conn.prepareStatement("select exists(select sectionId from section where sectionId=? )");
                PreparedStatement getCourseIdBysectionIdPtmt=conn.prepareStatement("select courseId from section where sectionId=?");
                PreparedStatement getGradMethodPtmt=conn.prepareStatement("select courseGrading from course where courseId=?");
                PreparedStatement addEnrollPtmt=conn.prepareStatement("insert into enroll(studentId,sectionId,grade) values(?,?,?)");
                PreparedStatement insertCourseGradePtmt=conn.prepareStatement("insert into courseGrade(studentId,courseId,grade) values(?,?,?)");
                PreparedStatement courseGradeExiPtmt=conn.prepareStatement("select exists(select * from courseGrade where studentId=? and courseId=?)");
                PreparedStatement setCourseGradePtmt=conn.prepareStatement("update courseGrade set grade=? where studentId=? and courseId=?");
                PreparedStatement getCourseGradePtmt=conn.prepareStatement("select grade from courseGrade where studentId=? and courseId=?");
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
            int mark=-1010;
            if (grade!=null){
                if (gradMe==1){
                    if (grade instanceof HundredMarkGrade){
                        throw new IntegrityViolationException();
                    }
                    if (((PassOrFailGrade)grade).toString().equals("PASS")){
                        addEnrollPtmt.setInt(3,-1);
                        mark=-1;
                    }
                    else {
                        addEnrollPtmt.setInt(3,-2);
                        mark=-2;
                    }
                    addEnrollPtmt.executeUpdate();
                }
                else{
                    if (grade instanceof PassOrFailGrade){
                        throw new IntegrityViolationException();
                    }
                    addEnrollPtmt.setInt(3,((HundredMarkGrade)grade).mark);
                    addEnrollPtmt.executeUpdate();
                    mark=((HundredMarkGrade)grade).mark;
                }
            }
            else{
                addEnrollPtmt.setInt(3,-3);
                addEnrollPtmt.executeUpdate();
                mark=-3;
            }

            courseGradeExiPtmt.setInt(1,studentId);
            courseGradeExiPtmt.setString(2,courseId);
            boolean courseGradeExi=false;
            set=courseGradeExiPtmt.executeQuery();
            while (set.next()){
                courseGradeExi=set.getBoolean(1);
            }
            if (courseGradeExi){
                int nowgra=-777;
                getCourseGradePtmt.setInt(1,studentId);
                getCourseGradePtmt.setString(2,courseId);
                set=getCourseGradePtmt.executeQuery();
                while (set.next()){
                    nowgra=set.getInt(1);
                }
                if (mark>nowgra){
                    setCourseGradePtmt.setInt(1,mark);
                    setCourseGradePtmt.setInt(2,studentId);
                    setCourseGradePtmt.setString(3,courseId);
                    setCourseGradePtmt.executeUpdate();
                }
            }
            else{
                insertCourseGradePtmt.setInt(1,studentId);
                insertCourseGradePtmt.setString(2,courseId);
                insertCourseGradePtmt.setInt(3,mark);
                insertCourseGradePtmt.executeUpdate();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public synchronized void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement enrollExiPtmt=conn.prepareStatement("select exists(select * from enroll where studentId=? and sectionId=?)");
                PreparedStatement getCourseIdBysectionIdPtmt=conn.prepareStatement("select courseId from section where sectionId=?");
                PreparedStatement getGradMethodPtmt=conn.prepareStatement("select courseGrading from course where courseId=?");
                PreparedStatement setGradePtmt=conn.prepareStatement("update enroll set grade=? where studentId=? and sectionId=?");
                PreparedStatement insertCourseGradePtmt=conn.prepareStatement("insert into courseGrade(studentId,courseId,grade) values(?,?,?)");
                PreparedStatement courseGradeExiPtmt=conn.prepareStatement("select exists(select * from courseGrade where studentId=? and courseId=?)");
                PreparedStatement setCourseGradePtmt=conn.prepareStatement("update courseGrade set grade=? where studentId=? and courseId=?");
                PreparedStatement getCourseGradePtmt=conn.prepareStatement("select grade from courseGrade where studentId=? and courseId=?");
                )
        {
            if (grade instanceof HundredMarkGrade){
                if ( ((HundredMarkGrade)grade).mark<0 ||  ((HundredMarkGrade)grade).mark>100 ){
                    throw new IntegrityViolationException();
                }
            }
            boolean enrollExi=false;
            enrollExiPtmt.setInt(1,studentId);
            enrollExiPtmt.setInt(2,sectionId);
            ResultSet set=enrollExiPtmt.executeQuery();
            while (set.next()){
                enrollExi=set.getBoolean(1);
            }
            if (!enrollExi){
                throw new EntityNotFoundException();
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

            setGradePtmt.setInt(2,studentId);
            setGradePtmt.setInt(3,sectionId);
            int insertMark=-777;
            if (gradMe==1){
                if (grade instanceof HundredMarkGrade){
                    throw new IntegrityViolationException();
                }
                if (((PassOrFailGrade)grade).toString().equals("PASS")){
                    setGradePtmt.setInt(1,-1);
                    insertMark=-1;
                }
                else {
                    setGradePtmt.setInt(1,-2);
                    insertMark=-2;
                }
            }
            else{
                if (grade instanceof PassOrFailGrade){
                    throw new IntegrityViolationException();
                }
                setGradePtmt.setInt(1,((HundredMarkGrade)grade).mark );
                insertMark=((HundredMarkGrade)grade).mark;
            }
            setGradePtmt.executeUpdate();


            courseGradeExiPtmt.setInt(1,studentId);
            courseGradeExiPtmt.setString(2,courseId);
            boolean courseGradeExi=false;
            set=courseGradeExiPtmt.executeQuery();
            while (set.next()){
                courseGradeExi=set.getBoolean(1);
            }
            if (courseGradeExi){
                int nowgra=-777;
                getCourseGradePtmt.setInt(1,studentId);
                getCourseGradePtmt.setString(2,courseId);
                set=getCourseGradePtmt.executeQuery();
                while (set.next()){
                    nowgra=set.getInt(1);
                }
                if (insertMark>nowgra){
                    setCourseGradePtmt.setInt(1,insertMark);
                    setCourseGradePtmt.setInt(2,studentId);
                    setCourseGradePtmt.setString(3,courseId);
                    setCourseGradePtmt.executeUpdate();
                }
            }
            else{
                insertCourseGradePtmt.setInt(1,studentId);
                insertCourseGradePtmt.setString(2,courseId);
                insertCourseGradePtmt.setInt(3,insertMark);
                insertCourseGradePtmt.executeUpdate();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getCoursesAndGradesPtmt=conn.prepareStatement("select * from enroll natural join section natural join semester where (semesterId=? or ?) and studentId=? order by semBegin");
                PreparedStatement getCourseByIdPtmt=conn.prepareStatement("select * from course where courseId=?");
                )
        {
            getCoursesAndGradesPtmt.setInt(3,studentId);
            if (semesterId==null){
                getCoursesAndGradesPtmt.setInt(1,-1);
                getCoursesAndGradesPtmt.setBoolean(2,true);
            }
            else{
                getCoursesAndGradesPtmt.setInt(1,semesterId);
                getCoursesAndGradesPtmt.setBoolean(2,false);
            }
            ResultSet set=getCoursesAndGradesPtmt.executeQuery();
            TreeMap<String,Integer> tempMap=new TreeMap<>();
            HashMap<Course,Grade> retMap=new HashMap<>();
//            Course: id name credit classHour grading
            String courseId="";
            int gra=0;
            int grading=0;
            while (set.next()){
                //System.out.println(set.getString("courseId")+" "+set.getInt("sectionId")+" "+set.getInt("semesterId")+" "+set.getInt("grade") );
                courseId=set.getString("courseId");
                gra=set.getInt("grade");
                tempMap.put(courseId,gra);
            }
            for (String s:tempMap.keySet()){
                gra=tempMap.get(s);
                Course course=new Course();
                Grade g=null;
                getCourseByIdPtmt.setString(1,s);
                set=getCourseByIdPtmt.executeQuery();
                while (set.next()){
                    course.id=s;
                    course.name=set.getString("courseName");
                    course.credit=set.getInt("credit");
                    course.classHour=set.getInt("classHour");
                    grading=set.getInt("courseGrading");
                    if (grading==1){
                        course.grading= Course.CourseGrading.PASS_OR_FAIL;
                    }
                    else{
                        course.grading= Course.CourseGrading.HUNDRED_MARK_SCORE;
                    }
                }
                if (gra==-3){
                    g=null;
                }
                else if (gra==-2){
                    g=PassOrFailGrade.FAIL;
                }
                else if (gra==-1){
                    g=PassOrFailGrade.PASS;
                }
                else{
                    g=new HundredMarkGrade((short)gra);
                }
                retMap.put(course,g);
            }
            return retMap;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public synchronized CourseTable getCourseTable(int studentId, Date date) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getSemesterByDatePtmt=conn.prepareStatement("select * from semester where semBegin<=? and semEnd>=?");
                PreparedStatement getClassBeingTakenPtmt=conn.prepareStatement("select instructorId,firstName,lastName,classTime,location,sectionName,courseName,dayOfWeek\n" +
                        "from (\n" +
                        "     select *\n" +
                        "    from enroll\n" +
                        "    where studentId=? and grade=-3\n" +
                        "         ) classTaken natural join section natural join class natural join course natural join teach natural join instructor\n" +
                        "where semesterId=? and weekList&?!=0");
                )
        {
            CourseTable courseTable=new CourseTable();
            courseTable.table=new HashMap<>();
            for (int i=1;i<=7;i++){
                courseTable.table.put(DayOfWeek.of(i),new ArrayList<>());
            }
            getSemesterByDatePtmt.setDate(1,date);
            getSemesterByDatePtmt.setDate(2,date);
            Date sembe=null;
            Date semen=null;
            int semid=-1;
            ResultSet set=getSemesterByDatePtmt.executeQuery();
            while (set.next()){
                semid=set.getInt("semesterId");
                sembe=set.getDate("semBegin");
                semen=set.getDate("semEnd");
            }
            short theNthWeek=(short) ( (date.getTime()- sembe.getTime())/1000/3600/24/7+1  );
            List<Short> weeklist=List.of(theNthWeek);
            int weekInt=Tools.MultiChooseIntGenerator.weekIntGenerator(weeklist);
            getClassBeingTakenPtmt.setInt(1,studentId);
            getClassBeingTakenPtmt.setInt(2,semid);
            getClassBeingTakenPtmt.setInt(3,weekInt);
            set=getClassBeingTakenPtmt.executeQuery();
            int dayOfweek=0;
            DayOfWeek dw=null;
            int classTime=-1;
            String courseName="";
            String sectionName="";
            while (set.next()){
                dayOfweek=set.getInt("dayOfweek");
                dw=DayOfWeek.of(dayOfweek);
                CourseTable.CourseTableEntry entry=new CourseTable.CourseTableEntry();
                Instructor ins=new Instructor();
                ins.id=set.getInt("instructorId");
                ins.fullName=UserServiceImp.getFullName(set.getString("firstName"),set.getString("lastName"));
                entry.instructor=ins;
                classTime=set.getInt("classTime");
                entry.classBegin=Tools.IntTox.getBegin(classTime);
                entry.classEnd=Tools.IntTox.getEnd(classTime);
                courseName=set.getString("courseName");
                sectionName=set.getString("sectionName");
                entry.courseFullName=String.format("%s[%s]", courseName, sectionName);
                entry.location=set.getString("location");
                courseTable.table.get(dw).add(entry);
            }
            return courseTable;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        CourseTable courseTable=new CourseTable();
        courseTable.table=new HashMap<>();
        for (int i=1;i<=7;i++){
            courseTable.table.put(DayOfWeek.of(i),new ArrayList<>());
        }
        return courseTable;
    }

    /*public static String getFullName(String fn,String ln){
        boolean fnb=false;
        for (int i=0;i<fn.length();i++){
            fnb=(fn.charAt(i)+"").matches("[a-zA-Z]");
            if (!fnb){
                return fn+ln;
            }
        }
        boolean lnb=false;
        for (int i=0;i<ln.length();i++){
            lnb=(ln.charAt(i)+"").matches("[a-zA-Z]");
            if (!lnb){
                return fn+ln;
            }
        }
        return fn+" "+ln;
    }*/

    @Override
    public synchronized boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
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

            getPreIdPtmt.setString(1,courseId);
            set=getPreIdPtmt.executeQuery();
            int preId=0;
            while (set.next()){
                preId=set.getInt(1);
            }
            if (preId==-888){
                return true;
            }

            boolean ans=false;
            executeGetAnsPtmt.setInt(1,preId);
            executeGetAnsPtmt.setInt(2,studentId);
            set=executeGetAnsPtmt.executeQuery();
            while (set.next()){
                ans=set.getBoolean(1);
            }
            return ans;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public synchronized Major getStudentMajor(int studentId) {
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

        short s=40;
        short g=61;
//        simp.addEnrolledCourseWithGrade(666,11,PassOrFailGrade.PASS);
//       simp.addEnrolledCourseWithGrade(666,8,new HundredMarkGrade(g));
//       simp.addEnrolledCourseWithGrade(666,9,new HundredMarkGrade(g));
//       simp.addEnrolledCourseWithGrade(666,10,new HundredMarkGrade(g));

        /*Map<Course,Grade> map=simp.getEnrolledCoursesAndGrades(666,null);
        Grade gg=null;
        for (Course c:map.keySet()){
            gg=map.get(c);
            if (gg instanceof PassOrFailGrade){
                System.out.println(c.id+" "+c.grading.toString()+" "+gg.toString());
            }
            else {
                System.out.println(c.id+" "+c.grading.toString()+" "+((HundredMarkGrade)gg).mark);
            }
        }*/

//        System.out.println(simp.passedPrerequisitesForCourse(666,"JIM"));


    /*    CourseTable ct= simp.getCourseTable(666,new Date(10,9,2));
        for (int i=1;i<8;i++){
            System.out.println(i);
            List< CourseTable.CourseTableEntry > list=ct.table.get(DayOfWeek.of(i));
            for (CourseTable.CourseTableEntry ce:list){
                System.out.println(ce.instructor.fullName+" "+ce.location+" "+ce.courseFullName);
            }
        }*/



/*        try (
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement isinPtmt=conn.prepareStatement(" select 'ace' = any(?)  ")
                )
        {
            String[] ss=new String[2];
            ss[0]="acre";
            ss[1]="ttt";
            Array arr=conn.createArrayOf("varchar",ss);
            isinPtmt.setArray(1,arr);
            ResultSet set=isinPtmt.executeQuery();
            while (set.next()){
                System.out.println(set.getBoolean(1));
            }

        }
        catch (SQLException e) {
            e.printStackTrace();
        }*/


       simp.addEnrolledCourseWithGrade(666,17,PassOrFailGrade.FAIL);
    }
}
