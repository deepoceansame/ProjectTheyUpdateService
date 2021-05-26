package implementingServiceByTXY;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.service.InstructorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class InstructorServiceImp implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement ptmt=conn.prepareStatement("insert into instructor(instructorId,firstName,lastName) values(?,?,?)")
                )
        {
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
        return null;
    }

    public static void main(String[] args) {
        InstructorServiceImp imp=new InstructorServiceImp();
        imp.addInstructor(110,"Kate","Yang");
    }
}
