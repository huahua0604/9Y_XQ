package com.hospital.req.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import com.hospital.req.entity.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank String employeeId,
        @NotBlank String name,
        @NotBlank String department,
        @NotNull  Boolean mustChangePassword,   
        @NotNull  Set<Role> roles,     
        @Size(max=32) String phone        
) {}
