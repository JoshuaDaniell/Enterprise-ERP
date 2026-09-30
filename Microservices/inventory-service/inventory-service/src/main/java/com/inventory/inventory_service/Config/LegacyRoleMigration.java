package com.inventory.inventory_service.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LegacyRoleMigration implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public LegacyRoleMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Integer legacyUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role IN " +
                        "('ROLE_ADMIN','ROLE_WAREHOUSE_MANAGER','ROLE_VIEWER')", Integer.class);
        if (legacyUsers == null || legacyUsers == 0) return;

        jdbcTemplate.execute("ALTER TABLE users MODIFY role VARCHAR(32) NOT NULL");
        jdbcTemplate.update("UPDATE users SET enabled = false, role = 'ROLE_INVENTORY_USER' WHERE role = 'ROLE_VIEWER'");
        jdbcTemplate.update("UPDATE users SET role = 'ROLE_INVENTORY_USER' WHERE role IN ('ROLE_ADMIN', 'ROLE_WAREHOUSE_MANAGER')");
        jdbcTemplate.execute("ALTER TABLE users MODIFY role ENUM('ROLE_INVENTORY_USER','ROLE_SALES_USER','ROLE_FINANCE_USER') NOT NULL");
    }
}
