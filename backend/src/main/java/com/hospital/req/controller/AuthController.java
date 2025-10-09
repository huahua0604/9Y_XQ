package com.hospital.req.controller;

import com.hospital.req.dto.ChangePasswordRequest;
import com.hospital.req.dto.LoginRequest;
import com.hospital.req.dto.LoginResponse;
import com.hospital.req.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest req, Authentication auth) {
        String empId = auth.getName();
        userService.changePassword(empId, req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok().build();
    }
}