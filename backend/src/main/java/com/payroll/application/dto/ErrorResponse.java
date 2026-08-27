package com.payroll.application.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {
    private boolean success = false;
    private int status;
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;
    private String errorId;

    public ErrorResponse(int status, String message, Map<String, String> errors) {
        this(status, message, errors, null);
    }

    public ErrorResponse(int status, String message, Map<String, String> errors, String errorId) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
        this.errorId = errorId;
    }

    public boolean isSuccess() { return success; }
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public Map<String, String> getErrors() { return errors; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getErrorId() { return errorId; }
}
