package com.lcaohoanq.userservice;

import com.lcaohoanq.commonlibrary.apis.MyApiResponse;
import com.lcaohoanq.commonlibrary.dto.AuthenticationRequest;
import com.lcaohoanq.commonlibrary.dto.RegisterRequest;
import com.lcaohoanq.commonlibrary.dto.ServiceResponse;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import com.lcaohoanq.commonlibrary.enums.Role;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Public endpoint - accessible to all authenticated users
    @GetMapping("/profile")
    public ResponseEntity<MyApiResponse<UserResponse>> getMyProfile(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Name") String username,
        @RequestHeader("X-User-Role") String role
    ) {
        log.info("User {} (ID: {}, Role: {}) accessing their profile", username, userId, role);
        
        try {
            var user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));
            return MyApiResponse.success(User.toResponse(user));
        } catch (Exception e) {
            return MyApiResponse.notFound(e.getMessage());
        }
    }

    // USER role and above - accessible to USER, STAFF, ADMIN
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<UserResponse>> getUserById(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") Long currentUserId,
        @RequestHeader("X-User-Name") String username,
        @RequestHeader("X-User-Role") String role
    ) {
        log.info("User {} (Role: {}) requesting user ID: {}", username, role, id);
        
        // Users can only see their own profile unless they are STAFF or ADMIN
        if (!hasPermission(role, Role.STAFF) && !currentUserId.equals(id)) {
            return MyApiResponse.forbidden("Access denied: You can only view your own profile");
        }

        try {
            var user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("User not found"));
            return MyApiResponse.success(User.toResponse(user));
        } catch (Exception e) {
            return MyApiResponse.notFound(e.getMessage());
        }
    }

    // STAFF role and above - accessible to STAFF, ADMIN
    @GetMapping
    public ResponseEntity<MyApiResponse<List<UserResponse>>> getAllUsers(
        @RequestHeader("X-User-Name") String username,
        @RequestHeader("X-User-Role") String role
    ) {
        log.info("User {} (Role: {}) requesting all users", username, role);
        
        if (!hasPermission(role, Role.STAFF)) {
            return MyApiResponse.forbidden("Access denied: STAFF role required");
        }

        return MyApiResponse.success(
            userRepository.findAll().stream()
                .map(User::toResponse)
                .toList()
        );
    }

    // ADMIN only - accessible to ADMIN only
    @GetMapping("/admin/stats")
    public ResponseEntity<MyApiResponse<String>> getAdminStats(
        @RequestHeader("X-User-Name") String username,
        @RequestHeader("X-User-Role") String role
    ) {
        log.info("User {} (Role: {}) requesting admin stats", username, role);
        
        if (!hasPermission(role, Role.ADMIN)) {
            return MyApiResponse.forbidden("Access denied: ADMIN role required");
        }

        long userCount = userRepository.count();
        String stats = String.format("Total users: %d, Admin access granted to: %s", userCount, username);
        
        return MyApiResponse.success(stats);
    }

    // Internal endpoints for Feign clients - these use ServiceResponse for easier deserialization
    @GetMapping("/email/{email}")
    public ResponseEntity<ServiceResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        try {
            var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));
            return ResponseEntity.ok(ServiceResponse.success(User.toResponse(user)));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ServiceResponse.notFound(e.getMessage()));
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<ServiceResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        try {
            var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("User not found"));
            return ResponseEntity.ok(ServiceResponse.success(User.toResponse(user)));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ServiceResponse.notFound(e.getMessage()));
        }
    }

    // Internal endpoint for Feign clients - get user by ID
    @GetMapping("/internal/{id}")
    public ResponseEntity<ServiceResponse<UserResponse>> getUserByIdInternal(@PathVariable Long id) {
        try {
            var user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("User not found"));
            return ResponseEntity.ok(ServiceResponse.success(User.toResponse(user)));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ServiceResponse.notFound(e.getMessage()));
        }
    }

    // Internal authentication endpoint - validates username and password
    @PostMapping("/authenticate")
    public ResponseEntity<ServiceResponse<UserResponse>> authenticateUser(
        @Valid @RequestBody AuthenticationRequest authRequest
    ) {
        try {
            var user = userRepository.findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new Exception("Invalid credentials"));
            
            // Validate password using password encoder
            if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
                return ResponseEntity.status(401).body(ServiceResponse.error(401, "Unauthorized", "Invalid credentials"));
            }
            
            return ResponseEntity.ok(ServiceResponse.success(User.toResponse(user)));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ServiceResponse.error(401, "Unauthorized", "Invalid credentials"));
        }
    }

    // Public registration endpoint - called by auth-service during registration
    @PostMapping("/register")
    public ResponseEntity<ServiceResponse<UserResponse>> registerUser(
        @Valid @RequestBody RegisterRequest registerRequest
    ) {
        log.info("Creating new user: {}", registerRequest.getUsername());
        try {
            var newUser = User.builder()
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .password(registerRequest.getPassword()) // This will be already encrypted by auth-service
                .role(Role.USER)
                .build();
            var savedUser = userRepository.save(newUser);
            return ResponseEntity.ok(ServiceResponse.success(User.toResponse(savedUser)));
        } catch (Exception e) {
            log.error("Failed to create user: {}", e.getMessage());
            return ResponseEntity.status(400).body(ServiceResponse.error(400, "Bad Request", "Failed to create user: " + e.getMessage()));
        }
    }

    // Admin-only user creation endpoint
    @PostMapping("/create")
    public ResponseEntity<MyApiResponse<UserResponse>> createUser(
        @RequestHeader("X-User-Role") String role,
        @Valid @RequestBody RegisterRequest registerRequest
    ) {
        if (!hasPermission(role, Role.ADMIN)) {
            return MyApiResponse.forbidden("Access denied: ADMIN role required to create users");
        }
        try {
            var newUser = User.builder()
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .password(registerRequest.getPassword())
                .role(Role.USER)
                .build();
            var savedUser = userRepository.save(newUser);
            return MyApiResponse.success(User.toResponse(savedUser));
        } catch (Exception e) {
            log.error("Failed to create user: {}", e.getMessage());
            return MyApiResponse.badRequest("Failed to create user: " + e.getMessage());
        }
    }

    // Helper method to check role permissions
    private boolean hasPermission(String userRole, Role requiredRole) {
        try {
            Role currentRole = Role.valueOf(userRole);
            return switch (requiredRole) {
                case USER -> true; // All roles can access USER level
                case STAFF -> currentRole == Role.STAFF || currentRole == Role.ADMIN;
                case ADMIN -> currentRole == Role.ADMIN;
                default -> false;
            };
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role: {}", userRole);
            return false;
        }
    }
}
