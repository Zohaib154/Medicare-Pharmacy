package com.medicare.config;

import com.medicare.entity.User;
import com.medicare.entity.User.Role;
import com.medicare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create default admin user if not present
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .email("admin@rxpro.com")
                .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_PHARMACIST, Role.ROLE_STORE_MANAGER))
                .build();
            userRepository.save(admin);
            log.info("Default admin user created: username=admin | password=admin123");
        }

        if (!userRepository.existsByUsername("pharmacist")) {
            User pharmacist = User.builder()
                .username("pharmacist")
                .passwordHash(passwordEncoder.encode("pharma123"))
                .fullName("Dr. Ahmed Hassan")
                .email("ahmed@rxpro.com")
                .licenseNumber("PH-2024-00142")
                .roles(Set.of(Role.ROLE_PHARMACIST))
                .build();
            userRepository.save(pharmacist);
            log.info("Default pharmacist user created: username=pharmacist | password=pharma123");
        }

        if (!userRepository.existsByUsername("cashier")) {
            User cashier = User.builder()
                .username("cashier")
                .passwordHash(passwordEncoder.encode("cash123"))
                .fullName("Ali Cashier")
                .email("cashier@rxpro.com")
                .roles(Set.of(Role.ROLE_CASHIER))
                .build();
            userRepository.save(cashier);
            log.info("Default cashier user created: username=cashier | password=cash123");
        }

        log.info("============================================");
        log.info("MediCare is ready!");
        log.info("Swagger UI: http://localhost:8080/api/swagger-ui.html");
        log.info("============================================");
    }
}
