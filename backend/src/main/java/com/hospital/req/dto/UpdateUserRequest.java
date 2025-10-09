package com.hospital.req.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import com.hospital.req.entity.Role;

public record UpdateUserRequest(
        @NotBlank String employeeId,
        @NotBlank String name,
        @NotBlank String department,
        @NotNull  Boolean mustChangePassword,   
        @NotNull  Set<Role> roles              
) {}
