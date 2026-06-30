package com.ghanaride.init;

import com.ghanaride.entity.*;
import com.ghanaride.repository.CarRepository;
import com.ghanaride.repository.TripRepository;
import com.ghanaride.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final TripRepository tripRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            // Admin
            User admin = new User();
            admin.setUsername("admin");
            admin.setFullName("Administrator");
            admin.setEmail("admin@ghanaride.com");
            admin.setPassword(passwordEncoder.encode(
                    System.getenv("ADMIN_PASSWORD")
            ));
            admin.setPhoneNumber("0200000000");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            // Driver
            User driver = new User();
            driver.setUsername("kwame");
            driver.setFullName("Kwame Mensah");
            driver.setEmail("kwame@ghanaride.com");
            driver.setPassword(passwordEncoder.encode("driver123"));
            driver.setPhoneNumber("0244123456");
            driver.setRole(Role.DRIVER);
            userRepository.save(driver);

            // User
            User user = new User();
            user.setUsername("ama");
            user.setFullName("Ama Serwaa");
            user.setEmail("ama@ghanaride.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setPhoneNumber("0551234567");
            user.setRole(Role.USER);
            userRepository.save(user);

            // Car for Kwame
            Car car = new Car();
            car.setDriver(driver);
            car.setCarBrand("Toyota Hiace");
            car.setNumberPlate("GR-1234-22");
            car.setStatus(CarStatus.APPROVED);
            carRepository.save(car);

            // Approved Trip
            Trip trip1 = new Trip();
            trip1.setCar(car);
            trip1.setDriver(driver);
            trip1.setFromLocation("Accra");
            trip1.setToLocation("Cape Coast");
            trip1.setDepartureTime(LocalDateTime.now().plusDays(1).withHour(6).withMinute(0));
            trip1.setTripAmount(new BigDecimal("35.00"));
            trip1.setAvailableSeats(14);
            trip1.setTotalSeats(14);
            trip1.setStatus(TripStatus.APPROVED);
            tripRepository.save(trip1);

            // Pending Trip
            Trip trip2 = new Trip();
            trip2.setCar(car);
            trip2.setDriver(driver);
            trip2.setFromLocation("Cape Coast");
            trip2.setToLocation("Kumasi");
            trip2.setDepartureTime(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0));
            trip2.setTripAmount(new BigDecimal("50.00"));
            trip2.setAvailableSeats(14);
            trip2.setTotalSeats(14);
            trip2.setStatus(TripStatus.PENDING);
            tripRepository.save(trip2);

            System.out.println("Sample data initialized successfully!");
        }
    }
}
