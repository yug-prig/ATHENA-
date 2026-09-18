package com.library.model;

/**
 * Entity representing a Student / Member of the library.
 */
public class Student {
    private int id;
    private String studentCode;
    private String name;
    private String email;
    private String department;
    private int activeLoansCount;

    public Student() {
    }

    public Student(int id, String studentCode, String name, String email, String department) {
        this.id = id;
        this.studentCode = studentCode;
        this.name = name;
        this.email = email;
        this.department = department;
    }

    public Student(String studentCode, String name, String email, String department) {
        this(0, studentCode, name, email, department);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    public void setActiveLoansCount(int activeLoansCount) {
        this.activeLoansCount = activeLoansCount;
    }

    @Override
    public String toString() {
        return name + " [" + studentCode + "] - " + department;
    }
}
