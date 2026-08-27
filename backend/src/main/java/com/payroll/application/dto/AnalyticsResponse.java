package com.payroll.application.dto;
import java.util.List;
public record AnalyticsResponse(double totalPayroll,double averageNetSalary,int employeeCount,
 List<DepartmentStat> departments,List<TrendPoint> trends) {
 public record DepartmentStat(String department,double payroll,int employees){}
 public record TrendPoint(String month,double payroll){}
}
