package com.library.dao;

import com.library.db.DatabaseManager;
import com.library.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Student/Member entities.
 * Strictly uses PreparedStatement for SQL injection defense.
 */
public class StudentDao {

    public boolean addStudent(Student student) {
        String sql = """
            INSERT INTO students (student_code, name, email, department)
            VALUES (?, ?, ?, ?);
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, student.getStudentCode().trim());
            ps.setString(2, student.getName().trim());
            ps.setString(3, student.getEmail().trim());
            ps.setString(4, student.getDepartment().trim());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        student.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
        }
        return false;
    }

    public boolean updateStudent(Student student) {
        String sql = """
            UPDATE students
            SET student_code = ?, name = ?, email = ?, department = ?
            WHERE id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, student.getStudentCode().trim());
            ps.setString(2, student.getName().trim());
            ps.setString(3, student.getEmail().trim());
            ps.setString(4, student.getDepartment().trim());
            ps.setInt(5, student.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating student: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteStudent(int id) {
        String sql = "DELETE FROM students WHERE id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting student: " + e.getMessage());
        }
        return false;
    }

    public Student getStudentById(int id) {
        String sql = """
            SELECT s.*, 
                   (SELECT COUNT(*) FROM transactions t WHERE t.student_id = s.id AND t.status IN ('ISSUED', 'OVERDUE')) AS active_loans
            FROM students s 
            WHERE s.id = ?;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student s = mapRowToStudent(rs);
                    s.setActiveLoansCount(rs.getInt("active_loans"));
                    return s;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting student by ID: " + e.getMessage());
        }
        return null;
    }

    public Student getStudentByCode(String code) {
        String sql = """
            SELECT s.*, 
                   (SELECT COUNT(*) FROM transactions t WHERE t.student_id = s.id AND t.status IN ('ISSUED', 'OVERDUE')) AS active_loans
            FROM students s 
            WHERE LOWER(s.student_code) = LOWER(?);
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student s = mapRowToStudent(rs);
                    s.setActiveLoansCount(rs.getInt("active_loans"));
                    return s;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting student by code: " + e.getMessage());
        }
        return null;
    }

    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = """
            SELECT s.*, 
                   (SELECT COUNT(*) FROM transactions t WHERE t.student_id = s.id AND t.status IN ('ISSUED', 'OVERDUE')) AS active_loans
            FROM students s 
            ORDER BY s.name ASC;
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Student s = mapRowToStudent(rs);
                s.setActiveLoansCount(rs.getInt("active_loans"));
                students.add(s);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all students: " + e.getMessage());
        }
        return students;
    }

    public List<Student> searchStudents(String query, String department) {
        List<Student> students = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT s.*, 
                   (SELECT COUNT(*) FROM transactions t WHERE t.student_id = s.id AND t.status IN ('ISSUED', 'OVERDUE')) AS active_loans
            FROM students s 
            WHERE 1=1 
        """);
        List<String> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (LOWER(s.name) LIKE ? OR LOWER(s.student_code) LIKE ? OR LOWER(s.email) LIKE ?) ");
            String wildcard = "%" + query.trim().toLowerCase() + "%";
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
        }

        if (department != null && !department.trim().isEmpty() && !department.equalsIgnoreCase("All Departments")) {
            sql.append(" AND LOWER(s.department) = LOWER(?) ");
            params.add(department.trim());
        }

        sql.append(" ORDER BY s.name ASC;");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student s = mapRowToStudent(rs);
                    s.setActiveLoansCount(rs.getInt("active_loans"));
                    students.add(s);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching students: " + e.getMessage());
        }
        return students;
    }

    public int getTotalStudentsCount() {
        String sql = "SELECT COUNT(*) FROM students;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total students count: " + e.getMessage());
        }
        return 0;
    }

    public List<String> getDistinctDepartments() {
        List<String> depts = new ArrayList<>();
        String sql = "SELECT DISTINCT department FROM students ORDER BY department ASC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String d = rs.getString(1);
                if (d != null && !d.isBlank()) {
                    depts.add(d);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting distinct departments: " + e.getMessage());
        }
        return depts;
    }

    private Student mapRowToStudent(ResultSet rs) throws SQLException {
        return new Student(
            rs.getInt("id"),
            rs.getString("student_code"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("department")
        );
    }
}
