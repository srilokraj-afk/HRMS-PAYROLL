package com.payroll.application.specification;

import com.payroll.application.dto.EmployeeSearchRequest;
import com.payroll.application.model.Employee;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecification {
    private EmployeeSpecification() {}

    public static Specification<Employee> filter(EmployeeSearchRequest request) {
        return Specification.where(nameContains(request.getName()))
                .and(emailContains(request.getEmail()))
                .and(departmentEquals(request.getDepartment()))
                .and(minSalary(request.getMinBasicSalary()))
                .and(maxSalary(request.getMaxBasicSalary()))
                .and(exactSalary(request.getSalary()));
    }

    private static Specification<Employee> nameContains(String value) {
        return value == null || value.isBlank() ? null :
                (root, query, cb) -> cb.like(cb.lower(root.get("name")),
                        "%" + value.toLowerCase() + "%");
    }

    private static Specification<Employee> emailContains(String value) {
        return value == null || value.isBlank() ? null :
                (root, query, cb) -> cb.like(cb.lower(root.get("email")),
                        "%" + value.toLowerCase() + "%");
    }

    private static Specification<Employee> departmentEquals(String value) {
        return value == null || value.isBlank() ? null :
                (root, query, cb) -> cb.equal(cb.lower(root.get("department")),
                        value.toLowerCase());
    }

    private static Specification<Employee> minSalary(Double value) {
        return value == null ? null :
                (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("basicSalary"), value);
    }

    private static Specification<Employee> exactSalary(Double value) {
        return value == null ? null :
                (root, query, cb) -> cb.equal(root.get("basicSalary"), value);
    }

    private static Specification<Employee> maxSalary(Double value) {
        return value == null ? null :
                (root, query, cb) -> cb.lessThanOrEqualTo(root.get("basicSalary"), value);
    }
}
