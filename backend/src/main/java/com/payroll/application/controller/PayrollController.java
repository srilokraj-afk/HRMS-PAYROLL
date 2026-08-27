package com.payroll.application.controller;

import com.payroll.application.dto.*; import com.payroll.application.model.Employee;
import com.payroll.application.service.*;
import com.payroll.application.repository.EmployeeRepository;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag; import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.*; import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/payroll") @Tag(name="Payroll",description="Payroll calculation, history and monthly workflow APIs")
@SecurityRequirement(name="bearerAuth")
public class   PayrollController {
 private final PayrollService service; private final EmployeeRepository employees; private final PayslipPdfService pdf;
 public PayrollController(PayrollService s,EmployeeRepository e,PayslipPdfService p){service=s;employees=e;pdf=p;}

 @GetMapping("/{employeeId}")
 public ResponseEntity<ApiResponse<PayrollResponse>> calculatePayroll(@PathVariable Long employeeId){
  return ResponseEntity.ok(new ApiResponse<>(true,"Payroll calculated successfully",service.calculatePayroll(employeeId)));
 }
 @GetMapping("/history/{employeeId}")
 public ResponseEntity<ApiResponse<List<PayrollHistoryResponse>>> history(@PathVariable Long employeeId){
  return ResponseEntity.ok(new ApiResponse<>(true,"Payroll history loaded",service.history(employeeId)));
 }
 @GetMapping("/history/me")
 public ResponseEntity<ApiResponse<List<PayrollHistoryResponse>>> myHistory(Authentication auth){
  Employee e=employees.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->new IllegalArgumentException("No employee profile is linked to "+auth.getName()));
  return ResponseEntity.ok(new ApiResponse<>(true,"Payroll history loaded",service.history(e.getId())));
 }
 @GetMapping("/months")
 public ResponseEntity<ApiResponse<List<MonthlyPayrollResponse>>> months(){return ResponseEntity.ok(new ApiResponse<>(true,"Payroll runs loaded",service.months()));}
 @GetMapping("/month/{month}")
 public ResponseEntity<ApiResponse<List<PayrollHistoryResponse>>> month(@PathVariable String month){return ResponseEntity.ok(new ApiResponse<>(true,"Monthly payroll loaded",service.monthHistory(month)));}

 @PostMapping("/run/{month}")
 public ResponseEntity<ApiResponse<MonthlyPayrollResponse>> run(@PathVariable String month,Authentication auth){
  return ResponseEntity.ok(new ApiResponse<>(true,"Monthly payroll processed",service.runMonthlyPayroll(month,auth.getName())));
 }
 @PostMapping("/approve/{month}")
 public ResponseEntity<ApiResponse<MonthlyPayrollResponse>> approve(@PathVariable String month,Authentication auth){
  return ResponseEntity.ok(new ApiResponse<>(true,"Payroll approved",service.approveMonth(month,auth.getName())));
 }
 @PostMapping("/pay/{month}")
 public ResponseEntity<ApiResponse<MonthlyPayrollResponse>> pay(@PathVariable String month,Authentication auth){
  return ResponseEntity.ok(new ApiResponse<>(true,"Payroll marked as paid",service.markPaid(month,auth.getName())));
 }
 @GetMapping("/payslip/{id}")
 public ResponseEntity<byte[]> payslip(@PathVariable Long id){
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
   .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=payslip-"+id+".pdf").body(pdf.generate(id));
 }
}
