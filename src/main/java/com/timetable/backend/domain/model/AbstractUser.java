package com.timetable.backend.domain.model;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AbstractUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Email
    @NotBlank
    private String email;

    @Column(name = "password_hash", nullable = false)
    @NotBlank
    @Size(min = 6)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    /** All roles of the account (e.g. TEACHER + STUDENT for dual-role users); "role" stays the primary one. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    private Set<Role> roles = new HashSet<>();

    @Column(name = "is_active")
    private boolean isActive = true;

    /** Returns the names of all roles (primary + additional), e.g. ["TEACHER", "STUDENT"]. */
    public Set<String> getRoleNames() {
        Set<String> names = new LinkedHashSet<>();
        if (role != null) {
            names.add(role.getName());
        }
        if (roles != null) {
            roles.forEach(r -> names.add(r.getName()));
        }
        return names;
    }

    public boolean hasRole(String roleName) {
        return getRoleNames().contains(roleName);
    }

    /** Grants an additional role while keeping the primary role in the collection. */
    public void addRole(Role newRole) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        if (role != null) {
            roles.add(role);
        }
        roles.add(newRole);
    }
}
