package com.hirely.backend.web;

import com.hirely.backend.dto.LoginRequest;
import com.hirely.backend.dto.LoginResponse;
import com.hirely.backend.model.User;
import com.hirely.backend.security.JwtService;
import com.hirely.backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        if (req.getEmail() == null || req.getPassword() == null) {
            throw new RuntimeException("email and password are required");
        }

        User user = authService.validateLogin(req.getEmail(), req.getPassword());
        String token = jwtService.generateToken(user.getUserId(), user.getRoleId());

        return new LoginResponse(token, user.getUserId(), user.getRoleId());
    }
}