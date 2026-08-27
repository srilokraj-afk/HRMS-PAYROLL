package com.payroll.application.controller;
import com.payroll.application.dto.*; import com.payroll.application.model.AuditLog; import com.payroll.application.service.AuditService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/audit") @SecurityRequirement(name="bearerAuth")
public class AuditController {
 private final AuditService service; public AuditController(AuditService s){service=s;}
 @GetMapping public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<AuditLogResponse>>> logs(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
  var result=service.recent(page,Math.min(size,100)).map(x->new AuditLogResponse(x.getId(),x.getUsername(),x.getAction(),x.getEntityType(),x.getEntityId(),x.getDetails(),x.getCreatedAt()));
  return ResponseEntity.ok(new ApiResponse<>(true,"Audit log loaded",result));
 }
}
