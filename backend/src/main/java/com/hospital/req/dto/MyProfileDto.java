package com.hospital.req.dto;

import com.hospital.req.entity.Role;
import com.hospital.req.entity.User;
import java.util.Set;

public record MyProfileDto(
        Long id,
        String employeeId,
        String name,
        String department,
        Set<Role> roles,
        boolean mustChangePassword,
        String phone
) {
    public static MyProfileDto from(User u) {
        return new MyProfileDto(
                u.getId(),
                u.getEmployeeId(),
                u.getName(),
                u.getDepartment(),
                u.getRoles(),
                u.isMustChangePassword(),
                u.getPhone()
        );
    }
}

