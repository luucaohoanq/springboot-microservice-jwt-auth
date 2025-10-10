package com.lcaohoanq.authserver.feign;

import com.lcaohoanq.commonlibrary.dto.RegisterRequest;
import com.lcaohoanq.commonlibrary.dto.ServiceResponse;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import com.lcaohoanq.commonlibrary.dto.AuthenticationRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserFeign {

    @GetMapping("/api/users/internal/{id}")
    ResponseEntity<ServiceResponse<UserResponse>> getUserById(@PathVariable Long id);

    @GetMapping("/api/users/email/{email}")
    ResponseEntity<ServiceResponse<UserResponse>> getUserByEmail(@PathVariable String email);

    @GetMapping("/api/users/username/{username}")
    ResponseEntity<ServiceResponse<UserResponse>> getUserByUsername(@PathVariable String username);

    @PostMapping("/api/users/register")
    ResponseEntity<ServiceResponse<UserResponse>> createUser(
        @Valid @RequestBody RegisterRequest registerRequest
    );

    @PostMapping("/api/users/authenticate")
    ResponseEntity<ServiceResponse<UserResponse>> authenticateUser(
        @Valid @RequestBody AuthenticationRequest authenticationRequest
    );
}
