package implementingServiceByTXY;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.service.InstructorService;

import javax.print.attribute.standard.ReferenceUriSchemesSupported;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InstructorServiceImp implements InstructorService {
    @Override
    public synchronized void addInstructor(int userId, String firstName, String lastName) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement ptmt=conn.prepareStatement("insert into instructor(instructorId,firstName,lastName) values(?,?,?)");
                PreparedStatement uptmt=conn.prepareStatement("insert into tuser(userId,firstName,lastName,tos) values(?,?,?,?)");
                )
        {
            uptmt.setInt(1,userId);
            uptmt.setString(2,firstName);
            uptmt.setString(3,lastName);
            uptmt.setInt(4,1);
            uptmt.executeUpdate();
            ptmt.setInt(1,userId);
            ptmt.setString(2,firstName);
            ptmt.setString(3,lastName);
            ptmt.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getSectionYouTeachPtmt=conn.prepareStatement("select sectionId,sectionName,totalCapacity,leftCapacity\n" +
                        "from (\n" +
                        "        select *\n" +
                        "       from teach\n" +
                        "        where instructorId=?\n" +
                        "    ) as classTeached natural join class natural join section\n" +
                        "where semesterId=?;");
                PreparedStatement instrExiPtmt= conn.prepareStatement("select exists(select * from instructor where instructorId=?)");
                PreparedStatement semesterExiPtmt=conn.prepareStatement("select exists(select * from section where sectionId=?)");
                )
        {
            boolean instrExi=false;
            instrExiPtmt.setInt(1,instructorId);
            ResultSet set=instrExiPtmt.executeQuery();
            while (set.next()){
                instrExi=set.getBoolean(1);
            }
            if (!instrExi){
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

            ArrayList<CourseSection> list=new ArrayList<>();
            getSectionYouTeachPtmt.setInt(1,instructorId);
            getSectionYouTeachPtmt.setInt(2,semesterId);
            set=getSectionYouTeachPtmt.executeQuery();
            while (set.next()){
                CourseSection section=new CourseSection();
                section.id=set.getInt("sectionId");
                section.name=set.getString("sectionName");
                section.totalCapacity=set.getInt("totalCapacity");
                section.leftCapacity=set.getInt("leftCapacity");
                list.add(section);
            }
            return list;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public static void main(String[] args) {
        InstructorServiceImp imp=new InstructorServiceImp();
//        imp.addInstructor(110,"Kate","Yang");
//        imp.addInstructor(111,"张","三");
        List<CourseSection> list=imp.getInstructedCourseSections(103,1);
        for (CourseSection s:list){
            System.out.println(s.id+" "+s.name+" "+s.leftCapacity+" "+s.totalCapacity);
        }
    }
}
