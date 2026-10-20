package com.chekwube.uber.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtService tokens;
 public JwtAuthenticationFilter(JwtService tokens) { this.tokens = tokens; }
 @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
  String header = request.getHeader("Authorization");
  if (header != null && header.startsWith("Bearer ")) {
   try {
    String token = header.substring(7);
    UUID id = UUID.fromString(tokens.subject(token));
    String role = tokens.role(token);
    var auth = new UsernamePasswordAuthenticationToken(id, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    SecurityContextHolder.getContext().setAuthentication(auth);
   } catch (Exception ex) { SecurityContextHolder.clearContext(); }
  }
  chain.doFilter(request, response);
 }
}
