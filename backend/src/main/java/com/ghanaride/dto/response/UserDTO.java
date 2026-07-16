package com.ghanaride.dto.response;

import com.ghanaride.entity.Role;
import com.ghanaride.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private String accountType;
    private String gender;
    private String address;
    private String profileImagePath;
    private String companyName;
    private Boolean emailVerified;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private Boolean isProfileComplete;

    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .accountType(user.getAccountType())
                .gender(user.getGender())
                .address(user.getAddress())
                .profileImagePath(user.getProfileImagePath())
                .companyName(user.getCompanyName())
                .emailVerified(user.getEmailVerified())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .isProfileComplete(user.isProfileComplete())
                .build();
    }
}
