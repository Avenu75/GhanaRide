package com.ghanaride.service;

import com.ghanaride.entity.Role;
import com.ghanaride.entity.Trip;
import com.ghanaride.entity.User;
import com.ghanaride.repository.BookingRepository;
import com.ghanaride.repository.CarRepository;
import com.ghanaride.repository.CompanyRepository;
import com.ghanaride.repository.TripRepository;
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
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final CarRepository carRepository;

    @Transactional
    public User registerUser(User user, String companyName, String companyEmail,
                             String companyPhone, String companyLocation,
                             String companyDescription, String registrationNumber) {

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Email verification is disabled — all users can log in immediately.
        user.setEmailVerified(true);

        String accountType = user.getAccountType();

        if ("company".equalsIgnoreCase(accountType)) {
            user.setRole(Role.COMPANY);
        } else if ("driver".equalsIgnoreCase(accountType)) {
            user.setRole(Role.DRIVER);
        } else if ("passenger".equalsIgnoreCase(accountType)) {
            user.setRole(Role.USER);
        } else {
            user.setRole(Role.USER);
        }

        User savedUser = userRepository.save(user);

        if ("company".equalsIgnoreCase(accountType)) {
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

    /**
     * Used by OAuth2 flow — registers a user who authenticated via Google.
     * Their email is already verified by Google, so we set emailVerified=true.
     */
    @Transactional
    public User registerOAuthUser(String email, String fullName, String googleId) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        // Use email prefix as username, ensure uniqueness
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix++;
        }
        user.setUsername(username);
        // Random secure password — OAuth users log in via Google, not password
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setEmailVerified(true); // Google already verified
        user.setRole(Role.USER);
        user.setAccountType("passenger");
        return userRepository.save(user);
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

        return userRepository.findByUsernameOrEmail(principal.getName(), principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    // ===== DELETE USER WITH ALL RELATED DATA =====
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // 1. Delete bookings made BY this user as a passenger
        bookingRepository.deleteByUserId(userId);

        // 2. Get all trips created by this user if driver
        List<Trip> userTrips = tripRepository.findByDriverId(userId);

        // 3. Delete bookings for each of those trips
        for (Trip trip : userTrips) {
            bookingRepository.deleteByTripId(trip.getId());
        }

        // 4. Delete trips created by this user
        tripRepository.deleteByDriverId(userId);

        // 5. Delete cars owned by this user
        carRepository.deleteByDriverId(userId);

        // 6. Finally delete the user
        userRepository.delete(user);
    }
}