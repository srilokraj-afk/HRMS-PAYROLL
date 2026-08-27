package com.payroll.application.service;

import com.payroll.application.dto.EmployeeRequest;
import com.payroll.application.dto.EmployeeSearchRequest;
import com.payroll.application.exception.BadRequestException;
import com.payroll.application.exception.EmployeeNotFoundException;
import com.payroll.application.model.Employee;
import com.payroll.application.repository.EmployeeRepository;
import com.payroll.application.specification.EmployeeSpecification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

@Service
public class EmployeeService {
    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "name", "email", "department", "basicSalary", "allowances", "deductions");

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Page<Employee> search(EmployeeSearchRequest request, int page, int size,
                                 String sortBy, String direction) {
        validatePagination(page, size);
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sortBy. Allowed fields: " + ALLOWED_SORT_FIELDS);
        }
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new BadRequestException("direction must be 'asc' or 'desc'");
        }
        validateSalaryFilters(request);

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        log.info("Searching employees: page={}, size={}, sortBy={}, direction={}",
                page, size, sortBy, direction);
        return employeeRepository.findAll(EmployeeSpecification.filter(request), pageable);
    }

    public Employee getEmployeeByIdOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee createEmployee(EmployeeRequest request) {
        Employee employee = new Employee();
        copy(request, employee);
        Employee saved = employeeRepository.save(employee);
        log.info("Employee created with id={}", saved.getId());
        audit("EMPLOYEE_CREATED", saved.getId(), saved.getName());
        return saved;
    }

    public Employee updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = getEmployeeByIdOrThrow(id);
        copy(request, employee);
        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated with id={}", id);
        audit("EMPLOYEE_UPDATED", id, saved.getName());
        return saved;
    }

    public void deleteEmployee(Long id) {
        Employee employee = getEmployeeByIdOrThrow(id);
        employeeRepository.delete(employee);
        log.info("Employee deleted with id={}", id);
        audit("EMPLOYEE_DELETED", id, employee.getName());
    }

    private void audit(String action, Long id, String details) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
            log.info("AUDIT action={} entity=EMPLOYEE id={} user={} details={}", action, id, username, details);
        } catch (Exception ignored) { }
    }

    private void copy(EmployeeRequest request, Employee employee) {
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setBasicSalary(request.getBasicSalary());
        employee.setAllowances(request.getAllowances());
        employee.setDeductions(request.getDeductions());
    }

    private void validateSalaryFilters(EmployeeSearchRequest request) {
        if (request.getSalary() != null && request.getSalary() < 0) {
            throw new BadRequestException("salary must be >= 0");
        }
        if (request.getMinBasicSalary() != null && request.getMinBasicSalary() < 0) {
            throw new BadRequestException("minBasicSalary must be >= 0");
        }
        if (request.getMaxBasicSalary() != null && request.getMaxBasicSalary() < 0) {
            throw new BadRequestException("maxBasicSalary must be >= 0");
        }
        if (request.getMinBasicSalary() != null && request.getMaxBasicSalary() != null
                && request.getMinBasicSalary() > request.getMaxBasicSalary()) {
            throw new BadRequestException("minBasicSalary cannot be greater than maxBasicSalary");
        }
        if (request.getSalary() != null
                && (request.getMinBasicSalary() != null || request.getMaxBasicSalary() != null)) {
            throw new BadRequestException("Use salary alone for exact salary search; do not combine it with min/max salary");
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0) throw new BadRequestException("page must be >= 0");
        if (size < 1 || size > 100) throw new BadRequestException("size must be between 1 and 100");
    }
}
