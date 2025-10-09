package com.hospital.req.controller;

import com.hospital.req.dto.CreateUserRequest;
import com.hospital.req.dto.UpdateUserRequest;
import com.hospital.req.entity.User;
import com.hospital.req.repo.UserRepository;
import com.hospital.req.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepo;

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest req) {
        var result = userService.createUser(req);
        return ResponseEntity.ok(Map.of(
                "id", result.id(),
                "tempPassword", result.usingDefault() ? result.plainPassword() : null
        ));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UserListItem> list() {
        return userRepo.findAll().stream().map(UserListItem::from).toList();
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> reset(@PathVariable Long id) {
        String newPwd = userService.resetPasswordToDefault(id);
        return ResponseEntity.ok(Map.of("tempPassword", newPwd));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UserListItem update(@PathVariable Long id, 
    @Valid @RequestBody UpdateUserRequest req) {
        var updated = userService.updateUser(id, req);
        return UserListItem.from(updated);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    public record UserListItem(
            Long id,
            String employeeId,
            String name,
            String department,
            boolean mustChangePassword,
            List<String> roles
    ) {
        public static UserListItem from(User u) {
            return new UserListItem(
                    u.getId(),
                    u.getEmployeeId(),
                    u.getName(),
                    u.getDepartment(),
                    u.isMustChangePassword(),
                    u.getRoles().stream().map(Enum::name).toList()
            );
        }
    }
}
