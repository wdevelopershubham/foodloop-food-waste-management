package com.foodloop.foodloop_backend.common;

import com.foodloop.foodloop_backend.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${app.admin.email}") String email;
    @Value("${app.admin.password}") String password;

    public DataSeeder(UserRepository users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }

    @Override public void run(String... args) {
        if (!users.existsByEmail(email)) {
            users.save(User.builder().name("Administrator").email(email)
                    .passwordHash(encoder.encode(password)).role(Role.ADMIN).build());
        }
    }
}
