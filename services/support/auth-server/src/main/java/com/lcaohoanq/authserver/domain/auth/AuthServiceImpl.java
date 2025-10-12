package com.lcaohoanq.authserver.domain.auth;

import com.lcaohoanq.authserver.components.JwtTokenUtils;
import com.lcaohoanq.authserver.domain.mail.MailService;
import com.lcaohoanq.authserver.domain.token.TokenService;
import com.lcaohoanq.authserver.feign.UserFeign;
import com.lcaohoanq.commonlibrary.annotations.RequireRole;
import com.lcaohoanq.commonlibrary.dto.LoginRequest;
import com.lcaohoanq.commonlibrary.dto.LoginResponse;
import com.lcaohoanq.commonlibrary.dto.RegisterRequest;
import com.lcaohoanq.commonlibrary.dto.RefreshTokenRequest;
import com.lcaohoanq.commonlibrary.dto.ResetPasswordRequest;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import com.lcaohoanq.commonlibrary.enums.Role;
import com.lcaohoanq.commonlibrary.exceptions.AccountNotActivatedException;
import com.lcaohoanq.commonlibrary.exceptions.AccountResourceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final MailService mailService;

    @Value("${jwt.expiration}")
    private long expiration; // in seconds

    @Value("${jwt.expiration-refresh-token}")
    private long expirationRefreshToken; // in seconds

    @Override
    public LoginResponse login(LoginRequest loginRequest, HttpServletRequest request) {
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
                          body == null, body != null ? true : "N/A");
                throw new RuntimeException("Invalid credentials");
            }

            // 4️⃣ Get authenticated user data from response
            UserResponse user = body.getData();

            // 4️⃣ Handle unactivated user
            if (!user.activated() && user.activationKey() != null) {
                log.warn("⚠️ User {} is not activated. Sending activation email...", user.username());
                // Send activation email (async)
                mailService.sendActivationEmail(user);
                // Stop here — do NOT continue login
                throw new AccountNotActivatedException(
                    "Account not activated. Activation email has been sent to " + user.email()
                );
            }

            // 5️⃣ Create UserInfo for JWT generation
            var userInfo = new UserResponse(
                user.id(),
                user.username(),
                user.email(),
                false,
                user.role(),
                user.activationKey(),
                user.resetKey(),
                user.langKey()
            );

            // 6️⃣ Generate access token and refresh token
            String accessToken = jwtTokenUtils.generateToken(userInfo);
            String refreshToken = jwtTokenUtils.generateRefreshToken(userInfo);

            // 7️⃣ Store token in database
            tokenService.addToken(user.id(), accessToken, isMobileDevice(request.getHeader("User-Agent")));

            mailService.sendWelcomeMail(user);

            // 8️⃣ Return LoginResponse
            return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiration) // 1 hour
                .refreshExpiresIn(expirationRefreshToken) // 24 hours
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

            mailService.sendActivationEmail(createUserResponse.getBody().getData());
            
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
                false,
                roleStr,
                user.activationKey(),
                user.resetKey(),
                user.langKey()
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

    @Override
    public void activateRegistration(String key) {
        try{
            var data  = userFeign.activateUser(key);
            if(!data.getStatusCode().is2xxSuccessful()){
                throw new AccountResourceException("No user found for this activation key");
            }
        } catch (Exception e){
            log.error("Account activation failed", e);
            throw new AccountResourceException("No user found for this activation key");
        }
    }

    @Override
    public void requestPasswordReset(String email) {
        try {
            var response = userFeign.getUserByEmail(email);
            if (response.getBody() == null || !response.getBody().isSuccess()) {
                throw new RuntimeException("Email address not found");
            }
            var data = userFeign.requestPasswordReset(email);

            if(data.getStatusCode().is2xxSuccessful()){
                mailService.sendPasswordResetMail(response.getBody().getData());
            } else {
                throw new AccountResourceException("Error requesting password reset");
            }
        } catch (Exception e) {
            log.error("Password reset request failed for email: {}", email, e);
            throw new RuntimeException("Password reset request failed: " + e.getMessage());
        }
    }

    @Override
    public void verifyResetKey(String key) {
        try{
            var data = userFeign.verifyResetKey(key);

            if(!data.getStatusCode().is2xxSuccessful()){
                throw new AccountResourceException("Invalid or expired password reset key");
            }
        }catch(Exception e){

            log.error("Password reset key verification failed", e);
            throw new AccountResourceException("Invalid or expired password reset key");
        }
    }

    @Override
    public void finishPasswordReset(ResetPasswordRequest request) {
        try{

            String encodedPassword = passwordEncoder.encode(request.newPassword());

            var user = userFeign.getUserByEmail(request.email());

            if(user.getBody() == null || !user.getBody().isSuccess()){
                throw new AccountResourceException("No user found for this email");
            }

            var newRequest = new ResetPasswordRequest(
                request.email(),
                request.key(),
                encodedPassword,
                request.confirmNewPassword()
            );

            var data = userFeign.finishPasswordReset(newRequest);

            if(!data.getStatusCode().is2xxSuccessful()){
                throw new AccountResourceException("Error finishing password reset");
            }
        }catch(Exception e){
            log.error("Finishing password reset failed", e);
            throw new AccountResourceException("Error finishing password reset: " + e.getMessage());
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isMobileDevice(String userAgent) {
        // Kiểm tra User-Agent header để xác định thiết bị di động
        if (userAgent == null) {
            return false;
        }
        return userAgent.toLowerCase().contains("mobile");
    }

}
