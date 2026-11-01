package gmbh.tantive.jsonplaceholder_crud.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("auth_accounts_by_email")
public class AuthAccount {

    @PrimaryKey
    private String email;

    @Column("account_id")
    private UUID accountId;

    @Column("password_hash")
    private String passwordHash;

    @Column("role")
    private String role;

    @Column("enabled")
    private boolean enabled;

    @Column("created_at")
    private Instant createdAt;

    public AuthAccount() {
    }

    public AuthAccount(String email, UUID accountId, String passwordHash, String role, boolean enabled,
            Instant createdAt) {
        this.email = email;
        this.accountId = accountId;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}