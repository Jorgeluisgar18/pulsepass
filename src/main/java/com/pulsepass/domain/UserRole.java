package com.pulsepass.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_roles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @MapsId("userId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    protected UserRole() {
    }

    public UserRole(
            User user,
            Role role
    ) {
        this.user = user;

        this.id = new UserRoleId(
                user.getId(),
                role
        );
    }

    public UserRoleId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Role getRole() {
        return id.getRole();
    }
}