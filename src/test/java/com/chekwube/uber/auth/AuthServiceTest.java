package com.chekwube.uber.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
 @Test void defaultsNewAccountsToRider() {
  UserRepository users = mock(UserRepository.class);
  when(users.existsByEmail("rider@example.com")).thenReturn(false);
  when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
  JwtService jwt = mock(JwtService.class);
  when(jwt.issue(any(AppUser.class))).thenReturn("test-token");
  AuthService service = new AuthService(users, new BCryptPasswordEncoder(4), jwt);
  AuthDtos.AuthResponse response = service.register(new AuthDtos.RegisterRequest("Rider@Example.com", "a-long-password-for-test", null));
  assertEquals("rider@example.com", response.email());
  assertEquals(UserRole.RIDER, response.role());
  assertEquals("Bearer", response.tokenType());
 }
 @Test void preventsSelfRegistrationAsAdmin() {
  UserRepository users = mock(UserRepository.class);
  when(users.existsByEmail("admin@example.com")).thenReturn(false);
  AuthService service = new AuthService(users, new BCryptPasswordEncoder(4), mock(JwtService.class));
  assertThrows(ResponseStatusException.class, () -> service.register(new AuthDtos.RegisterRequest("admin@example.com", "a-long-password-for-test", UserRole.ADMIN)));
  verify(users, never()).save(any());
 }
 @Test void rejectsIncorrectPassword() {
  UserRepository users = mock(UserRepository.class);
  AppUser user = new AppUser("rider@example.com", new BCryptPasswordEncoder(4).encode("correct-password"), UserRole.RIDER);
  when(users.findByEmail("rider@example.com")).thenReturn(Optional.of(user));
  AuthService service = new AuthService(users, new BCryptPasswordEncoder(4), mock(JwtService.class));
  assertThrows(ResponseStatusException.class, () -> service.login(new AuthDtos.LoginRequest("rider@example.com", "incorrect-password")));
 }
}
