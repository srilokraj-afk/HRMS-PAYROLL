package com.payroll.application.dto;

public class EmployeeSearchRequest {
    private String name;
    private String email;
    private String department;
    private Double minBasicSalary;
    private Double maxBasicSalary;
    private Double salary;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Double getMinBasicSalary() { return minBasicSalary; }
    public void setMinBasicSalary(Double minBasicSalary) { this.minBasicSalary = minBasicSalary; }
    public Double getMaxBasicSalary() { return maxBasicSalary; }
    public void setMaxBasicSalary(Double maxBasicSalary) { this.maxBasicSalary = maxBasicSalary; }
    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }
}
