package com.lcaohoanq.authserver.domain.auth;

import com.lcaohoanq.authserver.domain.token.TokenService;
import com.lcaohoanq.authserver.feign.UserFeign;
import com.lcaohoanq.commonlibrary.annotations.RequireRole;
import com.lcaohoanq.commonlibrary.apis.MyApiResponse;
import com.lcaohoanq.commonlibrary.dto.LoginRequest;
import com.lcaohoanq.commonlibrary.dto.LoginResponse;
import com.lcaohoanq.commonlibrary.dto.RegisterRequest;
import com.lcaohoanq.commonlibrary.dto.RefreshTokenRequest;
import com.lcaohoanq.commonlibrary.dto.ResetPasswordRequest;
import com.lcaohoanq.commonlibrary.enums.Role;
import com.lcaohoanq.commonlibrary.exceptions.AccountResourceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint - authenticates user and returns JWT tokens
     * @param loginRequest contains username/email and password
     * @return LoginResponse with access token, refresh token, and user info
     */
    @PostMapping("/login")
    public ResponseEntity<MyApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request
    ) {
        try {
            log.info("Login attempt for user: {}", loginRequest.getUsername());
            LoginResponse response = authService.login(loginRequest, request);
            return MyApiResponse.success(response);
        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getUsername(), e);
            return MyApiResponse.unauthorized("Invalid credentials");
        }
    }

    /**
     * Register endpoint - creates new user account
     * @param registerRequest contains user registration details
     * @return Success message or error
     */
    @PostMapping("/register")
    public ResponseEntity<MyApiResponse<String>> register(
        @Valid @RequestBody RegisterRequest registerRequest
    ) {
        try {
            log.info("Registration attempt for email: {}", registerRequest.getEmail());
            authService.register(registerRequest);
            return MyApiResponse.success("User registered successfully");
        } catch (Exception e) {
            log.error("Registration failed for email: {}", registerRequest.getEmail(), e);
            return MyApiResponse.badRequest("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Refresh token endpoint - generates new access token using refresh token
     * @param refreshTokenRequest contains refresh token
     * @return New access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<MyApiResponse<LoginResponse>> refreshToken(
        @Valid @RequestBody RefreshTokenRequest refreshTokenRequest
    ) {
        try {
            log.info("Token refresh attempt");
            LoginResponse response = authService.refreshToken(refreshTokenRequest);
            return MyApiResponse.success(response);
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return MyApiResponse.unauthorized("Invalid refresh token");
        }
    }

    /**
     * Logout endpoint - invalidates tokens
     * @param request HTTP request containing authorization header
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<MyApiResponse<String>> logout(HttpServletRequest request) {
        try {
            log.info("Logout attempt");
            authService.logout(request);
            return MyApiResponse.success("Logged out successfully");
        } catch (Exception e) {
            log.error("Logout failed", e);
            return MyApiResponse.badRequest("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Validate token endpoint - checks if JWT token is valid
     * @param request HTTP request containing authorization header
     * @return Token validation status
     */
    @PostMapping("/validate")
    public ResponseEntity<MyApiResponse<String>> validateToken(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null && authService.validateToken(token)) {
                return MyApiResponse.success("Token is valid");
            } else {
                return MyApiResponse.unauthorized("Invalid token");
            }
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return MyApiResponse.unauthorized("Token validation failed");
        }
    }

    /**
     * Account activation endpoint - activates user account using activation key
     * This is a public endpoint that users access from their email
     * @param key activation key sent to user's email
     * @return Success or error response
     */
    @GetMapping("/activate")
    public ResponseEntity<MyApiResponse<String>> activateAccount(@RequestParam(value = "key") String key) {
        try {
            log.info("Account activation attempt with key: {}", key);
            authService.activateRegistration(key);
            return MyApiResponse.success("Account activated successfully! You can now log in.");
        } catch (AccountResourceException e) {
            log.error("Account activation failed for key: {}", key, e);
            return MyApiResponse.badRequest("Account activation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during account activation for key: {}", key, e);
            return MyApiResponse.badRequest("Account activation failed: Invalid or expired activation key");
        }
    }


    @PostMapping("/reset-password/init")
    public ResponseEntity<MyApiResponse<String>> requestPasswordReset(@RequestParam("email") String email) {
        try{
            authService.requestPasswordReset(email);
            return MyApiResponse.success();
        }catch(Exception e){
            return MyApiResponse.badRequest("Failed to process password reset request: " + e.getMessage());
        }
    }

    @GetMapping("/reset-password/verify")
    public ResponseEntity<MyApiResponse<String>> verifyResetKey(@RequestParam("key") String key) {
        try{
            authService.verifyResetKey(key);
            return MyApiResponse.success("Password reset key is valid");
        }catch(AccountResourceException e){
            return MyApiResponse.badRequest("Invalid or expired password reset key: " + e.getMessage());
        }catch(Exception e){
            return MyApiResponse.badRequest("Failed to verify password reset key: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password/finish")
    public ResponseEntity<MyApiResponse<String>> finishPasswordReset(
        @RequestBody @Valid ResetPasswordRequest request){
        try{
            authService.finishPasswordReset(request);
            return MyApiResponse.success("Password has been reset successfully");
        } catch (Exception e){
            return MyApiResponse.badRequest("Failed to reset password: " + e.getMessage());
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
