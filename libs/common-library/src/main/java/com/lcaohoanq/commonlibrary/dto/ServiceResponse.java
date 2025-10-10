package com.lcaohoanq.commonlibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple response wrapper for internal service communication (Feign clients)
 * This avoids Jackson deserialization issues with sealed interfaces
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceResponse<T> {
    private int status;
    private String message;
    private T data;
    private String reason;
    private boolean success;
    
    public static <T> ServiceResponse<T> success(T data) {
        return ServiceResponse.<T>builder()
            .status(200)
            .message("Success")
            .data(data)
            .success(true)
            .build();
    }
    
    public static <T> ServiceResponse<T> error(int status, String message, String reason) {
        return ServiceResponse.<T>builder()
            .status(status)
            .message(message)
            .reason(reason)
            .success(false)
            .build();
    }
    
    public static <T> ServiceResponse<T> notFound(String reason) {
        return ServiceResponse.<T>builder()
            .status(404)
            .message("Not Found")
            .reason(reason)
            .success(false)
            .build();
    }
}