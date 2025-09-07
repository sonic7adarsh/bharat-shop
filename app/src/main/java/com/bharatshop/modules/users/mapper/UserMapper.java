package com.bharatshop.modules.users.mapper;

import com.bharatshop.modules.users.dto.UserDto;
import com.bharatshop.shared.entity.User;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for User entity and DTOs.
 * Provides type-safe mapping between domain objects and DTOs.
 */
@Component
public class UserMapper {
    
    /**
     * Maps User entity to UserResponse DTO.
     */
    public UserDto.UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        
        return new UserDto.UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            user.getRoles().isEmpty() ? null : user.getRoles().iterator().next(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * Maps CreateUserRequest DTO to User entity.
     */
    public User toEntity(UserDto.CreateUserRequest request) {
        if (request == null) {
            return null;
        }
        
        return User.builder()
            .email(request.email())
            .firstName(request.firstName())
            .lastName(request.lastName())
            .phone(request.phone())
            .status(User.UserStatus.ACTIVE)
            .userType(User.UserType.CUSTOMER)
            .build();
    }
    
    /**
     * Updates existing User entity with UpdateUserRequest data.
     */
    public void updateEntity(UserDto.UpdateUserRequest request, User user) {
        if (request == null || user == null) {
            return;
        }
        
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
    }
}