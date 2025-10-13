package com.lcaohoanq.authserver.domain.auth;

import java.util.List;

public interface ILoginHistoryService {

    void recordLogin(Long userId, String ipAddress, String userAgent,
                       boolean success);
    List<LoginHistory> getAll();
    List<LoginHistory> getByUserId(Long userId);

}
