package com.payroll.application.dto;
import com.payroll.application.model.PayrollStatus;
import java.time.LocalDate;
public record PayrollHistoryResponse(Long id, Long employeeId, String employeeName, String department,
 String payrollMonth, LocalDate payrollDate, double grossSalary, double netSalary, PayrollStatus status) {}
