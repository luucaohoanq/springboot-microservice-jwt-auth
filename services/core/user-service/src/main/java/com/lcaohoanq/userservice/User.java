package com.lcaohoanq.userservice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lcaohoanq.commonlibrary.dto.UserResponse;
import com.lcaohoanq.commonlibrary.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name= "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String password;

    @Size(max = 20)
    @Column(name = "activation_key")
    @JsonIgnore
    private String activationKey;

    @Size(max = 20)
    @Column(name = "reset_key")
    @JsonIgnore
    private String resetKey;

    @Size(min = 2, max = 10)
    @Column(name = "lang_key")
    private String langKey;

    @Enumerated(EnumType.STRING)
    private Role role;

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                                user.getRole().name(),
                                user.getActivationKey(), user.getResetKey(), user.getLangKey());
    }
}
