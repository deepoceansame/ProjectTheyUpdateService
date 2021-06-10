import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;

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


    String sear= "                         select sectionId\n"+
            "                         from(\n"+
            "                            select sectionId\n" +
            "                            from(\n" +
            "                                select *\n" +
            "                                from(\n" +
            "                                    select secTocous.sectionId, semesterId, sectionName, totalCapacity, leftCapacity, courseId, courseName, credit, classHour, courseGrading, pre, preId, classId, dayOfWeek, weekList, classTime, location\n" +
            "                                    from\n" +
            "                                    (\n" +
            "                                        select *\n" +
            "                                        from\n" +
            "                                            (\n" +
            "                                                select *\n" +
            "                                                from section\n" +
            "                                                where (semesterId=?) and \n" +  //--1 1
            "                                                    (leftCapacity>0 or ?) and \n" + //--1 2
            "                                                    ( noconfclass(sectionId,?) or ? ) and\n" + // --2 3,4
            "                                                    (not hasPassed(sectionId,?) or ?) \n" + //--2 5,6
            "                                            ) as secs natural join\n" +
            "                                            (\n" +
            "                                                select *\n" +
            "                                                from course\n" +
            "                                                where (getans(preId,?) or ?) and\n" + // --2 7,8
            "                                                (isCompulsory(courseId,?) or ?) and \n" + //--2 9,10
            "                                                (isElective(courseId,?) or ?) and \n" + //--2  11,12
            "                                                (isCross(courseId,?) or ?) and \n" + //--2 13,14
            "                                                (isPublic(courseId) or ?) and\n" + // --1 15
            "                                                (courseId like '%'||?||'%' or ?)\n" + //--2 16,17
            "                                            ) as cous\n" +
            "                                        where ( (courseName||'['||sectionName||']') like '%'||?||'%' or ? ) \n" + // --2 18,19
            "                                    ) as secTocous left outer join class on secTocous.sectionId=class.sectionId\n" +
            "                                    where ((dayOfWeek=? or ?) and                        \n" + // --2 20,21
            "                                          (?&classTime!=0 or ?) and                     \n" +  //--2 22,23
            "                                          (location=any (?) or ?) )  or (classId is null and ?)\n" + //--3 24,25,26
            "                                ) as secToclas\n" +
            "                                    left outer join teach on teach.classId=secToclas.classId\n" +
            "                            ) as secToteach left outer join instructor on secToteach.instructorId=instructor.instructorId\n" +
            "                            where ( (firstName like ?||'%' or lastName like ?||'%' or firstName||lastName like ?||'%' or firstName||' '||lastName like ?||'%')   or ?  ) or (instructor.instructorId is null and ?)\n" +
            "                            group by sectionId\n" +
            "                            ) secids natural join section natural join course\n"+
            "                            order by courseId,courseName||'['||section.sectionName||']'\n"+
            "                            limit ?\n" +
            "                            offset ?;";



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
                    "                            (leftCapacity>0 or true) and\n" +  //2
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
                    "                        (courseId like '%'||?||'%' or true)\n" +//16 17
                    "                    ) as cou\n" +
                    "            where ( (courseName||'['||sectionName||']') like '%'||?||'%' or true )\n" + //18 19
                    "        ) as secToCou\n" +
                    "        natural join\n" +
                    "        (\n" +
                    "            select *\n" +
                    "            from class\n" +
                    "            where (dayOfWeek=? or ?) and\n" + //20,21
                    "                  (?&classTime!=0 or ?) and\n" +// 22,23
                    "                  (location=any (?) or ?)\n" + //24,25
                    "        ) as claz\n" +
                    "        natural join teach natural join\n" +
                    "        (\n" +
                    "            select *\n" +
                    "            from instructor\n" +
                    "            where ( (firstName like ?||'%' or lastName like ?||'%' or firstName||lastName like ?||'%' or firstName||' '||lastName like ?||'%')   or ?  )\n" + //26 27 28 29 30
                    "        ) as ins\n" +
                    "    group by sectionId\n" +
                    ") secid natural join section natural join course\n" +
                    "order by courseId,courseName||'['||section.sectionName||']'\n" +
                    "limit 100\n" + //31
                    "offset 0;"; //32

    public static void main(String[] args) {
        String s="sd12-12a";
        System.out.println(s.toUpperCase());
    }
}
