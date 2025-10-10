package com.lcaohoanq.authserver.services;

import com.lcaohoanq.commonlibrary.dto.LoginRequest;
import com.lcaohoanq.commonlibrary.dto.LoginResponse;
import com.lcaohoanq.commonlibrary.dto.RegisterRequest;
import com.lcaohoanq.commonlibrary.dto.RefreshTokenRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResponse login(LoginRequest loginRequest);
    void logout(HttpServletRequest request);
    void register(RegisterRequest registerRequest);
    LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest) throws Exception;
    boolean validateToken(String token);

}
