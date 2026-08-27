package com.payroll.application.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false) private String username;
    @Column(nullable=false) private String action;
    @Column(nullable=false) private String entityType;
    private String entityId;
    @Column(length=2000) private String details;
    @Column(nullable=false) private LocalDateTime createdAt;

    public AuditLog(){}
    public AuditLog(String username,String action,String entityType,String entityId,String details){
        this.username=username;this.action=action;this.entityType=entityType;this.entityId=entityId;
        this.details=details;this.createdAt=LocalDateTime.now();
    }
    public Long getId(){return id;} public String getUsername(){return username;} public String getAction(){return action;}
    public String getEntityType(){return entityType;} public String getEntityId(){return entityId;}
    public String getDetails(){return details;} public LocalDateTime getCreatedAt(){return createdAt;}
}
