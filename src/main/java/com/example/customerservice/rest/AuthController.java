package com.example.customerservice.rest;

import com.example.customerservice.auth.JwtUtil;
import com.example.customerservice.servicies.UserService;
import com.example.customerservice.auth.AuthRequest;
import com.example.customerservice.auth.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtTokenUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login (@RequestBody AuthRequest authRequest) {

        if (authRequest.getUsername() == null || authRequest.getUsername().isEmpty()
                || authRequest.getPassword() == null || authRequest.getPassword().isEmpty()) {
            throw new IllegalArgumentException();
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        final UserDetails userDetails = userService.getUserDetailsByName(authRequest.getUsername());

        final String jwt = jwtTokenUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}
