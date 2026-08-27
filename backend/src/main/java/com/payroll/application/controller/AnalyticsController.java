package com.payroll.application.controller;
import com.payroll.application.dto.*; import com.payroll.application.service.AnalyticsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/analytics") @SecurityRequirement(name="bearerAuth")
public class AnalyticsController {
 private final AnalyticsService service; public AnalyticsController(AnalyticsService s){service=s;}
 @GetMapping public ResponseEntity<ApiResponse<AnalyticsResponse>> analytics(){return ResponseEntity.ok(new ApiResponse<>(true,"Analytics loaded",service.analytics()));}
}
