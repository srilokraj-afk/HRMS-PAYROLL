package com.payroll.application.service;

import com.payroll.application.dto.EmployeeRequest;
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
class EmployeeServiceTest {
    @Mock EmployeeRepository employeeRepository;
    @InjectMocks EmployeeService employeeService;

    @Test
    void createEmployee_shouldSaveEmployee() {
        EmployeeRequest request = request();
        Employee saved = new Employee("John", "john@example.com", "IT", 50000, 5000, 1000);
        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        Employee result = employeeService.createEmployee(request);

        assertEquals("John", result.getName());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void getEmployeeById_shouldThrowWhenMissing() {
        when(employeeRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(EmployeeNotFoundException.class,
                () -> employeeService.getEmployeeByIdOrThrow(99L));
    }

    @Test
    void deleteEmployee_shouldDeleteExistingEmployee() {
        Employee employee = new Employee();
        when(employeeRepository.findById(1L)).thenReturn(java.util.Optional.of(employee));

        employeeService.deleteEmployee(1L);

        verify(employeeRepository).delete(employee);
    }

    private EmployeeRequest request() {
        EmployeeRequest request = new EmployeeRequest();
        request.setName("John");
        request.setEmail("john@example.com");
        request.setDepartment("IT");
        request.setBasicSalary(50000);
        request.setAllowances(5000);
        request.setDeductions(1000);
        return request;
    }
}
