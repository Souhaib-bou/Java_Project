package com.hirely.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ===== PUBLIC =====
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ping").permitAll()

                        // QR scan page (phone opens this)
                        .requestMatchers("/plan-access/**").permitAll()

                        // Optional: if you kept the JSON token endpoint
                        .requestMatchers("/api/plans/access/**").permitAll()

                        // ===== ADMIN ONLY =====
                        .requestMatchers("/api/roles/**").hasRole("ADMIN")

                        // ===== CANDIDATE/RECRUITER/ADMIN =====
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/plans/**")
                        .hasAnyRole("ADMIN", "RECRUITER", "CANDIDATE")

                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/plans/*/status")
                        .hasAnyRole("ADMIN", "RECRUITER", "CANDIDATE")

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/plans/*/tasks")
                        .hasAnyRole("ADMIN", "RECRUITER", "CANDIDATE")

                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/tasks/*/candidate")
                        .hasRole("CANDIDATE")

                        // ===== ADMIN/RECRUITER CRUD =====
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/plans/**")
                        .hasAnyRole("ADMIN", "RECRUITER")

                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/plans/**")
                        .hasAnyRole("ADMIN", "RECRUITER")

                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/plans/**")
                        .hasAnyRole("ADMIN", "RECRUITER")

                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/plans/*/tasks")
                        .hasAnyRole("ADMIN", "RECRUITER")

                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/tasks/**")
                        .hasAnyRole("ADMIN", "RECRUITER")

                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/tasks/**")
                        .hasAnyRole("ADMIN", "RECRUITER")

                        // everything else
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}