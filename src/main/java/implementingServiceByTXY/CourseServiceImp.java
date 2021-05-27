package implementingServiceByTXY;
import Tools.MultiChooseIntGenerator;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.CourseSectionClass;
import cn.edu.sustech.cs307.dto.Student;
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
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite coursePrerequisite) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement addCoursePtmt =conn.prepareStatement("insert into " +
                        "course(courseId,courseName,credit,classHour,courseGrading,pre) " +
                        "values(?,?,?,?,?,?)")
            )
        {
            addCoursePtmt .setString(1,courseId);
            addCoursePtmt .setString(2,courseName);
            addCoursePtmt .setInt(3,credit);
            addCoursePtmt .setInt(4,classHour);
            if (grading.equals(Course.CourseGrading.PASS_OR_FAIL))
                addCoursePtmt .setInt(5,1);
            else
                addCoursePtmt .setInt(5,2);
            addCoursePtmt .setString(6,getPreString(coursePrerequisite));
            addCoursePtmt .executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    public static void main(String[] args) {
/*        Prerequisite KDK = new AndPrerequisite(List.of(
                new AndPrerequisite(List.of(new OrPrerequisite(List.of(new CoursePrerequisite("I"),new CoursePrerequisite("J"))),new CoursePrerequisite("F"))),
                new OrPrerequisite(List.of(new AndPrerequisite(List.of(new CoursePrerequisite("K"),new CoursePrerequisite("L"))),new CoursePrerequisite("H"))),
                new CoursePrerequisite("D")
        ));
        Prerequisite calculus = new OrPrerequisite(List.of(
                new CoursePrerequisite("MA101A"),
                new CoursePrerequisite("MA101B")
        ));
        Prerequisite algebra = new CoursePrerequisite("MA103A");
        Prerequisite prerequisite = new AndPrerequisite(List.of(calculus, algebra));
        String s=getPreString(KDK);
        System.out.println(s);
        System.out.println(s.replaceAll("\\*"," AND ").replaceAll("\\+"," OR "));*/

        Prerequisite calculus = new OrPrerequisite(List.of(
                new CoursePrerequisite("MA101A"),
                new CoursePrerequisite("MA101B")
        ));

        CourseServiceImp cimp=new CourseServiceImp();
        cimp.addCourse("MA101","微积分",4,16, Course.CourseGrading.PASS_OR_FAIL ,calculus);

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
        eimp.addCourseSectionClass(1,110,DayOfWeek.FRIDAY,List.of(a,b,c),b,d,"B");
        eimp.addCourseSectionClass(6,110,DayOfWeek.FRIDAY,List.of(c,d,e),d,e,"B");*/


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
                PreparedStatement getSectionSemsterIdPtmt=conn.prepareStatement("select semesterid from section where sectionid=?");
                PreparedStatement idptmt=conn.prepareStatement("select nextval('class_classid_seq')");
                PreparedStatement insptmt=conn.prepareStatement("insert into class" +
                        "(sectionId, dayOfWeek, weekList,classTime,semesterId,location) VALUES(?,?,?,?,?,?)");
                PreparedStatement setptmt=conn.prepareStatement("select setval('class_classid_seq',?)");
                PreparedStatement teachInsertPtmt=conn.prepareStatement("insert into teach " +
                        "(instructorId,classId) values(?,?)");
        )
        {
            if (classEnd<classStart) throw new IntegrityViolationException();
            int semesterid=0;
            getSectionSemsterIdPtmt.setInt(1,sectionId);
            ResultSet res=getSectionSemsterIdPtmt.executeQuery();
            while (res.next()){
                semesterid=res.getInt(1);
            }
            insptmt.setInt(1,sectionId);
            insptmt.setInt(2,dayOfWeek.getValue());
            insptmt.setInt(3, MultiChooseIntGenerator.weekIntGenerator(weekList));
            insptmt.setInt(4,MultiChooseIntGenerator.classTimeIntGenerator(classStart,classEnd));
            insptmt.setInt(5,semesterid);
            insptmt.setString(6,location);
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
                PreparedStatement getCourseSectionInSemesterPtmt=conn.prepareStatement("select * from ")
                )
        {

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        return null;
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        return null;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        return null;
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        return null;
    }
}
