package com.payroll.application.service;
import com.payroll.application.model.AuditLog;
import com.payroll.application.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
@Service
public class AuditService {
 private final AuditLogRepository repo;
 public AuditService(AuditLogRepository repo){this.repo=repo;}
 public void log(String username,String action,String type,String id,String details){repo.save(new AuditLog(username,action,type,id,details));}
 public Page<AuditLog> recent(int page,int size){return repo.findAllByOrderByCreatedAtDesc(PageRequest.of(page,size));}
}
