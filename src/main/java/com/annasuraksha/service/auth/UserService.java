package com.annasuraksha.service.auth;

import com.annasuraksha.model.auth.User;
import com.annasuraksha.model.auth.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepo;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(user.getRoles().stream()
                .map(SimpleGrantedAuthority::new).toList())
            .disabled(!user.isActive())
            .build();
    }

    public User createUser(String email, String password, List<String> roles, String stateCode, String fullName) {
        if (userRepo.existsByEmail(email)) throw new IllegalArgumentException("Email already registered: " + email);
        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .roles(roles)
            .stateCode(stateCode)
            .fullName(fullName)
            .active(true)
            .build();
        return userRepo.save(user);
    }

    public java.util.Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserService.class);
    public UserService(UserRepository userRepo, BCryptPasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }
}
