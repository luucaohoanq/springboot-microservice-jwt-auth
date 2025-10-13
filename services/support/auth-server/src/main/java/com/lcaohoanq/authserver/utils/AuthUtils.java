package com.lcaohoanq.authserver.utils;

import com.lcaohoanq.authserver.filter.JwtAuthenticationFilter.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtils {

    public static UserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof UserDetails details) {
            return details;
        }
        return null;
    }

    public static Long getCurrentUserId() {
        var user = getCurrentUser();
        return user != null ? user.userId() : null;
    }

    public static String getCurrentUsername() {
        var user = getCurrentUser();
        return user != null ? user.username() : null;
    }
}
