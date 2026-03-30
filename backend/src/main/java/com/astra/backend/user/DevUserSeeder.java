package com.astra.backend.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevUserSeeder {

    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder passwordEncoder) {
        return args -> {
            String email = "admin@local.dev";
            String rawPassword = "admin1234";

            if (users.existsByEmail(email)) return;

            UserEntity u = new UserEntity();
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setRole(UserRole.ADMIN);
            users.save(u);
        };
    }
}

