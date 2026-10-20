package com.chekwube.uber.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
  return http.csrf(csrf -> csrf.disable()).httpBasic(basic -> basic.disable()).formLogin(form -> form.disable())
   .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/**", "/api/v1/health", "/actuator/health/**").permitAll()
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/v1/rider/**").hasRole("RIDER")
    .requestMatchers("/api/v1/driver/**").hasRole("DRIVER")
    .requestMatchers("/actuator/**").denyAll().anyRequest().authenticated())
   .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
 }
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
}