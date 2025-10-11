package com.lcaohoanq.authserver.domain.auth;

import com.lcaohoanq.commonlibrary.dto.UserResponse;

public interface UserServiceClient {
    UserResponse getUserByUsername(String username);
    UserResponse getUserByEmail(String email);
    UserResponse getUserById(Long id);
}
