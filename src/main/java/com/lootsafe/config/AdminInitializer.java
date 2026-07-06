package com.lootsafe.config;

import com.lootsafe.enums.Roles;
import com.lootsafe.model.User;
import com.lootsafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;


    @Value("${admin_password}")
    private String password;

    @Value("${email_admin}")
    private String emailAdmin;

    @Override
    public void run(String... args) throws Exception {

        boolean adminExists = userRepository.existsByEmail(emailAdmin);

        if (!adminExists){
            log.info("Admin user not created. Creating...");

            User user = new User();
            user.setEmail(emailAdmin);
            user.setName("Admin LOOTSAFE");
            user.setPassword(passwordEncoder.encode(password));
            user.getRoles().add(Roles.MODERADOR);

            userRepository.save(user);

            log.info("Default admin user created successfully.");
        }

    }

}
