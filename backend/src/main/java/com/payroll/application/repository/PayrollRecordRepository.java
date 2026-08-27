package com.payroll.application.repository;

import com.payroll.application.model.PayrollRecord;
import com.payroll.application.model.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord,Long> {
    List<PayrollRecord> findByEmployeeIdOrderByPayrollDateDesc(Long employeeId);
    List<PayrollRecord> findByPayrollMonthOrderByEmployee_NameAsc(String payrollMonth);
    Optional<PayrollRecord> findByEmployeeIdAndPayrollMonth(Long employeeId,String payrollMonth);
    long countByPayrollMonthAndStatus(String payrollMonth, PayrollStatus status);
}
