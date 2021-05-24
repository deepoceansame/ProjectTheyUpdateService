package implementingServiceByTXY;

import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.service.InstructorService;

import java.util.List;

public class InstructorServiceImp implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {

    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        return null;
    }
}
