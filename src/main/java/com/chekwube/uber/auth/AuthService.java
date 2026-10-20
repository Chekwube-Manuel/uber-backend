package com.chekwube.uber.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) { this.users = users; this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        UserRole role = request.role() == null ? UserRole.RIDER : request.role();
        if (role == UserRole.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot self-register as admin");
        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()), role));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        AppUser user = users.findByEmail(normalize(request.email())).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        return response(user);
    }
    private AuthDtos.AuthResponse response(AppUser user) { return new AuthDtos.AuthResponse(jwtService.issue(user), "Bearer", user.getId(), user.getEmail(), user.getRole()); }
    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
