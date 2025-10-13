package com.lcaohoanq.authserver.domain.location;

import com.lcaohoanq.authserver.domain.auth.LoginHistory;
import com.lcaohoanq.authserver.domain.auth.LoginHistoryService;
import com.lcaohoanq.authserver.utils.AuthUtils;
import com.lcaohoanq.commonlibrary.annotations.RequireRole;
import com.lcaohoanq.commonlibrary.apis.MyApiResponse;
import com.lcaohoanq.commonlibrary.enums.Role;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/login-history")
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    public LoginHistoryController(LoginHistoryService loginHistoryService) {
        this.loginHistoryService = loginHistoryService;
    }

    @GetMapping
    public ResponseEntity<MyApiResponse<List<LoginHistory>>> getAll() {
        var currentUser = AuthUtils.getCurrentUser();
        assert currentUser != null;
        if(!Objects.equals(currentUser.role(), Role.ADMIN.name())) {
            return MyApiResponse.forbidden("Access denied: ADMIN role required");
        }

        return MyApiResponse.success(loginHistoryService.getAll());
    }

}
