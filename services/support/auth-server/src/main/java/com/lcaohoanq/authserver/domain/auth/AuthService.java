package com.lcaohoanq.authserver.domain.auth;

import com.lcaohoanq.commonlibrary.dto.LoginRequest;
import com.lcaohoanq.commonlibrary.dto.LoginResponse;
import com.lcaohoanq.commonlibrary.dto.RegisterRequest;
import com.lcaohoanq.commonlibrary.dto.RefreshTokenRequest;
import com.lcaohoanq.commonlibrary.dto.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface AuthService {

    LoginResponse login(LoginRequest loginRequest, HttpServletRequest request);
    void logout(HttpServletRequest request);
    void register(RegisterRequest registerRequest);
    LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest) throws Exception;
    boolean validateToken(String token);
    void activateRegistration(String key);
    //Reset Password
    void requestPasswordReset(String email);
    void verifyResetKey(String key);
    void finishPasswordReset(ResetPasswordRequest request);

}
