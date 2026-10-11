package ph.trackph.milestone.service;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.dto.request.AdminCreateUserRequest;
import ph.trackph.milestone.dto.request.LoginRequest;
import ph.trackph.milestone.dto.request.RegisterRequest;
import ph.trackph.milestone.dto.response.AuthResponse;
import ph.trackph.milestone.dto.response.UserResponse;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.model.User;
import ph.trackph.milestone.repository.UserRepository;
import ph.trackph.milestone.util.AuditContext;
import ph.trackph.milestone.util.JwtUtil;

import java.util.List;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditContext auditContext;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = buildUser(request.username(), request.email(), request.password(), request.fullName(), request.agencyName(), User.Role.PUBLIC);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getId());
    }

    @Transactional
    public UserResponse createByAdmin(AdminCreateUserRequest request) {
        User user = buildUser(request.username(), request.email(), request.password(), request.fullName(), request.agencyName(), request.role());
        userRepository.save(user);
        UserResponse response = UserResponse.from(user);
        auditContext.recordChange("User", user.getId(), null, response);
        return response;
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll(Sort.by("username")).stream().map(UserResponse::from).toList();
    }

    private User buildUser(String username, String email, String password, String fullName, String agencyName, User.Role role) {
        if (userRepository.existsByUsername(username))
            throw new IllegalArgumentException("Username already taken");
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email already registered");
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setAgencyName(agencyName);
        user.setRole(role);
        return user;
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

    public AuthService(final UserRepository userRepository, final PasswordEncoder passwordEncoder, final JwtUtil jwtUtil, final AuditContext auditContext) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditContext = auditContext;
    }
}
