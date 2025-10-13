package com.lcaohoanq.commonlibrary.dto;

import java.time.ZonedDateTime;

public record UpdateUserRequest(
    String username,
    String email,
    String langKey,
    ZonedDateTime lastLoginAttempt
) {

}
