package com.smartattendance.controller;

import com.smartattendance.dto.AuthRequest;
import com.smartattendance.dto.AuthResponse;
import com.smartattendance.dto.RegisterRequest;
import com.smartattendance.model.User;
import com.smartattendance.model.UserRole;
import com.smartattendance.security.CustomUserDetails;
import com.smartattendance.security.JwtTokenProvider;
import com.smartattendance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            String token = jwtTokenProvider.generateToken(auth.getName());
            User user = userService.findByUsername(auth.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found after authentication"));
            return ResponseEntity.ok(new AuthResponse(
                    token,
                    user.getUsername(),
                    user.getFullName(),
                    user.getRole().name()
            ));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid role, allowed: ROLE_STUDENT or ROLE_TEACHER");
        }

        try {
            userService.registerNewUser(request.getUsername(), request.getPassword(), request.getFullName(), role);
            return ResponseEntity.ok("User registered");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(new AuthResponse(
                null,
                user.getUsername(),
                user.getFullName(),
                user.getRole().name()
        ));
    }
}
