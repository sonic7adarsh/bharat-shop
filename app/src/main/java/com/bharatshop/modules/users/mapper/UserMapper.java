package com.bharatshop.modules.users.mapper;

import com.bharatshop.modules.users.dto.UserDto;
import com.bharatshop.modules.users.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for User entity and DTOs.
 * Provides type-safe mapping between domain objects and DTOs.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
    
    /**
     * Maps User entity to UserResponse DTO.
     */
    @Mapping(target = "id", source = "id")
    UserDto.UserResponse toResponse(User user);
    
    /**
     * Maps CreateUserRequest DTO to User entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true) // Will be set by service
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "tenantId", ignore = true) // Will be set by service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    User toEntity(UserDto.CreateUserRequest request);
    
    /**
     * Updates existing User entity with UpdateUserRequest data.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UserDto.UpdateUserRequest request, @MappingTarget User user);
}