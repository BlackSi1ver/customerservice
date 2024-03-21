package com.customerservice.rest.controller;

import com.customerservice.exception.InvalidArgumentException;
import com.customerservice.service.JwtService;
import com.customerservice.service.UserService;
import com.customerservice.rest.domain.AuthRequest;
import com.customerservice.rest.domain.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(final AuthenticationManager authenticationManager,
                          final UserService userService,
                          final JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login (@RequestBody AuthRequest authRequest) {

        if (authRequest.getUsername() == null || authRequest.getUsername().isEmpty()
                || authRequest.getPassword() == null || authRequest.getPassword().isEmpty()) {
            throw new InvalidArgumentException("Invalid username or password");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        final UserDetails userDetails = userService.getUserDetailsByName(authRequest.getUsername());

        final String jwt = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}
