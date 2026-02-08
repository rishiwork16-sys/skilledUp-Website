package com.skilledup.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@org.springframework.cloud.openfeign.EnableFeignClients
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    org.springframework.boot.CommandLineRunner initAdmin(
            com.skilledup.auth.repository.UserRepository userRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Fix schema issue (role column too short)
                jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(50)");
                // Fix password_hash column length for BCrypt
                try {
                    jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255)");
                    System.out.println("Schema patched: users.password_hash resized to VARCHAR(255)");
                } catch (Exception ex) {
                    // Try snake_case if camelCase failed or vice versa, but usually it's
                    // password_hash
                    // Ignoring specific error if column doesn't exist to avoid crash
                }

                System.out.println("Schema patched: users.role resized to VARCHAR(50)");
            } catch (Exception e) {
                System.out.println("Schema patch warning: " + e.getMessage());
            }

            try {
                com.skilledup.auth.model.User admin = userRepository.findByEmail("admin2@finallms.local")
                        .orElse(com.skilledup.auth.model.User.builder()
                                .name("Default Admin")
                                .email("admin2@finallms.local")
                                .mobile("0000000000") // Dummy
                                .role(com.skilledup.auth.model.Role.ROLE_ADMIN)
                                .active(true)
                                .emailVerified(true)
                                .mobileVerified(true)
                                .createdAt(java.time.Instant.now())
                                .build());

                // Always update password and role to ensure they match credentials
                System.out.println("RESETTING ADMIN PASSWORD FOR: admin2@finallms.local");
                String newHash = passwordEncoder.encode("Admin2@123");
                admin.setPasswordHash(newHash);
                admin.setRole(com.skilledup.auth.model.Role.ROLE_ADMIN);

                userRepository.save(admin);
                System.out.println(
                        "Admin initialized/updated: admin2@finallms.local with password Admin2@123 (Hash length: "
                                + newHash.length() + ")");
            } catch (Exception e) {
                System.err
                        .println("CRITICAL: Admin init failed, but continuing to allow fix-db endpoint to run. Error: "
                                + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}
