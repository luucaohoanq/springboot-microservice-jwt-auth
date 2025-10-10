package com.lcaohoanq.authserver.feign;

import com.lcaohoanq.commonlibrary.exceptions.NotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FeignClientErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == HttpStatus.SC_NOT_FOUND) {
            return new NotFoundException("Resource Not Found");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
