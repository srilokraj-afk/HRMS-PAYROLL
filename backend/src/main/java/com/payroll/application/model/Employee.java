package com.payroll.application.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String department;
    private double basicSalary;
    private double allowances;
    private double deductions;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PayrollRecord> payrollRecords = new ArrayList<>();

    public Employee() {}

    public Employee(String name, String email, String department,
                     double basicSalary, double allowances, double deductions) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public double getBasicSalary() { return basicSalary; }
    public double getAllowances() { return allowances; }
    public double getDeductions() { return deductions; }
    public List<PayrollRecord> getPayrollRecords() { return payrollRecords; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setDepartment(String department) { this.department = department; }
    public void setBasicSalary(double basicSalary) { this.basicSalary = basicSalary; }
    public void setAllowances(double allowances) { this.allowances = allowances; }
    public void setDeductions(double deductions) { this.deductions = deductions; }
}
