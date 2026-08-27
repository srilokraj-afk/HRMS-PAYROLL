package com.payroll.application.service;
import com.payroll.application.dto.AnalyticsResponse;
import com.payroll.application.model.Employee;
import com.payroll.application.repository.EmployeeRepository;
import com.payroll.application.repository.PayrollRecordRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class AnalyticsService {
 private final EmployeeRepository employees; private final PayrollRecordRepository payroll;
 public AnalyticsService(EmployeeRepository e,PayrollRecordRepository p){employees=e;payroll=p;}
 public AnalyticsResponse analytics(){
  List<Employee> es=employees.findAll();
  double total=es.stream().mapToDouble(e->e.getBasicSalary()+e.getAllowances()-e.getDeductions()).sum();
  double avg=es.isEmpty()?0:total/es.size();
  List<AnalyticsResponse.DepartmentStat> deps=es.stream().collect(Collectors.groupingBy(e->Optional.ofNullable(e.getDepartment()).orElse("Unassigned"))).entrySet().stream()
   .map(x->new AnalyticsResponse.DepartmentStat(x.getKey(),x.getValue().stream().mapToDouble(e->e.getBasicSalary()+e.getAllowances()-e.getDeductions()).sum(),x.getValue().size())).sorted(Comparator.comparingDouble(AnalyticsResponse.DepartmentStat::payroll).reversed()).toList();
  Map<String,Double> trend=new TreeMap<>();
  payroll.findAll().forEach(r->trend.merge(r.getPayrollMonth(),r.getNetSalary(),Double::sum));
  List<AnalyticsResponse.TrendPoint> trends=trend.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->new AnalyticsResponse.TrendPoint(e.getKey(),e.getValue())).toList();
  return new AnalyticsResponse(total,avg,es.size(),deps,trends);
 }
}
