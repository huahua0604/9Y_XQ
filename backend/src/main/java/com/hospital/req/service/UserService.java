package com.hospital.req.service;

import com.hospital.req.dto.CreateUserRequest;
import com.hospital.req.dto.LoginRequest;
import com.hospital.req.dto.LoginResponse;
import com.hospital.req.dto.UpdateUserRequest; 
import com.hospital.req.entity.Role;
import com.hospital.req.entity.User;
import com.hospital.req.exception.BadRequestException;
import com.hospital.req.repo.UserRepository;

import lombok.RequiredArgsConstructor;
import com.hospital.req.config.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public LoginResponse login(LoginRequest req) {
        try {
            var auth = new UsernamePasswordAuthenticationToken(req.getEmployeeId(), req.getPassword());
            authenticationManager.authenticate(auth);
        } catch (BadCredentialsException e) {
            throw new BadRequestException("工号或密码错误");
        }

        User u = userRepo.findByEmployeeId(req.getEmployeeId()).orElseThrow();
        UserDetails ud = userDetailsService.loadUserByUsername(u.getEmployeeId());
        String token = jwtService.generateToken(ud);
        return new LoginResponse(token, u.getName(), u.getRoles(), u.isMustChangePassword());
    }

    public void changePassword(String employeeId, String oldPassword, String newPassword) {
        User u = userRepo.findByEmployeeId(employeeId).orElseThrow();

        if (!encoder.matches(oldPassword, u.getPasswordHash())) {
            throw new BadRequestException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 64) {
            throw new BadRequestException("新密码长度需在 8-64 位之间");
        }

        u.setPasswordHash(encoder.encode(newPassword));
        u.setMustChangePassword(false);
        userRepo.save(u);
    }

    public boolean hasReviewer(String employeeId) {
        User u = userRepo.findByEmployeeId(employeeId).orElseThrow();
        return u.getRoles().contains(Role.REVIEWER) || u.getRoles().contains(Role.ADMIN);
    }

    // ================  管理员：新增用户 / 重置密码  ==========================

    public CreatedUserResult createUser(CreateUserRequest req) {
        userRepo.findByEmployeeId(req.getEmployeeId()).ifPresent(u -> {
            throw new BadRequestException("工号已存在");
        });

        String rawPwd = (req.getPassword() == null || req.getPassword().isBlank())
                ? (req.getEmployeeId()+req.getEmployeeId())
                : req.getPassword();

        if (rawPwd.length() < 8 || rawPwd.length() > 64) {
            throw new BadRequestException("密码长度需在 8-64 位之间");
        }

        Set<Role> roles = (req.getRoles() == null || req.getRoles().isEmpty())
                ? Set.of(Role.USER)
                : req.getRoles().stream().map(String::toUpperCase).map(roleName -> {
                    try { return Role.valueOf(roleName); }
                    catch (IllegalArgumentException e) {
                        throw new BadRequestException("非法角色：" + roleName);
                    }
                }).collect(Collectors.toSet());

        boolean usingDefault = (req.getPassword() == null || req.getPassword().isBlank());
        boolean mustChange = usingDefault ? true : (req.getMustChangePassword() == null ? true : req.getMustChangePassword());

        User u = new User();
        u.setEmployeeId(req.getEmployeeId());
        u.setName(req.getName());
        u.setDepartment(req.getDepartment());
        u.setRoles(roles);
        u.setMustChangePassword(mustChange);
        u.setPasswordHash(encoder.encode(rawPwd));
        userRepo.save(u);

        return new CreatedUserResult(u.getId(), usingDefault ? (req.getEmployeeId()+req.getEmployeeId()) : null, usingDefault);
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest req) {
        User u = userRepo.findById(id).orElseThrow(() -> new BadRequestException("用户不存在"));
        u.setEmployeeId(req.employeeId());
        u.setName(req.name());
        u.setDepartment(req.department());
        u.setMustChangePassword(Boolean.TRUE.equals(req.mustChangePassword()));
        u.setRoles(req.roles());
        return userRepo.save(u);
    }

    @Transactional
    public void deleteUser(Long id) {
        User target = userRepo.findById(id).orElseThrow(() -> new BadRequestException("用户不存在"));
        userRepo.delete(target);
    }

    public String resetPasswordToDefault(Long userId) {
    User u = userRepo.findById(userId).orElseThrow(() -> new BadRequestException("用户不存在"));

    String emp = u.getEmployeeId();
    if (emp == null || emp.isBlank()) {
        throw new BadRequestException("该用户工号为空，无法生成默认密码");
    }

    String raw = emp + emp;

    if (raw.length() < 8 || raw.length() > 64) {
        throw new BadRequestException("默认密码长度不满足 8-64 位，请手动指定密码");
    }

    u.setPasswordHash(encoder.encode(raw));
    u.setMustChangePassword(true);
    userRepo.save(u);
    return raw;
    }
    public record CreatedUserResult(Long id, String plainPassword, boolean usingDefault) {}
}
