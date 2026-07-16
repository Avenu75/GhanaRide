package com.ghanaride.init;

import com.ghanaride.entity.*;
import com.ghanaride.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {

            // ===== ADMIN USER ONLY =====
            String adminPassword = System.getenv("ADMIN_PASSWORD");

            if (adminPassword == null || adminPassword.isEmpty()) {
                System.err.println("WARNING: ADMIN_PASSWORD environment variable not set!");
                adminPassword = "ChangeMe@2025!"; // fallback only
            }

            User admin = new User();
            admin.setUsername("admin");
            admin.setFullName("Administrator");
            admin.setEmail("admin@ghanaride.com");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setPhoneNumber("0200000000");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            System.out.println("Admin user initialized successfully!");
            System.out.println("Login with username: admin");
        }
    }
}