package com.medicare.config;

import com.medicare.security.JwtAuthFilter;
import com.medicare.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/auth/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health",
        "/",
        "/index.html",
        "/css/**",
        "/js/**",
        "/assets/**",
        "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                // User Management - only ADMIN
                .requestMatchers("/users/**").hasRole("ADMIN")
                // Backup Management - only ADMIN
                .requestMatchers("/backup/**").hasRole("ADMIN")
                // Dashboard - any authenticated user
                .requestMatchers(HttpMethod.GET, "/dashboard/**").authenticated()
                // Drugs - read for all, write for managers/admins
                .requestMatchers(HttpMethod.GET, "/drugs/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/drugs/**")
                    .hasAnyRole("ADMIN", "STORE_MANAGER", "PHARMACIST")
                .requestMatchers(HttpMethod.PUT, "/drugs/**")
                    .hasAnyRole("ADMIN", "STORE_MANAGER", "PHARMACIST")
                .requestMatchers(HttpMethod.DELETE, "/drugs/**")
                    .hasAnyRole("ADMIN", "STORE_MANAGER")
                // Inventory
                .requestMatchers("/inventory/**")
                    .hasAnyRole("ADMIN", "STORE_MANAGER", "PHARMACIST")
                // Patients
                .requestMatchers("/patients/**").authenticated()
                // Prescriptions
                .requestMatchers("/prescriptions/**")
                    .hasAnyRole("ADMIN", "PHARMACIST")
                // Sales
                .requestMatchers("/sales/**")
                    .hasAnyRole("ADMIN", "PHARMACIST", "CASHIER")
                // Suppliers and Purchase Orders
                .requestMatchers("/suppliers/**", "/purchase-orders/**")
                    .hasAnyRole("ADMIN", "STORE_MANAGER")
                // User management
                .requestMatchers("/users/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}


