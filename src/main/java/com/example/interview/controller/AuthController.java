package com.example.interview.controller;

import com.example.interview.model.AuthRequest;
import com.example.interview.model.JwtResponse;
import com.example.interview.model.User;
import com.example.interview.service.UserService;
import com.example.interview.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        userService.register(request.getUsername(), request.getPassword());
        return ResponseEntity.ok().body("注册成功");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!userService.checkPassword(user, request.getPassword())) {
            return ResponseEntity.status(401).body("密码错误");
        }
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("未登录");
        }
        String username = authentication.getName();
        return ResponseEntity.ok(Map.of("username", username));
    }
}
