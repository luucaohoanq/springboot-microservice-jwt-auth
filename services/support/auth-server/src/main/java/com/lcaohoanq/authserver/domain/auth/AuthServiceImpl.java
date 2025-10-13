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
import com.lcaohoanq.commonlibrary.dto.UpdateUserRequest;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import com.lcaohoanq.commonlibrary.enums.Role;
import com.lcaohoanq.commonlibrary.exceptions.AccountNotActivatedException;
import com.lcaohoanq.commonlibrary.exceptions.AccountResourceException;
import com.lcaohoanq.commonlibrary.exceptions.ConflictException;
import feign.Feign;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final LoginHistoryService loginHistoryService;

    @Value("${jwt.expiration}")
    private long expiration; // in seconds

    @Value("${jwt.expiration-refresh-token}")
    private long expirationRefreshToken; // in seconds

    @Override
    public LoginResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        log.info("🔐 Starting login process for user: {}", loginRequest.getUsername());

        // 1️⃣ Create authentication request
        var authRequest = new com.lcaohoanq.commonlibrary.dto.AuthenticationRequest(
            loginRequest.getUsername(),
            loginRequest.getPassword()
        );
        log.info("🔐 Created authentication request for user: {}", authRequest.getUsername());

        // 2️⃣ Call user-service authentication endpoint to validate credentials
        log.info("🔐 Calling user-service authenticate endpoint for user: {}", authRequest.getUsername());

        String clientIp = extractClientIp(request);

        UserResponse user;
        try {
            var authResponse = userFeign.authenticateUser(authRequest);
            // 3️⃣ Check if authentication was successful
            if(authResponse.getBody() == null || !authResponse.getBody().isSuccess()){
                throw new BadCredentialsException("Invalid credentials");
            }
            user = authResponse.getBody().getData();
            log.info("✅ User {} authenticated successfully", user.username());
            loginHistoryService.recordLogin(
                user.id(),
                clientIp,
                request.getHeader("User-Agent"),
                true
            );
        } catch(FeignException e){
            log.error("❌ Error during authentication call to user-service: {}", e.getMessage());
            throw new BadCredentialsException("Invalid credentials");
        }

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
            user.langKey(),
            user.lastLoginAttempt()
        );

        // 6️⃣ Generate access token and refresh token
        String accessToken = jwtTokenUtils.generateToken(userInfo);
        String refreshToken = jwtTokenUtils.generateRefreshToken(userInfo);

        // 7️⃣ Store token in database
        tokenService.addToken(user.id(), accessToken, isMobileDevice(request.getHeader("User-Agent")));


        var response = userFeign.updateUserInternal(userInfo.id(),
                                                    new UpdateUserRequest(null,
                                                                                     null,
                                                                                     null,
                                                                                     ZonedDateTime.now()));

        if(!response.getStatusCode().is2xxSuccessful()){
            log.error("❌ Failed to update lastLoginAttempt for user {}", user.username());
            throw new RuntimeException("Failed to update lastLoginAttempt");
        }

        if(userInfo.lastLoginAttempt() == null){
            log.info("✉️ First login detected for user {}. Sending welcome email...", user.username());
            mailService.sendWelcomeMail(user);
        }

        // 8️⃣ Return LoginResponse
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(expiration) // 1 hour
            .refreshExpiresIn(expirationRefreshToken) // 24 hours
            .user(user)
            .build();
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
        // 1️⃣ Check if username already exists
        try {
            var userByUsernameResponse = userFeign.getUserByUsername(registerRequest.getUsername());
            if (userByUsernameResponse.getBody() != null && userByUsernameResponse.getBody().isSuccess()) {
                throw new ConflictException("Username already exists");
            }
        } catch (com.lcaohoanq.commonlibrary.exceptions.NotFoundException e) {
            // Expected - username doesn't exist, which is good for registration
            log.debug("Username {} is available", registerRequest.getUsername());
        }

        // 2️⃣ Check if email already exists
        try {
            var userByEmailResponse = userFeign.getUserByEmail(registerRequest.getEmail());
            if (userByEmailResponse.getBody() != null && userByEmailResponse.getBody().isSuccess()) {
                throw new ConflictException("Email already exists");
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
                user.langKey(),
                user.lastLoginAttempt()
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
        if (email == null || email.isBlank()) {
            throw new AccountResourceException("Email must not be blank");
        }

        var userResponse = userFeign.getUserByEmail(email);
        if (userResponse.getBody() == null || !userResponse.getBody().isSuccess()) {
            throw new AccountResourceException("Email address not found");
        }

        try {
            var resetResponse = userFeign.requestPasswordReset(email);
            if (!resetResponse.getStatusCode().is2xxSuccessful()) {
                throw new AccountResourceException("Error requesting password reset");
            }

            var updatedUser = userFeign.getUserByEmail(email).getBody().getData();
            mailService.sendPasswordResetMail(updatedUser);

        } catch (FeignException e) {
            log.error("❌ Feign error during password reset for {}: {}", email, e.getMessage());
            throw new AccountResourceException("Failed to request password reset " + e.getMessage());
        }
    }

    @Override
    public void verifyResetKey(String key) {
        var data = userFeign.verifyResetKey(key);
        if(!data.getStatusCode().is2xxSuccessful()){
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

    public String extractClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isEmpty()) {
            // If there are multiple IPs, the first one is the client IP
            return header.split(",")[0];
        }
        return request.getRemoteAddr();
    }

}
