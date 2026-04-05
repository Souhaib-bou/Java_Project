package com.hirely.backend.security;

import io.jsonwebtoken.Claims;
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

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);
        try {
            Claims claims = jwtService.parse(token);

            Integer userId = Integer.valueOf(claims.getSubject());
            Integer roleId = claims.get("roleId", Integer.class);

            // Map roleId -> authority string
            String roleName = switch (roleId) {
                case 3 -> "ROLE_ADMIN";
                case 2 -> "ROLE_RECRUITER";
                default -> "ROLE_CANDIDATE";
            };

            AuthPrincipal principal = new AuthPrincipal(userId, roleId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(roleName))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            // invalid token: ignore and continue (will be blocked by security rules)
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}