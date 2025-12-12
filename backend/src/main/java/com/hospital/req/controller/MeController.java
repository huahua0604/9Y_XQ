package com.hospital.req.controller;

import com.hospital.req.dto.*;
import com.hospital.req.entity.User;
import com.hospital.req.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public MyProfileDto me(Authentication auth) {
        User u = userService.getByEmployeeId(auth.getName());
        return MyProfileDto.from(u);
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MyProfileDto> updateMe(@Valid @RequestBody UpdateSelfRequest req,
                                                 Authentication auth) {
        var result = userService.updateSelf(auth.getName(), req);
        return ResponseEntity.ok()
                .header("X-Reauth", result.employeeIdChanged() ? "true" : "false")
                .body(MyProfileDto.from(result.user()));
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePwd(@Valid @RequestBody ChangePasswordRequest req,
                                          Authentication auth) {
        userService.changePassword(auth.getName(), req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
