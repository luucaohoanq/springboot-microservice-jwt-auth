package com.lcaohoanq.authserver.feign;

import com.lcaohoanq.commonlibrary.exceptions.NotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeignClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Error occurred in Feign client. Method: {}, Status: {}, Reason: {}", 
                  methodKey, response.status(), response.reason());
        
        return switch (response.status()) {
            case 404 -> new NotFoundException("Resource not found");
            case 400 -> new IllegalArgumentException("Bad request");
            case 401 -> new SecurityException("Unauthorized");
            case 403 -> new SecurityException("Forbidden");
            case 500 -> new RuntimeException("Internal server error");
            default -> new RuntimeException("Unexpected error: " + response.reason());
        };
    }
}