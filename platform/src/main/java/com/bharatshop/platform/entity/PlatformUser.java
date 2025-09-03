package com.bharatshop.platform.entity;

import com.bharatshop.shared.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Entity(name = "PlatformPlatformUser")
@Table(name = "platform_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlatformUser extends BaseEntity {

    @Column(unique = true, nullable = false)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @ElementCollection(targetClass = PlatformRole.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "platform_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<PlatformRole> roles = Set.of(PlatformRole.VENDOR);

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonExpired = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    public enum PlatformRole {
        ADMIN,
        VENDOR,
        STAFF
    }
    
    // Manual getters for compilation compatibility
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public Set<PlatformRole> getRoles() {
        return roles;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public Boolean getAccountNonExpired() {
        return accountNonExpired;
    }
    
    public Boolean getAccountNonLocked() {
        return accountNonLocked;
    }
    
    public Boolean getCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    // Manual builder method for compilation compatibility
    public static PlatformUserBuilder builder() {
        return new PlatformUserBuilder();
    }
    
    public static class PlatformUserBuilder {
        private String email;
        private String password;
        private Set<PlatformRole> roles;
        private Boolean enabled = true;
        private Boolean accountNonExpired = true;
        private Boolean accountNonLocked = true;
        private Boolean credentialsNonExpired = true;
        
        public PlatformUserBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public PlatformUserBuilder password(String password) {
            this.password = password;
            return this;
        }
        
        public PlatformUserBuilder roles(Set<PlatformRole> roles) {
            this.roles = roles;
            return this;
        }
        
        public PlatformUserBuilder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public PlatformUserBuilder accountNonExpired(Boolean accountNonExpired) {
            this.accountNonExpired = accountNonExpired;
            return this;
        }
        
        public PlatformUserBuilder accountNonLocked(Boolean accountNonLocked) {
            this.accountNonLocked = accountNonLocked;
            return this;
        }
        
        public PlatformUserBuilder credentialsNonExpired(Boolean credentialsNonExpired) {
            this.credentialsNonExpired = credentialsNonExpired;
            return this;
        }
        
        public PlatformUser build() {
            PlatformUser user = new PlatformUser();
            user.email = this.email;
            user.password = this.password;
            user.roles = this.roles;
            user.enabled = this.enabled;
            user.accountNonExpired = this.accountNonExpired;
            user.accountNonLocked = this.accountNonLocked;
            user.credentialsNonExpired = this.credentialsNonExpired;
            return user;
        }
    }
}