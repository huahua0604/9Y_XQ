package com.hospital.req.dto;

import com.hospital.req.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String name;
    private Set<Role> roles;
    private boolean mustChangePassword;
}

