package com.hospital.req.config;

import com.hospital.req.entity.Role;
import com.hospital.req.entity.User;
import com.hospital.req.repo.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (userRepo.count() == 0) {
            User admin = new User();
            admin.setEmployeeId("100001");
            admin.setName("管理员");
            admin.setDepartment("信息科");
            admin.setRoles(Set.of(Role.ADMIN, Role.REVIEWER));
            admin.setPasswordHash(encoder.encode("ChangeMe123!"));
            admin.setMustChangePassword(true);
            admin.setEnabled(true);
            userRepo.save(admin);

            User user = new User();
            user.setEmployeeId("100002");
            user.setName("普通员工");
            user.setDepartment("信息科");
            user.setRoles(Set.of(Role.USER));
            user.setPasswordHash(encoder.encode("ChangeMe123!"));
            user.setMustChangePassword(true);
            user.setEnabled(true);
            userRepo.save(user);
        }
    }
}
