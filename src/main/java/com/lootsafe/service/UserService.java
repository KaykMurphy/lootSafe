package com.lootsafe.service;

import com.lootsafe.dto.request.SignupRequest;
import com.lootsafe.enums.Roles;
import com.lootsafe.model.User;
import com.lootsafe.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequest request) {
        String email = request.getEmail();

        if (repository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    String.format("User with the email address '%s' already exists.", email)
            );
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPassword(hashedPassword);

        user.getRoles().add(Roles.BUYER);

        repository.save(user);
    }
}