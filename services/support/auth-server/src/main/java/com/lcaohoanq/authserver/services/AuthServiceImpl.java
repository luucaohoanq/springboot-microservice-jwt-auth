package com.lcaohoanq.authserver.services;

import com.lcaohoanq.authserver.components.JwtTokenUtils;
import com.lcaohoanq.authserver.domain.token.TokenService;
import com.lcaohoanq.authserver.feign.UserFeign;
import com.lcaohoanq.commonlibrary.apis.MyApiResponse;
import com.lcaohoanq.commonlibrary.dto.LoginRequest;
import com.lcaohoanq.commonlibrary.dto.LoginResponse;
import com.lcaohoanq.commonlibrary.dto.RegisterRequest;
import com.lcaohoanq.commonlibrary.dto.RefreshTokenRequest;
import com.lcaohoanq.commonlibrary.dto.ServiceResponse;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserFeign userFeign;
    private final JwtTokenUtils jwtTokenUtils;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("🔐 Starting login process for user: {}", loginRequest.getUsername());
        try {
            // 1️⃣ Create authentication request
            var authRequest = new com.lcaohoanq.commonlibrary.dto.AuthenticationRequest(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );
            log.info("🔐 Created authentication request for user: {}", authRequest.getUsername());

            // 2️⃣ Call user-service authentication endpoint to validate credentials
            log.info("🔐 Calling user-service authenticate endpoint for user: {}", authRequest.getUsername());
            var authResponse = userFeign.authenticateUser(authRequest);
            var body = authResponse.getBody();
            log.info("🔐 Received response from user-service: {}", body != null ? body.isSuccess() : "null");

            // 3️⃣ Check if authentication was successful
            if (body == null || !body.isSuccess()) {
                log.error("🔐 Authentication failed - body is null: {} or not successful: {}", 
                    body == null, body != null ? !body.isSuccess() : "N/A");
                throw new RuntimeException("Invalid credentials");
            }

            // 4️⃣ Get authenticated user data from response
            UserResponse user = body.getData();

            // 5️⃣ Create UserInfo for JWT generation
            var userInfo = new UserResponse(
                user.id(),
                user.username(),
                user.email(),
                user.role()
            );

            // 6️⃣ Generate access token and refresh token
            String accessToken = jwtTokenUtils.generateToken(userInfo);
            String refreshToken = jwtTokenUtils.generateRefreshToken(userInfo);

            // 7️⃣ Store token in database
            tokenService.addToken(user.id(), accessToken, false);

            // 8️⃣ Return LoginResponse
            return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L) // 1 hour
                .refreshExpiresIn(86400L) // 24 hours
                .user(user)
                .build();

        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    @Override
    public void logout(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                Long userId = jwtTokenUtils.extractUserId(token);
                tokenService.deleteToken(token, userId);
                log.info("User {} logged out successfully", userId);
            }
        } catch (Exception e) {
            log.error("Error during logout", e);
            throw new RuntimeException("Logout failed: " + e.getMessage());
        }
    }

    @Override
    public void register(RegisterRequest registerRequest) {
        try {
            // 1️⃣ Check if username already exists
            try {
                var userByUsernameResponse = userFeign.getUserByUsername(registerRequest.getUsername());
                if (userByUsernameResponse.getBody() != null && userByUsernameResponse.getBody().isSuccess()) {
                    throw new RuntimeException("Username already exists");
                }
            } catch (com.lcaohoanq.commonlibrary.exceptions.NotFoundException e) {
                // Expected - username doesn't exist, which is good for registration
                log.debug("Username {} is available", registerRequest.getUsername());
            }

            // 2️⃣ Check if email already exists
            try {
                var userByEmailResponse = userFeign.getUserByEmail(registerRequest.getEmail());
                if (userByEmailResponse.getBody() != null && userByEmailResponse.getBody().isSuccess()) {
                    throw new RuntimeException("Email already exists");
                }
            } catch (com.lcaohoanq.commonlibrary.exceptions.NotFoundException e) {
                // Expected - email doesn't exist, which is good for registration
                log.debug("Email {} is available", registerRequest.getEmail());
            }

            // 3️⃣ Encrypt password
            String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());
            registerRequest.setPassword(encodedPassword);

            // 4️⃣ Create user via user-service
            var createUserResponse = userFeign.createUser(registerRequest);
            if (createUserResponse.getBody() == null || !createUserResponse.getBody().isSuccess()) {
                throw new RuntimeException("Failed to create user");
            }
            
            log.info("User {} registered successfully", registerRequest.getUsername());
            
        } catch (RuntimeException e) {
            log.error("Registration failed for user: {}", registerRequest.getUsername(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during registration for user: {}", registerRequest.getUsername(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest) throws Exception {
        try {
            String refreshToken = refreshTokenRequest.getRefreshToken();
            
            // Validate refresh token
            if (!jwtTokenUtils.validateToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }

            // Extract user info from refresh token
            Long userId = jwtTokenUtils.extractUserId(refreshToken);
            String username = jwtTokenUtils.extractUsername(refreshToken);
            String email = jwtTokenUtils.extractEmail(refreshToken);
            String roleStr = jwtTokenUtils.extractRole(refreshToken);

            // Get fresh user data
            var userResponse = userFeign.getUserById(userId);
            var body = userResponse.getBody();

            if (body == null || !body.isSuccess()) {
                throw new RuntimeException("User not found");
            }

            UserResponse user = body.getData();

            // Create new tokens
            var userInfo = new UserResponse(
                userId,
                username,
                email,
                roleStr
            );

            String newAccessToken = jwtTokenUtils.generateToken(userInfo);
            String newRefreshToken = jwtTokenUtils.generateRefreshToken(userInfo);

            // Store new token
            tokenService.addToken(userId, newAccessToken, false);

            return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .refreshExpiresIn(86400L)
                .user(user)
                .build();

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new Exception("Token refresh failed: " + e.getMessage());
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            return jwtTokenUtils.validateToken(token);
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
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
