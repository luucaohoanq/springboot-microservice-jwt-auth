package com.lcaohoanq.commonlibrary.dto;

public record UserResponse(
    Long id,
    String username,
    String email,
    String role
) {

}
