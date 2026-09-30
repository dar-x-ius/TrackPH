package ph.trackph.milestone.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.dto.request.LoginRequest;
import ph.trackph.milestone.dto.request.RegisterRequest;
import ph.trackph.milestone.dto.response.AuthResponse;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.model.User;
import ph.trackph.milestone.repository.UserRepository;
import ph.trackph.milestone.util.JwtUtil;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username()))
            throw new IllegalArgumentException("Username already taken");
        if (userRepository.existsByEmail(request.email()))
            throw new IllegalArgumentException("Email already registered");
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setAgencyName(request.agencyName());
        user.setRole(request.role() != null ? request.role() : User.Role.PUBLIC);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid credentials");
        if (!user.isActive())
            throw new IllegalArgumentException("Account is disabled");
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    public AuthService(final UserRepository userRepository, final PasswordEncoder passwordEncoder, final JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
}
