package com.hirely.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p)) {
            throw new RuntimeException("Unauthenticated");
        }

        AppRole role = switch (p.getRoleId()) {
            case 3 -> AppRole.ADMIN;
            case 2 -> AppRole.RECRUITER;
            default -> AppRole.CANDIDATE;
        };

        return new CurrentUser(p.getUserId(), role);
    }
}