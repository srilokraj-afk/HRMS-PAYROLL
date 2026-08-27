package com.payroll.application.dto;

public class      PayrollResponse {

    private Long employeeId;
    private String employeeName;
    private double basicSalary;
    private double allowances;
    private double deductions;
    private double grossSalary;
    private double netSalary;

    public PayrollResponse(
            Long employeeId,
            String employeeName,
            double basicSalary,
            double allowances,
            double deductions,
            double grossSalary,
            double netSalary
    ) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;
        this.grossSalary = grossSalary;
        this.netSalary = netSalary;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public double getAllowances() {
        return allowances;
    }

    public double getDeductions() {
        return deductions;
    }

    public double getGrossSalary() {
        return grossSalary;
    }

    public double getNetSalary() {
        return netSalary;
    }
}