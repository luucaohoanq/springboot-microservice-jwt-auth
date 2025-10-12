package com.lcaohoanq.authserver.feign;

import com.lcaohoanq.commonlibrary.apis.MyApiResponse;
import com.lcaohoanq.commonlibrary.dto.AuthenticationRequest;
import com.lcaohoanq.commonlibrary.dto.RegisterRequest;
import com.lcaohoanq.commonlibrary.dto.ResetPasswordRequest;
import com.lcaohoanq.commonlibrary.dto.ServiceResponse;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${user-service.url:}")
public interface UserFeign {

    @GetMapping("/api/users/email/{email}")
    ResponseEntity<ServiceResponse<UserResponse>> getUserByEmail(@PathVariable String email);

    @GetMapping("/api/users/username/{username}")
    ResponseEntity<ServiceResponse<UserResponse>> getUserByUsername(@PathVariable String username);

    @GetMapping("/api/users/internal/{id}")
    ResponseEntity<ServiceResponse<UserResponse>> getUserById(@PathVariable Long id);

    @PostMapping("/api/users/authenticate")
    ResponseEntity<ServiceResponse<UserResponse>> authenticateUser(@Valid @RequestBody AuthenticationRequest authRequest);

    @PostMapping("/api/users/register")
    ResponseEntity<ServiceResponse<UserResponse>> createUser(@Valid @RequestBody RegisterRequest registerRequest);

    @GetMapping("/api/users/activate-registration")
    ResponseEntity<Void> activateUser(@RequestParam String key);

    // 3 endpoints for reset password

    @PostMapping("/api/reset-password/init")
    ResponseEntity<Void> requestPasswordReset(@RequestParam String email);

    @GetMapping("/reset-password/verify")
    ResponseEntity<Void> verifyResetKey(@RequestParam String key);

    @PostMapping("/reset-password/finish")
    ResponseEntity<Void> finishPasswordReset(@Valid @RequestBody ResetPasswordRequest request);
}