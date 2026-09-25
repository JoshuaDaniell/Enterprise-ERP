package com.inventory.inventory_service.Config;

import com.inventory.inventory_service.Entity.Role;
import com.inventory.inventory_service.Entity.User;
import com.inventory.inventory_service.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUserIfNotFound("admin", "admin@enterprise-erp.com", "admin123", Role.ROLE_ADMIN);
        seedUserIfNotFound("manager", "manager@enterprise-erp.com", "manager123", Role.ROLE_WAREHOUSE_MANAGER);
        seedUserIfNotFound("sales", "sales@enterprise-erp.com", "sales123", Role.ROLE_SALES_USER);
        seedUserIfNotFound("finance", "finance@enterprise-erp.com", "finance123", Role.ROLE_FINANCE_USER);
        seedUserIfNotFound("viewer", "viewer@enterprise-erp.com", "viewer123", Role.ROLE_VIEWER);
    }

    private void seedUserIfNotFound(String username, String email, String password, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Initialized default {} user: username='{}', password='{}'", role, username, password);
        }
    }
}
