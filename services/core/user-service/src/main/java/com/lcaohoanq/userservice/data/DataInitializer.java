package com.lcaohoanq.userservice.data;

import com.lcaohoanq.commonlibrary.enums.LangKey;
import com.lcaohoanq.commonlibrary.enums.Role;
import com.lcaohoanq.userservice.User;
import com.lcaohoanq.userservice.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        if (userRepository.count() == 0) {
            // Create sample users for testing with default password "password123"
            String defaultPassword = passwordEncoder.encode("password123");
            
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@example.com");
            adminUser.setPassword(defaultPassword);
            adminUser.setActivationKey(UUID.randomUUID().toString());
            adminUser.setActivated(true);
            adminUser.setLangKey(LangKey.EN.getKey());
            adminUser.setRole(Role.ADMIN);
            userRepository.save(adminUser);

            User staffUser = new User();
            staffUser.setUsername("staff");
            staffUser.setEmail("staff@example.com");
            staffUser.setPassword(defaultPassword);
            staffUser.setActivationKey(UUID.randomUUID().toString());
            staffUser.setLangKey(LangKey.EN.getKey());
            staffUser.setActivated(true);
            staffUser.setRole(Role.STAFF);
            userRepository.save(staffUser);

            User regularUser = new User();
            regularUser.setUsername("hoang");
            regularUser.setEmail("hoangdz1604@gmail.com");
            regularUser.setPassword(defaultPassword);
            regularUser.setActivationKey(UUID.randomUUID().toString());
            regularUser.setLangKey(LangKey.EN.getKey());
            regularUser.setRole(Role.USER);
            userRepository.save(regularUser);

            System.out.println("Sample users created with default password 'password123':");
            System.out.println("- admin (ADMIN role)");
            System.out.println("- staff (STAFF role)");
            System.out.println("- user (USER role)");
        }
    }
}