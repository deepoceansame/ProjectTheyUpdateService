package ExtendingFactory;

import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.*;
import implementingServiceByTXY.*;

public class TheServiceFactory extends ServiceFactory {
    public TheServiceFactory(){
        registerService(CourseService.class,new CourseServiceImp());
        registerService(DepartmentService.class,new DepartmentServiceImp());
        registerService(InstructorService.class,new InstructorServiceImp());
        registerService(MajorService.class,new MajorServiceImp());
        registerService(SemesterService.class,new SemesterServiceImp());
        registerService(StudentService.class,new StudentServiceImp());
        registerService(UserService.class,new UserServiceImp());
    }
}
