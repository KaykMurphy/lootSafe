package com.lootsafe.service;

import com.lootsafe.model.User; 
import com.lootsafe.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Aqui está a mágica: importação estática apenas do método builder
import static org.springframework.security.core.userdetails.User.builder;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository repository;

    public UserDetailsServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {

        User user = repository.findByName(name)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + name));

        return builder()
                .username(user.getName())
                .password(user.getPassword())
                .roles(user.getRoles().stream().map(Enum::name).toArray(String[]::new))
                .build();
    }
}
