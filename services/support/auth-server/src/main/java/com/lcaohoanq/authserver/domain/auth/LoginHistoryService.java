package com.lcaohoanq.authserver.domain.auth;

import com.lcaohoanq.authserver.domain.location.GeoLocationService;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryService implements ILoginHistoryService{

    private final LoginHistoryRepository loginHistoryRepository;
    private final GeoLocationService geoLocationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLogin(Long userId, String ipAddress, String userAgent,
                            boolean success) {

        var location = geoLocationService.getLocationFromIp(ipAddress);
        var loginEntry = LoginHistory.builder()
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .location(String.format("%s, %s", location.getCity(), location.getCountry()))
                .loginAt(ZonedDateTime.now())
                .build();

        loginHistoryRepository.save(loginEntry);
        log.info("User {} logged in from IP={}, UA={}, Location={}, {}",
                userId, ipAddress, userAgent, location.getCity(), location.getCountry());

    }

    @Override
    public List<LoginHistory> getAll() {
        return loginHistoryRepository.findAll();
    }

    @Override
    public List<LoginHistory> getByUserId(Long userId) {
        return loginHistoryRepository.findTop5ByUserIdOrderByLoginAtDesc(userId);
    }
}
