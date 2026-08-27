package com.payroll.application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "payroll_records",
       uniqueConstraints = @UniqueConstraint(name = "uk_payroll_employee_month",
               columnNames = {"employee_id", "payroll_month"}))
public class PayrollRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate payrollDate;
    @Column(name = "payroll_month")
    private String payrollMonth;
    private double grossSalary;
    private double netSalary;

    @Enumerated(EnumType.STRING)
    @Column
    private PayrollStatus status = PayrollStatus.DRAFT;

    private LocalDate approvedAt;
    private LocalDate paidAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    public PayrollRecord() {}
    public PayrollRecord(Employee employee, String payrollMonth, LocalDate payrollDate,
                         double grossSalary, double netSalary) {
        this.employee = employee; this.payrollMonth = payrollMonth; this.payrollDate = payrollDate;
        this.grossSalary = grossSalary; this.netSalary = netSalary;
        this.status = PayrollStatus.CALCULATED;
    }
    public Long getId(){return id;}
    public LocalDate getPayrollDate(){return payrollDate;}
    public String getPayrollMonth(){return payrollMonth;}
    public double getGrossSalary(){return grossSalary;}
    public double getNetSalary(){return netSalary;}
    public PayrollStatus getStatus(){return status;}
    public LocalDate getApprovedAt(){return approvedAt;}
    public LocalDate getPaidAt(){return paidAt;}
    public Employee getEmployee(){return employee;}
    public void setEmployee(Employee e){this.employee=e;}
    public void setStatus(PayrollStatus s){this.status=s;}
    public void setApprovedAt(LocalDate d){this.approvedAt=d;}
    public void setPaidAt(LocalDate d){this.paidAt=d;}
}
