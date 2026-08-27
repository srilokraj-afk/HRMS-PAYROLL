package com.payroll.application.dto;
import java.time.LocalDateTime;
public record AuditLogResponse(Long id,String username,String action,String entityType,String entityId,
 String details,LocalDateTime createdAt) {}
