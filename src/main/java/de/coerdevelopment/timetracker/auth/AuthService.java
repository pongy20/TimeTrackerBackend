package de.coerdevelopment.timetracker.auth;

import de.coerdevelopment.timetracker.security.JwtService;
import de.coerdevelopment.timetracker.user.User;
import de.coerdevelopment.timetracker.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public String register(String username, String password) {
        if (userRepository.existsByUsername(username)) throw new IllegalArgumentException("Username bereits vergeben");
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole("USER");
        userRepository.save(u);
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername()).password(u.getPassword()).roles(u.getRole()).build();
        return jwtService.generateToken(details);
    }

    public String login(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        User user = userRepository.findByUsername(username).orElseThrow();
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername()).password(user.getPassword()).roles(user.getRole()).build();
        return jwtService.generateToken(details);
    }
}

