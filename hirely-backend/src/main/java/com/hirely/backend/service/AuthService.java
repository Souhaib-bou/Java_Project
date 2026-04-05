package com.hirely.backend.service;

import com.hirely.backend.model.User;
import com.hirely.backend.repo.UserRepo;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepo userRepo;

    public AuthService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public User validateLogin(String email, String password) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // plaintext check (for now)
        if (user.getPassword() == null || !user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid credentials");
        }

        return user;
    }
}