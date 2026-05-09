package br.com.accenture.auth.infrastructure.persistence.entity;

import br.com.accenture.auth.domain.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCredentialEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_credential_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}