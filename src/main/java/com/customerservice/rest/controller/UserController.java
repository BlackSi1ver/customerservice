package com.customerservice.rest.controller;

import com.customerservice.exception.ForbiddenAccessException;
import com.customerservice.exception.InvalidArgumentException;
import com.customerservice.rest.domain.UpdateUserRoleRequest;
import com.customerservice.rest.domain.UserDto;
import com.customerservice.domain.ERole;
import com.customerservice.service.UserService;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(
                userService.findAll().stream()
                        .map(userService::convertToDto)
                        .collect(Collectors.toList())
        );
    }

    @PutMapping("/role/{name}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserDto> updateUserRoles(@NonNull @PathVariable String name,
                                                   @NonNull @RequestBody UpdateUserRoleRequest request) {

        if (name.isEmpty()) {
            throw new InvalidArgumentException("Invalid username");
        }

        if (!request.getRole().equals(ERole.OPERATOR)) {
            throw new ForbiddenAccessException("Invalid role");
        }

        return ResponseEntity.ok(
                userService.convertToDto(
                        userService.setUserRole(name, request.getRole(), request.isEnable())
                )
        );
    }
}
