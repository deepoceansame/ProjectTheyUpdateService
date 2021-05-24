package implementingServiceByTXY;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.CourseSectionClass;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.service.CourseService;
import cn.edu.sustech.cs307.database.SQLDataSource;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.List;

public class CourseServiceImp implements CourseService{
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite coursePrerequisite) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement ptmt =conn.prepareStatement("insert into " +
                        "course(courseId,courseName,credit,classHour,courseGrading,pre) " +
                        "values(?,?,?,?,?,?)")
            )
        {
            ptmt.setString(1,courseId);
            ptmt.setString(2,courseName);
            ptmt.setInt(3,credit);
            ptmt.setInt(4,classHour);
            ptmt.setString(5,grading.toString());
            ptmt.setString(6,getPreString(coursePrerequisite));
            ptmt.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
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

       /* Prerequisite calculus = new OrPrerequisite(List.of(
                new CoursePrerequisite("MA101A"),
                new CoursePrerequisite("MA101B")
        ));

        CourseServiceImp cimp=new CourseServiceImp();
        cimp.addCourse("MA101","微积分",4,16, Course.CourseGrading.PASS_OF_FAIL ,calculus);
*/

        CourseServiceImp dimp=new CourseServiceImp();
        dimp.addCourseSection("MA101",3,"中文二班",100);
        dimp.addCourseSection("MA101",4,"中文二班",100);




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
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
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
            return -1;
        }
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, List<Short> weekList, short classStart, short classEnd, String location) {
        return 0;
    }

    @Override
    public void removeCourse(String courseId) {

    }

    @Override
    public void removeCourseSection(int sectionId) {

    }

    @Override
    public void removeCourseSectionClass(int classId) {

    }

    @Override
    public List<Course> getAllCourses() {
        return null;
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
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
