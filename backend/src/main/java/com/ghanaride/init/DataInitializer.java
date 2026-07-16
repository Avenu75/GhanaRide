package com.ghanaride.init;

import com.ghanaride.entity.*;
import com.ghanaride.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // ===== ADMIN USER INITIALIZATION / SYNC =====
        String adminPassword = System.getenv("ADMIN_PASSWORD");

        if (adminPassword == null || adminPassword.isEmpty()) {
            System.err.println("WARNING: ADMIN_PASSWORD environment variable not set!");
            adminPassword = "ChangeMe@2025!"; // fallback only
        }

        Optional<User> existingAdmin = userRepository.findByUsername("admin");
        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setEmail("admin@ghanaride.com"); // ensure email is set
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            admin.setEmailVerified(true);
            userRepository.save(admin);
            System.out.println("Admin password updated/synchronized successfully!");
        } else {
            User admin = new User();
            admin.setUsername("admin");
            admin.setFullName("Administrator");
            admin.setEmail("admin@ghanaride.com");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setPhoneNumber("0200000000");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            admin.setEmailVerified(true);
            userRepository.save(admin);
            System.out.println("Admin user initialized successfully!");
            System.out.println("Login with username: admin");
        }
    }
}