package com.lcaohoanq.authserver.domain.auth;

import com.lcaohoanq.authserver.feign.UserFeign;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import com.lcaohoanq.commonlibrary.utils.FeignResponseResolver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceClientImpl implements UserServiceClient {

    private final UserFeign userFeign;

    @Override
    public UserResponse getUserByUsername(String username) {
        return FeignResponseResolver.resolve(userFeign.getUserByUsername(username));
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return FeignResponseResolver.resolve(userFeign.getUserByEmail(email));
    }

    @Override
    public UserResponse getUserById(Long id) {
        return FeignResponseResolver.resolve(userFeign.getUserById(id));
    }
}

