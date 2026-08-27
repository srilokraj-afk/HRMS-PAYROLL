package com.payroll.application.service;

import com.payroll.application.dto.PayrollResponse;
import com.payroll.application.exception.EmployeeNotFoundException;
import com.payroll.application.model.Employee;
import com.payroll.application.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {
    @Mock EmployeeRepository employeeRepository;
    @InjectMocks PayrollService payrollService;

    @Test
    void calculatePayroll_shouldCalculateGrossAndNetSalary() {
        Employee employee = new Employee("John", "john@example.com", "IT", 50000, 5000, 1000);
        when(employeeRepository.findById(1L)).thenReturn(java.util.Optional.of(employee));

        PayrollResponse response = payrollService.calculatePayroll(1L);

        assertEquals(55000, response.getGrossSalary());
        assertEquals(54000, response.getNetSalary());
    }

    @Test
    void calculatePayroll_shouldThrowWhenEmployeeMissing() {
        when(employeeRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(EmployeeNotFoundException.class,
                () -> payrollService.calculatePayroll(99L));
    }
}
