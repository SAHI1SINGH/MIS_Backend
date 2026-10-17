package com.tadobasolutions.config;

import com.tadobasolutions.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Public (no JWT needed) ────────────────────────────────────────
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/v1/quotes/get-visit-count").permitAll()
                        .requestMatchers("/api/v1/quotes/update-visit-count").permitAll()

                        // ── Employee self-access ──────────────────────────────────────────
                        .requestMatchers("/api/employees/me").hasAnyRole("ADMIN", "SUB_ADMIN", "EMPLOYEE")

                        // ── Report endpoints ──────────────────────────────────────────────
                        .requestMatchers(
                                "/api/report/employee-summary",
                                "/api/report/employee-excel"
                        ).hasAnyRole("ADMIN", "SUB_ADMIN", "EMPLOYEE", "INTERN")

                        .requestMatchers(
                                "/api/report/team-summary",
                                "/api/report/team-excel"
                        ).hasRole("ADMIN")

                        // ── Admin-only ────────────────────────────────────────────────────
                        .requestMatchers("/api/admins/**").permitAll()
                        .requestMatchers("/api/employees/**").hasAnyRole("ADMIN", "SUB_ADMIN")

                        // ── Intern self-access + management ───────────────────────────────
                        .requestMatchers("/api/interns/me").hasAnyRole("ADMIN", "SUB_ADMIN", "INTERN")
                        .requestMatchers("/api/interns/**").hasAnyRole("ADMIN", "SUB_ADMIN")

                        // ── Tasks ─────────────────────────────────────────────────────────
                        .requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "SUB_ADMIN", "EMPLOYEE", "INTERN")

                        // ── Shared read-only content ──────────────────────────────────────
                        .requestMatchers("/api/notices/**").hasAnyRole("ADMIN", "SUB_ADMIN", "INTERN","EMPLOYEE")
                        .requestMatchers("/api/highlights/**").hasAnyRole("ADMIN", "SUB_ADMIN", "INTERN", "EMPLOYEE")
                        .requestMatchers("/api/quotes/**").hasAnyRole("ADMIN", "SUB_ADMIN", "INTERN", "EMPLOYEE")
                        .requestMatchers("/api/leaves/**").hasAnyRole("EMPLOYEE", "SUB_ADMIN", "INTERN", "ADMIN")
                        .requestMatchers(
                                "/api/standup/admin",
                                "/api/standup/admin/**",
                                "/api/standup/date/**"
                        ).hasRole("ADMIN")
                        // Incharge (employee side): submit, today, history, view one report
                        .requestMatchers("/api/standup/**").hasAnyRole("ADMIN", "SUB_ADMIN", "EMPLOYEE", "INTERN")

                        .requestMatchers("/api/attendance/**").hasAnyRole("ADMIN", "SUB_ADMIN", "EMPLOYEE", "INTERN")

                        // ── Store (component inventory) ──────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/departments/*").hasAnyRole("ADMIN", "SUB_ADMIN", "EMPLOYEE")
                        .requestMatchers("/api/departments/**").hasRole("ADMIN")

                        // ── FCM ───────────────────────────────────────────────────────────
                        .requestMatchers("/api/fcm/**").hasAnyRole("ADMIN", "SUB_ADMIN", "EMPLOYEE", "INTERN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",
                "http://localhost:*",
                "https://mis.tadobasolutions.com",
                "https://tadobasolutions.com",
                "*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}