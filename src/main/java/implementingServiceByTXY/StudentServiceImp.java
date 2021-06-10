package implementingServiceByTXY;

import Tools.IntTox;
import Tools.MultiChooseIntGenerator;
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
            throw new IntegrityViolationException();
        }

    }


    public static String searchCourseSQL=
                    "select sectionId\n" +
                    "from(\n" +
                    "    select sectionId\n" +
                    "    from\n" +
                    "        (\n" +
                    "            select *\n" +
                    "            from    (\n" +
                    "                    select *\n" +
                    "                    from section\n" +
                    "                    where  (semesterId=?) and\n" +   //1
                    "                            (leftCapacity>0 or ?) and\n" +  //2
                    "                            ( noconfclass(sectionId,?) or ? ) and\n" + //3 4
                    "                            (not hasPassed(sectionId,?) or ?)\n" + // 5 6
                    "                    ) as sec\n" +
                    "                    natural join\n" +
                    "                    (\n" +
                    "                        select *\n" +
                    "                        from course\n" +
                    "                        where (getans(preId,?) or ?) and\n" + // 7 8
                    "                        (isCompulsory(courseId,?) or ?) and\n" +// 9 10
                    "                        (isElective(courseId,?) or ?) and\n" +//11 12
                    "                        (isCross(courseId,?) or ?) and\n" +//13 14
                    "                        (isPublic(courseId) or ?) and\n" +//15
                    "                        (courseId like '%'||?||'%' or ?)\n" +//16 17
                    "                    ) as cou\n" +
                    "            where ( (courseName||'['||sectionName||']') like '%'||?||'%' or ? )\n" + //18 19
                    "        ) as secToCou\n" +
                    "        natural join\n" +
                    "        (\n" +
                    "            select *\n" +
                    "            from class\n" +
                    "            where (dayOfWeek=? or ?) and\n" + //20,21
                    "                  (?&classTime!=0 or ?) and\n" +// 22,23
                    "                  (locationlikearr(location,?) or ?)" + //24,25
                    "        ) as claz\n" +
                    "        natural join teach natural join\n" +
                    "        (\n" +
                    "            select *\n" +
                    "            from instructor\n" +
                    "            where ( (firstName like ?||'%' or lastName like ?||'%' or firstName||lastName like ?||'%' or firstName||' '||lastName like ?||'%')   or ?  )\n" + //25 26 27 28 29
                    "        ) as ins\n" +
                    "    group by sectionId\n" +
                    ") secid natural join section natural join course\n" +
                    "order by courseId,courseName||'['||section.sectionName||']'\n" +
                    "limit ?\n" + //30
                    "offset ?;"; //31



    public static String getConflictSectionSQL="select courseName,sectionName\n" +
            "from (\n" +
            "        select *\n" +
            "        from enroll natural join section\n" +
            "        where semesterId=? and studentId=?\n" +
            "         ) as sectionTaken natural join course\n" +
            "where exists(\n" +
            "        select *\n" +
            "        from(\n" +
            "                select class.*,section.courseId,section.semesterId\n" +
            "                from class natural join section\n" +
            "                where class.sectionId=sectionTaken.sectionId\n" +
            "            ) as classOfTakenSec\n" +
            "            join\n" +
            "            (\n" +
            "                select class.*,section.courseId,section.semesterId\n" +
            "                from class natural join section\n" +
            "                where class.sectionId=?\n" +
            "            ) as classOftheSec on true\n" +
            "        where classOfTakenSec.courseId=classOftheSec.courseId or (classOfTakenSec.semesterId=classOftheSec.semesterId and classOfTakenSec.dayOfWeek=classOftheSec.dayOfWeek and\n" +
            "                                                                  classOfTakenSec.weekList&classOftheSec.weekList!=0 and  classOfTakenSec.classTime &classOftheSec.classTime!=0)\n" +
            "          ) or (courseId=(select courseId from section where sectionId=?)::varchar and semesterId=(select semesterId from section where sectionId=?)::int);";

    @Override
    public synchronized List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement searchCoursePtmt=conn.prepareStatement(searchCourseSQL);
                PreparedStatement getCourseBySectionIdPtmt=conn.prepareStatement("select * from (select courseId from section where sectionId=?) as theSec natural join course");
                PreparedStatement getSectionPtmt=conn.prepareStatement("select * from section where sectionId=?");
                PreparedStatement getClassesPtmt=conn.prepareStatement("select * from class natural join teach natural join instructor where sectionId=?");
                PreparedStatement getConflictSectionPtmt=conn.prepareStatement(getConflictSectionSQL);
                )
        {
            searchCoursePtmt.setInt(1,semesterId);

            searchCoursePtmt.setBoolean(2, !ignoreFull);

            searchCoursePtmt.setInt(3,studentId);
            searchCoursePtmt.setBoolean(4, !ignoreConflict);

            searchCoursePtmt.setInt(5,studentId);
            searchCoursePtmt.setBoolean(6,!ignorePassed);

            searchCoursePtmt.setInt(7,studentId);
            searchCoursePtmt.setBoolean(8,!ignoreMissingPrerequisites);

            searchCoursePtmt.setInt(9,studentId);
            searchCoursePtmt.setBoolean(10,!searchCourseType.equals(CourseType.MAJOR_COMPULSORY));

            searchCoursePtmt.setInt(11,studentId);
            searchCoursePtmt.setBoolean(12,!searchCourseType.equals(CourseType.MAJOR_ELECTIVE));

            searchCoursePtmt.setInt(13,studentId);
            searchCoursePtmt.setBoolean(14,!searchCourseType.equals(CourseType.CROSS_MAJOR));

            searchCoursePtmt.setBoolean(15,!searchCourseType.equals(CourseType.PUBLIC));

            searchCoursePtmt.setString(16, Objects.requireNonNullElse(searchCid, "fdsdf"));
            searchCoursePtmt.setBoolean(17,searchCid==null);

            searchCoursePtmt.setString(18,Objects.requireNonNullElse(searchName, "fdsdf"));
            searchCoursePtmt.setBoolean(19,searchName==null);

            int sdayOfWeek=0;
            if (searchDayOfWeek==null){
                sdayOfWeek=8;
            }
            else{
                sdayOfWeek=searchDayOfWeek.getValue();
            }
            searchCoursePtmt.setInt(20,sdayOfWeek);
            searchCoursePtmt.setBoolean(21,searchDayOfWeek==null);

            short sclassTime=0;
            sclassTime = Objects.requireNonNullElse(searchClassTime, (short)20);
            searchCoursePtmt.setInt(22, MultiChooseIntGenerator.classTimeIntGenerator(sclassTime,sclassTime));
            searchCoursePtmt.setBoolean(23,searchClassTime==null);

            List<String> sLocationList=Objects.requireNonNullElse(searchClassLocations,List.of("popj"));
            Array locationArr=conn.createArrayOf("varchar",sLocationList.toArray());
            searchCoursePtmt.setArray(24,locationArr);
            searchCoursePtmt.setBoolean(25,searchClassLocations==null);

            String sInsName=Objects.requireNonNullElse(searchInstructor,">");
            searchCoursePtmt.setString(26,sInsName);
            searchCoursePtmt.setString(27,sInsName);
            searchCoursePtmt.setString(28,sInsName);
            searchCoursePtmt.setString(29,sInsName);
            searchCoursePtmt.setBoolean(30,searchInstructor==null);

            searchCoursePtmt.setInt(31,pageSize);
            searchCoursePtmt.setInt(32,pageSize*pageIndex);

            ResultSet set=searchCoursePtmt.executeQuery();
            ArrayList<Integer> secs=new ArrayList<>();
            while (set.next()){
                secs.add(set.getInt(1));
            }

            ArrayList<CourseSearchEntry> entrys=new ArrayList<>();
            for (Integer secid:secs){
                int grameth=0;
                CourseSearchEntry entr=new CourseSearchEntry();
                Course cou=new Course();
                CourseSection sect=new CourseSection();
                Set<CourseSectionClass> clas=new HashSet<>();
                ArrayList<String> conc=new ArrayList<>();
                getCourseBySectionIdPtmt.setInt(1,secid);
                set=getCourseBySectionIdPtmt.executeQuery();
                while (set.next()){
                    cou.id=set.getString("courseId");
                    cou.name=set.getString("courseName");
                    cou.credit=set.getInt("credit");
                    cou.classHour=set.getInt("classHour");
                    grameth=set.getInt("courseGrading");
                    if (grameth==1){
                        cou.grading= Course.CourseGrading.PASS_OR_FAIL;
                    }
                    else {
                        cou.grading= Course.CourseGrading.HUNDRED_MARK_SCORE;
                    }
                }

                getSectionPtmt.setInt(1,secid);
                set=getSectionPtmt.executeQuery();
                while (set.next()){
                    sect.id=secid;
                    sect.name=set.getString("sectionName");
                    sect.totalCapacity=set.getInt("totalCapacity");
                    sect.leftCapacity=set.getInt("leftCapacity");
                }

                getClassesPtmt.setInt(1,secid);
                set=getClassesPtmt.executeQuery();
                while (set.next()){
                    CourseSectionClass cas=new CourseSectionClass();
                    Instructor ins=new Instructor();
                    cas.id=set.getInt("classId");
                    cas.dayOfWeek=DayOfWeek.of(set.getInt("dayOfWeek"));
                    cas.weekList= IntTox.intToList(set.getInt("weekList"));
                    cas.classBegin=IntTox.getBegin(set.getInt("classTime"));
                    cas.classEnd=IntTox.getEnd(set.getInt("classTime"));
                    cas.location=set.getString("location");
                    ins.id=set.getInt("instructorId");
                    ins.fullName=UserServiceImp.getFullName(set.getString("firstName"),set.getString("lastName"));
                    cas.instructor=ins;
                    clas.add(cas);
                }

                getConflictSectionPtmt.setInt(1,semesterId);
                getConflictSectionPtmt.setInt(2,studentId);
                getConflictSectionPtmt.setInt(3,secid);
                getConflictSectionPtmt.setInt(4,secid);
                getConflictSectionPtmt.setInt(5,secid);
                set=getConflictSectionPtmt.executeQuery();
                while (set.next()){
                    conc.add(String.format("%s[%s]",set.getString("courseName"),set.getString("sectionName")));
                }
                conc.sort(String::compareTo);

                entr.course=cou;
                entr.section=sect;
                entr.sectionClasses=clas;
                entr.conflictCourseNames=conc;
                entrys.add(entr);
            }
            entrys.sort(new CourseSearchEntryComp());
            return entrys;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public static class CourseSearchEntryComp implements Comparator<CourseSearchEntry>{

        @Override
        public int compare(CourseSearchEntry o1, CourseSearchEntry o2) {
            if (o1.course.id.compareTo(o2.course.id)!=0){
                return o1.course.id.toUpperCase().compareTo(o2.course.id.toUpperCase());
            }
            else{
                String one=String.format("%s[%s]",o1.course.name,o1.section.name);
                String two=String.format("%s[%s]",o2.course.name,o2.section.name);
                return one.compareTo(two);
            }
        }
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
                    "where studentId=? and sectionId=?)");
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
        catch(EntityNotFoundException e){
            return;
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
                if (true){
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
                if (true){
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
                        "    where studentId=?\n" +
                        "         ) classTaken natural join section natural join class natural join course natural join teach natural join instructor\n" +
                        "where semesterId=? and weekList&?!=0");
                )
        {
            CourseTable courseTable=new CourseTable();
            courseTable.table=new HashMap<>();
            for (int i=1;i<=7;i++){
                courseTable.table.put(DayOfWeek.of(i),new HashSet<>());
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
            if (semid==-1){
                return courseTable;
            }
            short theNthWeek=(short) ( (date.getTime()- sembe.getTime())/1000/3600/24/7+1  );
            /*if ( ((date.getTime()- sembe.getTime())/1000/3600/24)%7==0){
                theNthWeek-=1;
            }*/
            Set<Short> weeklist=new HashSet<>();
            weeklist.add(theNthWeek);
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
            courseTable.table.put(DayOfWeek.of(i),new HashSet<>());
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
//        simp.addStudent(666,2,"Tang","Xinyu",new Date(2021,1,1));

//        Major ma=simp.getStudentMajor(666);
//        System.out.println(ma.id+" "+ma.name+" "+ma.department.id+" "+ma.department.name);

//        System.out.println(simp.passedPrerequisitesForCourse(666,"JIM"));

//        System.out.println(simp.enrollCourse(666,11).toString());

//        System.out.println(simp.enrollCourse(666,2));

        short s=40;
        short g=61;
        short h=-3;
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
            List<String> list=List.of("jio","ace","dsd","dsd");
            Array arr=conn.createArrayOf("varchar",list.toArray());
            isinPtmt.setArray(1,arr);
            ResultSet set=isinPtmt.executeQuery();
            while (set.next()){
                System.out.println(set.getBoolean(1));
            }

        }
        catch (SQLException e) {
            e.printStackTrace();
        }*/


        List<CourseSearchEntry> list=simp.searchCourse(666,1,null,
                null,null,null,
                null,null,CourseType.ALL,true,true,true,false,20,0);
        int a=0;


//        System.out.println(simp.enrollCourse(666,18));
/*        simp.setEnrolledCourseGrade(666,1,new HundredMarkGrade(h));
        simp.setEnrolledCourseGrade(666,3,new HundredMarkGrade(h));
        simp.setEnrolledCourseGrade(666,4,new HundredMarkGrade(h));
        simp.setEnrolledCourseGrade(666,6,new HundredMarkGrade(h));
        simp.setEnrolledCourseGrade(666,7,new HundredMarkGrade(h));*/
//        simp.setEnrolledCourseGrade(666,24,new HundredMarkGrade(h));
//        System.out.println(simp.passedPrerequisitesForCourse(666,"JIM"));


       /* System.out.println(simp.enrollCourse(666,3));
        System.out.println(simp.enrollCourse(666,4));
        System.out.println(simp.enrollCourse(666,6));*/

  /*      simp.setEnrolledCourseGrade(666,1,new HundredMarkGrade(g));
        simp.setEnrolledCourseGrade(666,3,new HundredMarkGrade(g));
        simp.setEnrolledCourseGrade(666,4,new HundredMarkGrade(g));
        simp.setEnrolledCourseGrade(666,6,new HundredMarkGrade(g));*/


//        System.out.println(simp.passedPrerequisitesForCourse(666,"JIM"));
 /*       System.out.println(simp.enrollCourse(666,3));
        System.out.println(simp.enrollCourse(666,4));
        System.out.println(simp.enrollCourse(666,6));*/
//        System.out.println(simp.enrollCourse(666,7));

//        simp.setEnrolledCourseGrade(666,7,new HundredMarkGrade(g));

    }
}
