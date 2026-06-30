package com.ghanaride.service;

import com.ghanaride.entity.Role;
import com.ghanaride.entity.User;
import com.ghanaride.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final com.ghanaride.repository.CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User user, String companyName, String companyEmail, String companyPhone, String companyLocation, String companyDescription, String registrationNumber) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        if ("company".equals(user.getAccountType())) {
            user.setRole(Role.COMPANY);
        } else if ("driver".equals(user.getAccountType())) {
            user.setRole(Role.DRIVER);
        } else if ("passenger".equals(user.getAccountType())) {
            user.setRole(Role.USER);
        } else {
            user.setRole(Role.USER); // safe fallback
        }
        
        User savedUser = userRepository.save(user);
        
        if ("company".equals(user.getAccountType())) {
            com.ghanaride.entity.Company company = new com.ghanaride.entity.Company();
            company.setUser(savedUser);
            company.setCompanyName(companyName);
            company.setEmail(companyEmail);
            company.setPhone(companyPhone);
            company.setLocation(companyLocation);
            company.setDescription(companyDescription);
            company.setRegistrationNumber(registrationNumber);
            companyRepository.save(company);
        }
        
        return savedUser;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsernameOrEmail(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier, identifier);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<User> findUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("No authenticated user");
        }
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ===== DELETE USER =====
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        userRepository.delete(user);
    }

    // ===== FIND USER BY ID =====
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }
}