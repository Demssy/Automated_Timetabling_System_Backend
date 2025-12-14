package com.timetable.backend.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Column;
import lombok.*;

@Entity
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Admin extends AbstractUser {

    @Column(name = "has_billing_access")
    private boolean hasBillingAccess = false;

    public Admin(Long id, String email, String passwordHash, String fullName, Role role, boolean isActive, boolean hasBillingAccess) {
        super(id, email, passwordHash, fullName, role, isActive);
        this.hasBillingAccess = hasBillingAccess;
    }
}
