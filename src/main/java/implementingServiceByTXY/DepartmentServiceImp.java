package implementingServiceByTXY;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.DepartmentService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DepartmentServiceImp implements DepartmentService {

    @Override
    public synchronized int addDepartment(String name) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement idptmt=conn.prepareStatement("select nextval('department_departmentid_seq')");
                PreparedStatement insptmt=conn.prepareStatement("insert into department " +
                        "(departmentName) VALUES(?)");
                PreparedStatement setptmt=conn.prepareStatement("select setval('department_departmentid_seq',?)");
        )
        {
            insptmt.setString(1,name);
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
    public void removeDepartment(int departmentId) {
        try(
                Connection conn= SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectDepartmentPtmt=conn.prepareStatement("select * from department where departmentId=?");
                PreparedStatement removeDepartmentPtmt=conn.prepareStatement("delete from department where departmentId=?");
        )
        {
            int departmentId_temp=-1;
            selectDepartmentPtmt.setInt(1,departmentId);
            ResultSet set=selectDepartmentPtmt.executeQuery();
            while (set.next()){
                departmentId_temp=set.getInt("departmentId");
            }
            if (departmentId_temp==-1){
                throw new EntityNotFoundException();
            }
            else{
                removeDepartmentPtmt.setInt(1,departmentId);
                removeDepartmentPtmt.executeUpdate();
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement getAllDepsPtmt=conn.prepareStatement("select * from department");
        )
        {
            ArrayList<Department> deps=new ArrayList<>();
            ResultSet set=getAllDepsPtmt.executeQuery();
            while (set.next()){
                Department dep=new Department();
                dep.id=set.getInt("departmentId");
                dep.name=set.getString("departmentName");
                deps.add(dep);
            }
            return deps;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public Department getDepartment(int departmentId) {
        try(
                Connection conn=SQLDataSource.getInstance().getSQLConnection();
                PreparedStatement selectDepartmentPtmt=conn.prepareStatement("select * from department where departmentId=?");
                )
        {
            int departmentId_temp=-1;
            Department dep=new Department();
            selectDepartmentPtmt.setInt(1,departmentId);
            ResultSet set=selectDepartmentPtmt.executeQuery();
            while (set.next()){
                departmentId_temp=set.getInt("departmentId");
                dep.name=set.getString("departmentName");
            }
            if (departmentId_temp==-1){
                throw new EntityNotFoundException();
            }
            else{
                dep.id=departmentId_temp;
            }
            return dep;
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    public static void main(String[] args) {
        DepartmentServiceImp imp=new DepartmentServiceImp();
        System.out.println(imp.addDepartment("魔法部"));
        System.out.println(imp.addDepartment("魔具部"));
        List<Department> list=imp.getAllDepartments();
        for (Department dd:list){
            System.out.println(dd.name);
        }
    }
}
