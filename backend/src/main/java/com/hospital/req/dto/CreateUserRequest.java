package com.hospital.req.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class CreateUserRequest {
    @NotBlank(message = "工号不能为空")
    private String employeeId;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String department;

    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    private String password;

    private Set<String> roles;

    private Boolean mustChangePassword;

    @Size(max=32) 
    String phone;
}