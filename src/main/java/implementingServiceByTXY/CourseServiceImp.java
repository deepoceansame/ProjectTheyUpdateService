package implementingServiceByTXY;
import Tools.CanPassPre;
import Tools.IntTox;
import Tools.MultiChooseIntGenerator;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;
import cn.edu.sustech.cs307.database.SQLDataSource;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class CourseServiceImp implements CourseService{
    static String insPre="insert into prerequisite (isRoot,preType,courseId) values(?,?,?)";
    static String insMas="insert into mas (master,servant) values(?,?) ";
    static String getPreId="select nextval('prerequisite_preid_seq')";
    static String setPreId="select setval('prerequisite_preid_seq',?)";
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite coursePrerequisite) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement addCoursePtmt =conn.prepareStatement("insert into " +
                        "course(courseId,courseName,credit,classHour,courseGrading,pre,preId) " +
                        "values(?,?,?,?,?,?,?)");
                PreparedStatement existPtmt=conn.prepareStatement("select exists(select * from course where courseId=?)");
            )
        {
            String pre="";
            if (coursePrerequisite!=null){
                pre=getPreString(coursePrerequisite);
                String[] ss= CanPassPre.getPreName(pre);
                boolean isexist=false;
                ResultSet set=null;
                for (String cid:ss){
                    existPtmt.setString(1,cid);
                    set=existPtmt.executeQuery();
                    while (set.next()){
                        isexist=set.getBoolean(1);
                    }
                    if (!isexist){
                        throw new IntegrityViolationException();
                    }
                }
            }
            int pid=0;
            if(coursePrerequisite!=null){
                pid=addPreToTable(coursePrerequisite,true,-1);
            }
            addCoursePtmt .setString(1,courseId);
            addCoursePtmt .setString(2,courseName);
            addCoursePtmt .setInt(3,credit);
            addCoursePtmt .setInt(4,classHour);
            if (grading.equals(Course.CourseGrading.PASS_OR_FAIL))
                addCoursePtmt .setInt(5,1);
            else
                addCoursePtmt .setInt(5,2);
            if (coursePrerequisite!=null){
                addCoursePtmt .setString(6,pre);
                addCoursePtmt.setInt(7,pid);
            }
            else{
                addCoursePtmt.setString(6,null);
                addCoursePtmt.setInt(7,-888);
            }
            addCoursePtmt .executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    public static int addPreToTable(Prerequisite prep,boolean isRoot,int fatherId) throws SQLException {
        int thisId=0;
        if (prep instanceof CoursePrerequisite){
            thisId=addPre(isRoot,3,((CoursePrerequisite) prep).courseID);
            if (!isRoot){
                addMas(fatherId,thisId);
            }
            else{
                isRoot=false;
            }
        }
        else if (prep instanceof AndPrerequisite){
            thisId=addPre(isRoot,1,null);
            if (!isRoot){
                addMas(fatherId,thisId);
            }
            else{
                isRoot=false;
            }
           for (Prerequisite item: ((AndPrerequisite) prep).terms ){
               addPreToTable(item,isRoot,thisId);
           }
        }
        else{
            thisId=addPre(isRoot,2,null);
            if (!isRoot){
                addMas(fatherId,thisId);
            }
            else{
                isRoot=false;
            }
            for (Prerequisite item: ((OrPrerequisite) prep).terms ){
                addPreToTable(item,isRoot,thisId);
            }
        }
        return thisId;
    }

    public static int addPre(boolean isRoot,int type,@Nullable String courseId) throws SQLException {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement insPtmt=conn.prepareStatement(insPre);
                PreparedStatement getIdPtmt=conn.prepareStatement(getPreId);
                PreparedStatement setIdPtmt=conn.prepareStatement(setPreId);
                )
        {
            insPtmt.setBoolean(1,isRoot);
            insPtmt.setInt(2,type);
            if (courseId==null)
                insPtmt.setString(3,null);
            else
                insPtmt.setString(3,courseId);
            insPtmt.executeUpdate();
            int id=0;
            ResultSet set=getIdPtmt.executeQuery();
            while (set.next()){
                id=set.getInt(1);
            }
            setIdPtmt.setInt(1,id-1);
            setIdPtmt.executeQuery();
            return id-1;
        }

    }

    public static void addMas(int master,int servant) throws SQLException{
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement insPtmt=conn.prepareStatement(insMas);
        )
        {
            insPtmt.setInt(1,master);
            insPtmt.setInt(2,servant);
            insPtmt.executeUpdate();
        }
    }

    public static String getPreString(Prerequisite pre){
        StringBuilder s=new StringBuilder("");
        if (pre instanceof CoursePrerequisite){
            return ((CoursePrerequisite) pre).courseID;
        }
        else if (pre instanceof AndPrerequisite){
            s.append('(');
            for (Prerequisite p:((AndPrerequisite) pre).terms){
                s.append(getPreString(p));
                s.append('*');
            }
            s.deleteCharAt(s.length()-1);
            s.append(')');
            return s.toString();
        }
        else{
            s.append('(');
            for (Prerequisite p:((OrPrerequisite) pre).terms){
                s.append(getPreString(p));
                s.append('+');
            }
            s.deleteCharAt(s.length()-1);
            s.append(')');
            return s.toString();
        }
    }

    @Override
    public synchronized int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement idptmt=conn.prepareStatement("select nextval('section_sectionid_seq')");
                PreparedStatement insptmt=conn.prepareStatement("insert into section" +
                        "(courseId, semesterId, sectionName, totalCapacity , leftCapacity) VALUES(?,?,?,?,?)");
                PreparedStatement setptmt=conn.prepareStatement("select setval('section_sectionid_seq',?)");
        )
        {
            insptmt.setString(1,courseId);
            insptmt.setInt(2,semesterId);
            insptmt.setString(3,sectionName);
            insptmt.setInt(4,totalCapacity);
            insptmt.setInt(5,totalCapacity);
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
        catch(SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public synchronized int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, List<Short> weekList, short classStart, short classEnd, String location) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement instrExiPtmt=conn.prepareStatement("select * from instructor where instructorId=?");
                PreparedStatement getSectionSemsterIdPtmt=conn.prepareStatement("select semesterid from section where sectionid=?");
                PreparedStatement idptmt=conn.prepareStatement("select nextval('class_classid_seq')");
                PreparedStatement insptmt=conn.prepareStatement("insert into class" +
                        "(sectionId, dayOfWeek, weekList,classTime,location) VALUES(?,?,?,?,?)");
                PreparedStatement setptmt=conn.prepareStatement("select setval('class_classid_seq',?)");
                PreparedStatement teachInsertPtmt=conn.prepareStatement("insert into teach " +
                        "(instructorId,classId) values(?,?)");
                PreparedStatement executeNoCoincidencePtmt=conn.prepareStatement("select noCoincidenceAboutTeach(?,?,?,?,?,?)");
                PreparedStatement executeNoCoinAboutClassPtmt=conn.prepareStatement("select noCoinAboutClass(?,?,?,?,?)");
                PreparedStatement executeNoConfClassWithSameSection=conn.prepareStatement("select NoConfClassWithSameSection(?,?,?,?)")

        )
        {
            if (classEnd<classStart) throw new IntegrityViolationException();
            int semesterid=-1;
            getSectionSemsterIdPtmt.setInt(1,sectionId);
            ResultSet res=getSectionSemsterIdPtmt.executeQuery();
            while (res.next()){
                semesterid=res.getInt(1);
            }
            if (semesterid==-1){
                throw new IntegrityViolationException();
            }
            boolean instrExi=false;
            instrExiPtmt.setInt(1,instructorId);
            ResultSet set=instrExiPtmt.executeQuery();
            while (set.next()){
                instrExi=set.getBoolean(1);
            }
            if (!instrExi){
                throw new IntegrityViolationException();
            }

            int weekListInt=MultiChooseIntGenerator.weekIntGenerator(weekList);
            int classTimeInt=MultiChooseIntGenerator.classTimeIntGenerator(classStart,classEnd);
            executeNoCoincidencePtmt.setInt(1,instructorId);
            executeNoCoincidencePtmt.setInt(2,dayOfWeek.getValue());
            executeNoCoincidencePtmt.setInt(3,semesterid);
            executeNoCoincidencePtmt.setInt(4,weekListInt);
            executeNoCoincidencePtmt.setInt(5,classTimeInt);
            executeNoCoincidencePtmt.setString(6,location);
            ResultSet checkCoin=executeNoCoincidencePtmt.executeQuery();
            boolean canpass=false;
            while (checkCoin.next()){
                canpass=checkCoin.getBoolean(1);
            }
            if (!canpass){
                throw new IntegrityViolationException();
            }

            executeNoCoinAboutClassPtmt.setInt(1,semesterid);
            executeNoCoinAboutClassPtmt.setInt(2,weekListInt);
            executeNoCoinAboutClassPtmt.setInt(3,dayOfWeek.getValue());
            executeNoCoinAboutClassPtmt.setInt(4,classTimeInt);
            executeNoCoinAboutClassPtmt.setString(5,location);
            checkCoin=executeNoCoinAboutClassPtmt.executeQuery();
            canpass=false;
            while (checkCoin.next()){
                canpass=checkCoin.getBoolean(1);
            }
            if (!canpass){
                throw new IntegrityViolationException();
            }

            executeNoConfClassWithSameSection.setInt(1,sectionId);
            executeNoConfClassWithSameSection.setInt(2,weekListInt);
            executeNoConfClassWithSameSection.setInt(3,dayOfWeek.getValue());
            executeNoConfClassWithSameSection.setInt(4,classTimeInt);
            canpass=false;
            checkCoin=executeNoConfClassWithSameSection.executeQuery();
            while (checkCoin.next()){
                canpass=checkCoin.getBoolean(1);
            }
            if (!canpass){
                throw new IntegrityViolationException();
            }

            insptmt.setInt(1,sectionId);
            insptmt.setInt(2,dayOfWeek.getValue());
            insptmt.setInt(3, weekListInt);
            insptmt.setInt(4,classTimeInt);
            insptmt.setString(5,location);
            insptmt.executeUpdate();
            int id=0;
            ResultSet nowid=idptmt.executeQuery();
            while (nowid.next()){
                id=nowid.getInt(1);
            }
            teachInsertPtmt.setInt(1,instructorId);
            teachInsertPtmt.setInt(2,id-1);
            teachInsertPtmt.executeUpdate();
            setptmt.setInt(1,id-1);
            setptmt.executeQuery();
            return id-1;
        }
        catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeCourse(String courseId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectCoursePtmt=conn.prepareStatement("select * from course where courseId=?");
                PreparedStatement removeCoursePtmt=conn.prepareStatement("delete from course where courseId=?");
                )
        {
            String courseId_temp="tingting";
            selectCoursePtmt.setString(1,courseId);
            ResultSet set=selectCoursePtmt.executeQuery();
            while (set.next()){
                courseId_temp=set.getString("courseId");
            }
            if (courseId_temp.equals("tingting")){
                throw new EntityNotFoundException();
            }
            else{
                removeCoursePtmt.setString(1,courseId);
                removeCoursePtmt.executeUpdate();
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSection(int sectionId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectSectionPtmt=conn.prepareStatement("select * from section where sectionId=?");
                PreparedStatement removeSectionPtmt=conn.prepareStatement("delete from section where sectionId=?");
                )
        {
            int sectionId_temp=-1;
            selectSectionPtmt.setInt(1,sectionId);
            ResultSet set=selectSectionPtmt.executeQuery();
            while (set.next()){
                sectionId_temp=set.getInt("sectionId");
            }
            if (sectionId_temp==-1){
                throw new EntityNotFoundException();
            }
            else{
                removeSectionPtmt.setInt(1,sectionId);
                removeSectionPtmt.executeUpdate();
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectClassPtmt=conn.prepareStatement("select * from class where classId=?");
                PreparedStatement removeClassPtmt=conn.prepareStatement("delete from class where classId=?");
        )
        {
            int classId_temp=-1;
            selectClassPtmt.setInt(1,classId);
            ResultSet set=selectClassPtmt.executeQuery();
            while (set.next()){
                classId_temp=set.getInt("classId");
            }
            if (classId_temp==-1){
                throw new EntityNotFoundException();
            }
            else{
                removeClassPtmt.setInt(1,classId);
                removeClassPtmt.executeUpdate();
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<Course> getAllCourses() {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getAllCoursesPtmt=conn.prepareStatement("select * from course");
                )
        {
            ArrayList<Course> courses=new ArrayList<>();
            ResultSet set=getAllCoursesPtmt.executeQuery();
            int grading=-1;
            while (set.next()){
                Course course=new Course();
                course.id=set.getString("CourseId");
                course.name=set.getString("courseName");
                course.credit=set.getInt("credit");
                course.classHour=set.getInt("classHour");
                grading=set.getInt("courseGrading");
                if (grading==1){
                    course.grading=Course.CourseGrading.PASS_OR_FAIL;
                }
                else {
                    course.grading=Course.CourseGrading.HUNDRED_MARK_SCORE;
                }
                courses.add(course);
            }
            return courses;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectCoursePtmt=conn.prepareStatement("select * from course where courseId=?");
                PreparedStatement selectSemesterPtmt=conn.prepareStatement("select * from semester where semesterId=?");
                PreparedStatement getCourseSectionInSemesterPtmt=conn.prepareStatement("select * from section where courseId=? and semesterId=?");
                )
        {
            String courseId_temp="tingting";
            selectCoursePtmt.setString(1,courseId);
            ResultSet set=selectCoursePtmt.executeQuery();
            while (set.next()){
                courseId_temp=set.getString("courseId");
            }
            if (courseId_temp.equals("tingting")){
                throw new EntityNotFoundException();
            }
            selectSemesterPtmt.setInt(1,semesterId);
            int semesterid=-1;
            set=selectSemesterPtmt.executeQuery();
            while (set.next()){
                semesterid=set.getInt(1);
            }
            if (semesterid==-1){
                throw new EntityNotFoundException();
            }
            getCourseSectionInSemesterPtmt.setString(1,courseId);
            getCourseSectionInSemesterPtmt.setInt(1,semesterId);
            set=getCourseSectionInSemesterPtmt.executeQuery();
            ArrayList<CourseSection> sections =new ArrayList<>();
            while (set.next()){
                CourseSection section=new CourseSection();
                section.id=set.getInt("sectionId");
                section.name=set.getString("sectionName");
                section.totalCapacity=set.getInt("totalCapacity");
                section.leftCapacity=set.getInt("leftCapacity");
                sections.add(section);
            }
            return sections;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectSectionPtmt=conn.prepareStatement("select * from section where sectionId=?");
                PreparedStatement selectCoursePtmt=conn.prepareStatement("select * from course where courseId=?")
                )
        {
            String courseId="tingting";
            selectSectionPtmt.setInt(1,sectionId);
            ResultSet set=selectSectionPtmt.executeQuery();
            while (set.next()){
                courseId=set.getString("courseId");
            }
            if (courseId.equals("tingting")){
                throw new EntityNotFoundException();
            }
            Course course=new Course();
            selectCoursePtmt.setString(1,courseId);
            set=selectCoursePtmt.executeQuery();
            int grading=-1;
            while (set.next()){
                course.id=courseId;
                course.name=set.getString("courseName");
                course.credit=set.getInt("credit");
                course.classHour=set.getInt("classHour");
                grading=set.getInt("courseGrading");
                if (grading==1){
                    course.grading=Course.CourseGrading.PASS_OR_FAIL;
                }
                else {
                    course.grading=Course.CourseGrading.HUNDRED_MARK_SCORE;
                }
            }
            return course;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectSectionPtmt=conn.prepareStatement("select * from section where sectionId=?");
                PreparedStatement getCourseSectionClassesPtmt=conn.prepareStatement("select * from class where sectionId=?");
                PreparedStatement getInstructorPtmt=conn.prepareStatement(
                        "select *\n" +
                        "from (\n" +
                        "        select *\n" +
                        "        from teach\n" +
                        "        where classId=?\n" +
                        "     ) as insid join instructor on insid.instructorId=instructor.instructorId")
                )
        {

            ArrayList<CourseSectionClass> list=new ArrayList<>();
            int sectionId_temp=-1;
            selectSectionPtmt.setInt(1,sectionId);
            ResultSet set=selectSectionPtmt.executeQuery();
            while (set.next()){
                sectionId_temp=set.getInt("sectionId");
            }
            if (sectionId_temp==-1){
                throw new EntityNotFoundException();
            }
            getCourseSectionClassesPtmt.setInt(1,sectionId);
            set=getCourseSectionClassesPtmt.executeQuery();
            ResultSet aset=null;
            while (set.next()){
                CourseSectionClass tclass=new CourseSectionClass();
                Instructor ins=new Instructor();
                tclass.id=set.getInt("classId");
                tclass.dayOfWeek=DayOfWeek.of(set.getInt("dayOfWeek"));
                tclass.weekList= IntTox.intToList(set.getInt("weekList"));
                tclass.classBegin=IntTox.getBegin(set.getInt("classTime"));
                tclass.classEnd=IntTox.getEnd(set.getInt("classTime"));
                tclass.location=set.getString("location");
                getInstructorPtmt.setInt(1,tclass.id);
                aset=getInstructorPtmt.executeQuery();
                while (aset.next()){
                    ins.id=aset.getInt("instructorId");
                    ins.fullName=aset.getString("firstName")+" "+aset.getString("lastName");
                }
                tclass.instructor=ins;
                list.add(tclass);
            }
            return list;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<CourseSectionClass>();
        }
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectClassPtmt=conn.prepareStatement("select * from class where classId=?");
                PreparedStatement selectSectionPtmt=conn.prepareStatement("select * from section where sectionId=?");
                )
        {
            int sectionId=-1;
            selectClassPtmt.setInt(1,classId);
            ResultSet set=selectClassPtmt.executeQuery();
            while (set.next()){
                sectionId=set.getInt("sectionId");
            }
            if (sectionId==-1){
                throw new EntityNotFoundException();
            }
            selectSectionPtmt.setInt(1,sectionId);
            set=selectSectionPtmt.executeQuery();
            CourseSection section=new CourseSection();
            while (set.next()){
                section.id=sectionId;
                section.name=set.getString("sectionName");
                section.totalCapacity=set.getInt("totalCapacity");
                section.leftCapacity=set.getInt("leftCapacity");
            }
            return section;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getEnrolledStudentPtmt=conn.prepareStatement(
                        "select studentId,firstName,lastName,enrolledDate,majorId,majorName,departmentId,departmentName\n" +
                        "from(\n" +
                        "    select *\n" +
                        "    from (\n" +
                        "            select *\n" +
                        "            from course\n" +
                        "            where courseId=?\n" +
                        "         ) theCourse natural join section\n" +
                        "    where semesterId=?\n" +
                        "    ) theSections natural join enroll natural join student natural join major natural join department");
                PreparedStatement courseExiPtmt=conn.prepareStatement("select exists(select * from course where courseId=?)");
                PreparedStatement semesterExiPtmt=conn.prepareStatement("select exists(select * from section where sectionId=?)");
                )
        {
                boolean courseExi=false;
                courseExiPtmt.setString(1,courseId);
                ResultSet set=courseExiPtmt.executeQuery();
                while (set.next()){
                    courseExi=set.getBoolean(1);
                }
                if (!courseExi){
                    throw new EntityNotFoundException();
                }

                boolean semesterExi=false;
                semesterExiPtmt.setInt(1,semesterId);
                set=semesterExiPtmt.executeQuery();;
                while (set.next()){
                    semesterExi=set.getBoolean(1);
                }
                if (!semesterExi){
                    throw new EntityNotFoundException();
                }

                ArrayList<Student> list=new ArrayList<>();
                getEnrolledStudentPtmt.setString(1,courseId);
                getEnrolledStudentPtmt.setInt(2,semesterId);
                set=getEnrolledStudentPtmt.executeQuery();
                while (set.next()){
                    Student stu=new Student();
                    Major ma=new Major();
                    Department dep=new Department();
                    stu.id=set.getInt("studentId");
                    stu.fullName=UserServiceImp.getFullName(set.getString("firstName"),set.getString("lastName"));
                    stu.enrolledDate=set.getDate("enrolledDate");
                    ma.id=set.getInt("majorId");
                    ma.name=set.getString("majorName");
                    dep.id=set.getInt("departmentId");
                    dep.name=set.getString("departmentName");
                    ma.department=dep;
                    stu.major=ma;
                    list.add(stu);
                }
                return list;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return List.of();
    }




    public static void main(String[] args) throws SQLException {
        Prerequisite KDK = new AndPrerequisite(List.of(
                new AndPrerequisite(List.of(new OrPrerequisite(List.of(new CoursePrerequisite("I"),new CoursePrerequisite("J"))),new CoursePrerequisite("F"))),
                new OrPrerequisite(List.of(new AndPrerequisite(List.of(new CoursePrerequisite("K"),new CoursePrerequisite("L"))),new CoursePrerequisite("H"))),
                new CoursePrerequisite("D")
        ));

        short a=1;
        short b=2;
        short c=3;
        short d=4;
        short e=5;
        short f=6;
        short g=7;
        short h=8;
        Prerequisite LUL=new OrPrerequisite(List.of(new OrPrerequisite(List.of(new CoursePrerequisite("I"),new CoursePrerequisite("F"))),
                new AndPrerequisite(List.of(new CoursePrerequisite("K")))));
        CourseServiceImp imp=new CourseServiceImp();

        List<Student> list=null;
        list=imp.getEnrolledStudentsInSemester("Ifdsf",3);
        for (Student s:list){
            System.out.println(s.fullName+" "+s.id+" "+s.enrolledDate+" "+s.major.id+" "+s.major.name+" "+s.major.department.id+" "+s.major.department.name);
        }

        //imp.addCourseSectionClass(2,101,DayOfWeek.SUNDAY,List.of(a,b),a,b,"A");
//        imp.addCourseSectionClass(3,102,DayOfWeek.SUNDAY,List.of(b,d),a,b,"B");
//        imp.addCourse("LUL","LUL",1,1, Course.CourseGrading.PASS_OR_FAIL,
//        LUL);


//        imp.addCourseSectionClass(4,101,DayOfWeek.SATURDAY,List.of(g,h),g,h,"A");
//        imp.addCourseSectionClass(4,102,DayOfWeek.SUNDAY,List.of(f,g),f,g,"B");

       /* imp.addCourse("I","I",1,1, Course.CourseGrading.HUNDRED_MARK_SCORE
            ,null);
        imp.addCourse("J","J",1,1, Course.CourseGrading.HUNDRED_MARK_SCORE
                ,null);
        imp.addCourse("K","K",1,1, Course.CourseGrading.HUNDRED_MARK_SCORE
                ,null);
        imp.addCourse("L","L",1,1, Course.CourseGrading.HUNDRED_MARK_SCORE
                ,null);
        imp.addCourse("H","H",1,1, Course.CourseGrading.HUNDRED_MARK_SCORE
                ,null);
        imp.addCourse("D","D",1,1, Course.CourseGrading.HUNDRED_MARK_SCORE
                ,null);*/

        /*imp.addCourse("F","F",1,1, Course.CourseGrading.HUNDRED_MARK_SCORE
                ,null);
        imp.addCourse("JIM","JIM",1,1, Course.CourseGrading.PASS_OR_FAIL
        ,KDK);*/

        /*Prerequisite calculus = new OrPrerequisite(List.of(
                new CoursePrerequisite("MA101A"),
                new CoursePrerequisite("MA101B")
        ));
        Prerequisite algebra = new CoursePrerequisite("MA103A");
        Prerequisite prerequisite = new AndPrerequisite(List.of(calculus, algebra));
        String s=getPreString(KDK);
        System.out.println(s);
        System.out.println(s.replaceAll("\\*"," AND ").replaceAll("\\+"," OR "));*/

      /*  Prerequisite calculus = new OrPrerequisite(List.of(
                new CoursePrerequisite("MA101A"),
                new CoursePrerequisite("MA101B")
        ));*/


 /*       CourseServiceImp dimp=new CourseServiceImp();
        dimp.addCourseSection("MA101",3,"中文二班",100);
        dimp.addCourseSection("MA101",4,"中文二班",100);*/


        /*try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement ptmt=conn.prepareStatement("insert into testserial(val) values (111)");
                )
        {
            ptmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }*/

       /* CourseServiceImp eimp=new CourseServiceImp();
        short a=1;
        short b=2;
        short c=3;
        short d=4;
        short e=5;
        System.out.println(eimp.addCourseSectionClass(2,111,DayOfWeek.SUNDAY,List.of(a,b,c,d,e),a,e,"A"));*/

       /* CourseServiceImp kimp=new CourseServiceImp();
        kimp.removeCourse("MA101");*/

     /*   CourseServiceImp wimp=new CourseServiceImp();
        List<CourseSectionClass> list=wimp.getCourseSectionClasses(3);
        for (CourseSectionClass c:list){
            System.out.println(c.id+" "+c.instructor.id);*/

       /* CourseServiceImp limp=new CourseServiceImp();
        CourseSection section= limp.getCourseSectionByClass(5);
        System.out.println(section.id+" "+section.name);*/

       /* CourseServiceImp oimp=new CourseServiceImp();
        oimp.addCourse("MD101","魔法导论",10,50,Course.CourseGrading.HUNDRED_MARK_SCORE,null);*/

//        CourseServiceImp jimp=new CourseServiceImp();
//        jimp.removeCourseSectionClass(3);

 /*       CourseServiceImp imp=new CourseServiceImp();
        Prerequisite calculus = new OrPrerequisite(List.of(
                new CoursePrerequisite("MA101A"),
                new CoursePrerequisite("MA101B")
        ));
        imp.addCourse("MA707","CESHI",1,
                1, Course.CourseGrading.HUNDRED_MARK_SCORE,calculus);
*/
    }
}
