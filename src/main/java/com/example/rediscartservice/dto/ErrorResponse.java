package com.example.rediscartservice.dto;

import java.time.Instant;

public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String timestamp;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
}
