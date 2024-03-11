package com.example.customerservice.rest;

import com.example.customerservice.data.UserDto;
import com.example.customerservice.repositories.UsersRepository;
import com.example.customerservice.data.ERole;
import com.example.customerservice.data.UserEntry;
import com.example.customerservice.requests.UpdateRolesRequest;
import com.example.customerservice.servicies.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(
                usersRepository.findAll().stream()
                        .map(userService::convertToDto)
                        .collect(Collectors.toList())
        );
    }

    @PutMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void updateUserRoles(@RequestBody UpdateRolesRequest updateRequest) {

        if (updateRequest.getUsername() == null || updateRequest.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Invalid username");
        }

        final Optional<UserEntry> userEntry = usersRepository.findByName(updateRequest.getUsername());
        if (userEntry.isPresent()) {
            final Set<ERole> roles = userEntry.get().getRoles();
            if (updateRequest.isSetOperatorRole())
                roles.add(ERole.OPERATOR);
            else
                roles.remove(ERole.OPERATOR);
            usersRepository.updateUserRoles(updateRequest.getUsername(), roles);
        }
        else {
            throw new NoSuchElementException();
        }
    }
}
