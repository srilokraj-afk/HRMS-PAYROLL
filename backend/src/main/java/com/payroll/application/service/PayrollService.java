package com.payroll.application.service;

import com.payroll.application.dto.*;
import com.payroll.application.exception.EmployeeNotFoundException;
import com.payroll.application.model.*;
import com.payroll.application.repository.*;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayrollService {
 private static final Logger log=LoggerFactory.getLogger(PayrollService.class);
 private final EmployeeRepository employeeRepository;
 private final PayrollRecordRepository payrollRepository;
 private final AuditService auditService;

 public PayrollService(EmployeeRepository e,PayrollRecordRepository p,AuditService a){
  employeeRepository=e; payrollRepository=p; auditService=a;
 }

 public PayrollResponse calculatePayroll(Long employeeId){
  Employee e=employeeRepository.findById(employeeId).orElseThrow(()->new EmployeeNotFoundException("Employee not found with id: "+employeeId));
  double gross=e.getBasicSalary()+e.getAllowances(), net=gross-e.getDeductions();
  log.info("Payroll calculated for employeeId={}",employeeId);
  return new PayrollResponse(e.getId(),e.getName(),e.getBasicSalary(),e.getAllowances(),e.getDeductions(),gross,net);
 }

 @Transactional
 public MonthlyPayrollResponse runMonthlyPayroll(String month,String username){
  YearMonth ym=parseMonth(month); String m=ym.toString();
  List<Employee> employees=employeeRepository.findAll();
  double gross=0,net=0; int count=0;
  for(Employee e:employees){
   Optional<PayrollRecord> existing=payrollRepository.findByEmployeeIdAndPayrollMonth(e.getId(),m);
   if(existing.isPresent()) { PayrollRecord r=existing.get(); gross+=r.getGrossSalary(); net+=r.getNetSalary(); count++; continue; }
   double g=e.getBasicSalary()+e.getAllowances(), n=g-e.getDeductions();
   PayrollRecord r=new PayrollRecord(e,m,ym.atEndOfMonth(),g,n);
   payrollRepository.save(r); gross+=g; net+=n; count++;
  }
  auditService.log(username,"MONTHLY_PAYROLL_RUN","PAYROLL",m,"Processed "+count+" employees");
  return summary(m);
 }

 @Transactional
 public MonthlyPayrollResponse approveMonth(String month,String username){
  String m=parseMonth(month).toString(); List<PayrollRecord> records=payrollRepository.findByPayrollMonthOrderByEmployee_NameAsc(m);
  if(records.isEmpty()) throw new IllegalArgumentException("No payroll records found for "+m);
  for(PayrollRecord r:records){ if(effectiveStatus(r)==PayrollStatus.CALCULATED || r.getStatus()==PayrollStatus.DRAFT){r.setStatus(PayrollStatus.APPROVED);r.setApprovedAt(LocalDate.now());}}
  payrollRepository.saveAll(records); auditService.log(username,"PAYROLL_APPROVED","PAYROLL",m,"Approved "+records.size()+" records");
  return summary(m);
 }

 @Transactional
 public MonthlyPayrollResponse markPaid(String month,String username){
  String m=parseMonth(month).toString(); List<PayrollRecord> records=payrollRepository.findByPayrollMonthOrderByEmployee_NameAsc(m);
  if(records.isEmpty()) throw new IllegalArgumentException("No payroll records found for "+m);
  boolean invalid=records.stream().anyMatch(r->effectiveStatus(r)!=PayrollStatus.APPROVED && effectiveStatus(r)!=PayrollStatus.PAID);
  if(invalid) throw new IllegalStateException("Payroll must be approved before marking it paid");
  for(PayrollRecord r:records){r.setStatus(PayrollStatus.PAID);r.setPaidAt(LocalDate.now());}
  payrollRepository.saveAll(records); auditService.log(username,"PAYROLL_PAID","PAYROLL",m,"Marked "+records.size()+" records as paid");
  return summary(m);
 }

 public List<PayrollHistoryResponse> history(Long employeeId){
  return payrollRepository.findByEmployeeIdOrderByPayrollDateDesc(employeeId).stream().map(this::map).toList();
 }
 public List<PayrollHistoryResponse> monthHistory(String month){
  return payrollRepository.findByPayrollMonthOrderByEmployee_NameAsc(parseMonth(month).toString()).stream().map(this::map).toList();
 }
 public List<MonthlyPayrollResponse> months(){
  return payrollRepository.findAll().stream().collect(Collectors.groupingBy(PayrollRecord::getPayrollMonth)).keySet().stream().sorted(Comparator.reverseOrder()).map(this::summary).toList();
 }
 public MonthlyPayrollResponse summary(String month){
  List<PayrollRecord> rs=payrollRepository.findByPayrollMonthOrderByEmployee_NameAsc(month);
  double g=rs.stream().mapToDouble(PayrollRecord::getGrossSalary).sum(), n=rs.stream().mapToDouble(PayrollRecord::getNetSalary).sum();
  long c=rs.stream().filter(r->effectiveStatus(r)==PayrollStatus.CALCULATED).count(), a=rs.stream().filter(r->effectiveStatus(r)==PayrollStatus.APPROVED).count(), p=rs.stream().filter(r->effectiveStatus(r)==PayrollStatus.PAID).count();
  String status=rs.isEmpty()?"NOT_RUN":p==rs.size()?"PAID":a+c==rs.size()&&a>0?"APPROVED":c>0?"CALCULATED":"DRAFT";
  return new MonthlyPayrollResponse(month,rs.size(),g,n,c,a,p,status);
 }
 private PayrollStatus effectiveStatus(PayrollRecord r){ return r.getStatus()==null?PayrollStatus.CALCULATED:r.getStatus(); }
 private PayrollHistoryResponse map(PayrollRecord r){return new PayrollHistoryResponse(r.getId(),r.getEmployee().getId(),r.getEmployee().getName(),r.getEmployee().getDepartment(),r.getPayrollMonth(),r.getPayrollDate(),r.getGrossSalary(),r.getNetSalary(),effectiveStatus(r));}
 private YearMonth parseMonth(String month){try{return YearMonth.parse(month);}catch(Exception e){throw new IllegalArgumentException("Month must use YYYY-MM format");}}
}
