package com.lcaohoanq.authserver.domain.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Optional<LoginHistory> findTop1ByUserIdOrderByLoginAtDesc (Long userId);
    List<LoginHistory> findTop5ByUserIdOrderByLoginAtDesc (Long userId);
    Boolean existsByUserId(Long userId);
}
