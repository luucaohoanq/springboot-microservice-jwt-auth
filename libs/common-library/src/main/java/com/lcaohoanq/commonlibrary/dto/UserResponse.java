package com.lcaohoanq.commonlibrary.dto;

public record UserResponse(
    Long id,
    String username,
    String email,
    boolean activated,
    String role,
    String activationKey,
    String resetKey,
    String langKey
) {

}
