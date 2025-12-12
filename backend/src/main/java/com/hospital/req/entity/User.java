package com.hospital.req.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity @Table(name = "users")
@Getter @Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String employeeId; // 工号

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 64)
    private String department;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean mustChangePassword = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 20, unique = true)
    private String phone;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name="user_roles", joinColumns=@JoinColumn(name="user_id"))
    @Column(name="role")
    private Set<Role> roles;
}
