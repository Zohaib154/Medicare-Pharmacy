package com.medicare.controller;

import com.medicare.dto.UserDTO;
import com.medicare.entity.User;
import com.medicare.entity.User.Role;
import com.medicare.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Manage pharmacists and staff")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all staff members")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(
            userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList())
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a new staff member")
    public ResponseEntity<?> createUser(@RequestBody UserDTO dto) {
        if (!StringUtils.hasText(dto.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }

        String email = normalizeEmail(dto.getEmail());
        if (email != null && userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        }

        String rawPassword = resolvePassword(dto);
        if (!StringUtils.hasText(rawPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
        }

        Set<Role> roles = parseRoles(dto.getRoles());
        if (roles.isEmpty()) {
            roles = Set.of(Role.ROLE_PHARMACIST);
        }

        if (!StringUtils.hasText(dto.getFullName())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Full name is required"));
        }

        User user = User.builder()
            .username(dto.getUsername().trim())
            .passwordHash(passwordEncoder.encode(rawPassword))
            .fullName(dto.getFullName())
            .email(email)
            .contactNumber(blankToNull(dto.getContactNumber()))
            .licenseNumber(blankToNull(dto.getLicenseNumber()))
            .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
            .roles(roles)
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(userRepository.save(user)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update staff member details")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (StringUtils.hasText(dto.getFullName())) {
            user.setFullName(dto.getFullName());
        }

        String email = normalizeEmail(dto.getEmail());
        if (email != null && !email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        }
        user.setEmail(email);

        if (dto.getContactNumber() != null) {
            user.setContactNumber(blankToNull(dto.getContactNumber()));
        }
        if (dto.getIsActive() != null) {
            if (Boolean.FALSE.equals(dto.getIsActive())
                && Boolean.TRUE.equals(user.getIsActive())
                && user.getRoles().contains(Role.ROLE_ADMIN)
                && userRepository.countActiveUsersWithRole(Role.ROLE_ADMIN) <= 1) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cannot deactivate the last active administrator"));
            }
            user.setIsActive(dto.getIsActive());
            if (Boolean.FALSE.equals(dto.getIsActive())) {
                user.setRefreshToken(null);
            }
        }
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            user.setRoles(parseRoles(dto.getRoles()));
        }

        String rawPassword = resolvePassword(dto);
        if (StringUtils.hasText(rawPassword)) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        return ResponseEntity.ok(toDto(userRepository.save(user)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a staff member")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        String currentUsername = authentication.getName();
        return userRepository.findById(id).map(user -> {
            if (user.getUsername().equals(currentUsername)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete your own account"));
            }
            if (Boolean.TRUE.equals(user.getIsActive())
                && user.getRoles().contains(Role.ROLE_ADMIN)
                && userRepository.countActiveUsersWithRole(Role.ROLE_ADMIN) <= 1) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cannot remove the last active administrator"));
            }
            user.setIsActive(false);
            user.setRefreshToken(null);
            userRepository.save(user);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private UserDTO toDto(User user) {
        return UserDTO.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .contactNumber(user.getContactNumber())
            .licenseNumber(user.getLicenseNumber())
            .roles(user.getRoles().stream().map(Role::name).collect(Collectors.toSet()))
            .isActive(user.getIsActive())
            .build();
    }

    private Set<Role> parseRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }
        return roleNames.stream()
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(Role::valueOf)
            .collect(Collectors.toSet());
    }

    private String resolvePassword(UserDTO dto) {
        if (StringUtils.hasText(dto.getPassword())) {
            return dto.getPassword();
        }
        return dto.getPasswordHash();
    }

    private String normalizeEmail(String email) {
        return blankToNull(email);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
