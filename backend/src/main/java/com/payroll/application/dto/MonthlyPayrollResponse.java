package com.payroll.application.dto;
public record MonthlyPayrollResponse(String month,int employeeCount,double grossTotal,double netTotal,
 long calculated,long approved,long paid,String status) {}
