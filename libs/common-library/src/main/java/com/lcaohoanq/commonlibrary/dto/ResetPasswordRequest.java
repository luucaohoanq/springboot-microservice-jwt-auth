package com.lcaohoanq.commonlibrary.dto;

public record ResetPasswordRequest(
    String email,
    String key,
    String newPassword,
    String confirmNewPassword
){

}
