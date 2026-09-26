package ph.trackph.milestone.service;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new IllegalArgumentException("Username already taken");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email already registered");

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setAgencyName(request.getAgencyName());
        user.setRole(request.getRole() != null ? request.getRole() : User.Role.PUBLIC);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid credentials");
        if (!user.isActive())
            throw new IllegalArgumentException("Account is disabled");

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }
}
