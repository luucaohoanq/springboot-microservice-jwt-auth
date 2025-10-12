package com.lcaohoanq.userservice;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);

    Optional<User> findOneByActivationKey(String activationKey);

    Optional<User> findOneByActivationKeyAndActivatedIsFalse(String activationKey);

}
