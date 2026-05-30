package com.lootsafe.controller;

import com.lootsafe.dto.request.LoginRequest;
import com.lootsafe.dto.request.LoginResponse;
import com.lootsafe.dto.request.SignupRequest;
import com.lootsafe.helper.JwtHelper;
import com.lootsafe.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtHelper jwtHelper;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest requestDto) {
        userService.signup(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(value = "/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.name(), request.password()));
        String token = jwtHelper.generateToken(request.name());
        return ResponseEntity.ok(new LoginResponse(request.name(), token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUserName(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "name", authentication.getName(),
                "roles", authentication.getAuthorities()
        ));
    }

}
