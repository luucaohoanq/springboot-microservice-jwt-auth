package com.lcaohoanq.commonlibrary.utils;

import com.lcaohoanq.commonlibrary.dto.ServiceResponse;
import org.springframework.http.ResponseEntity;

public class FeignResponseResolver {

    public static <T> T resolve(ResponseEntity<ServiceResponse<T>> response) {
        if (response == null || response.getBody() == null)
            throw new RuntimeException("Empty response from service");

        ServiceResponse<T> body = response.getBody();

        if (!body.isSuccess()) {
            String reason = body.getReason() != null ? body.getReason() : "Unknown";
            throw new RuntimeException("Remote service error [" + body.getStatus() + "]: " + reason);
        }

        return body.getData();
    }
}
