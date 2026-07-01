package com.medicare.controller;

import com.medicare.dto.*;
import com.medicare.entity.User;
import com.medicare.entity.User.Role;
import com.medicare.exception.DuplicateResourceException;
import com.medicare.repository.UserRepository;
import com.medicare.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, register, and token refresh")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        Set<String> roles = userDetails.getAuthorities().stream()
            .map(a -> a.getAuthority()).collect(Collectors.toSet());

        return ResponseEntity.ok(LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .userId(user.getUserId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .roles(roles)
            .build());
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new user (Admin only)")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        Set<Role> roles = (request.getRoles() == null || request.getRoles().isEmpty())
            ? Set.of(Role.ROLE_PHARMACIST)
            : request.getRoles().stream().map(Role::valueOf).collect(Collectors.toSet());

        User user = User.builder()
            .username(request.getUsername())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .email(request.getEmail())
            .contactNumber(request.getContactNumber())
            .licenseNumber(request.getLicenseNumber())
            .roles(roles)
            .build();

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully: " + request.getUsername());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<LoginResponse> refresh(@RequestParam String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }
        String username = jwtUtils.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username).orElseThrow();

        if (!refreshToken.equals(user.getRefreshToken())) {
            return ResponseEntity.status(401).build();
        }

        String newAccessToken = jwtUtils.generateAccessToken(username);
        return ResponseEntity.ok(LoginResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .userId(user.getUserId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate refresh token")
    public ResponseEntity<String> logout(@RequestParam String username) {
        userRepository.findByUsername(username).ifPresent(u -> {
            u.setRefreshToken(null);
            userRepository.save(u);
        });
        return ResponseEntity.ok("Logged out successfully");
    }
}


