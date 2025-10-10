package com.lcaohoanq.userservice.data;

import com.lcaohoanq.commonlibrary.enums.Role;
import com.lcaohoanq.userservice.User;
import com.lcaohoanq.userservice.UserRepository;
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
            adminUser.setRole(Role.ADMIN);
            userRepository.save(adminUser);

            User staffUser = new User();
            staffUser.setUsername("staff");
            staffUser.setEmail("staff@example.com");
            staffUser.setPassword(defaultPassword);
            staffUser.setRole(Role.STAFF);
            userRepository.save(staffUser);

            User regularUser = new User();
            regularUser.setUsername("user");
            regularUser.setEmail("user@example.com");
            regularUser.setPassword(defaultPassword);
            regularUser.setRole(Role.USER);
            userRepository.save(regularUser);

            System.out.println("Sample users created with default password 'password123':");
            System.out.println("- admin (ADMIN role)");
            System.out.println("- staff (STAFF role)");
            System.out.println("- user (USER role)");
        }
    }
}