package com.multimodal.rag.service;

import com.multimodal.rag.model.Role;
import com.multimodal.rag.model.User;
import com.multimodal.rag.model.dto.AuthResponse;
import com.multimodal.rag.model.dto.LoginRequest;
import com.multimodal.rag.model.dto.RegisterRequest;
import com.multimodal.rag.model.dto.UserDTO;
import com.multimodal.rag.repository.RoleRepository;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setIsActive(true);
        
        String requestedRole = request.getRoleName() == null || request.getRoleName().isBlank()
                ? "USER"
                : request.getRoleName().trim().toUpperCase(Locale.ROOT);

        if (!Set.of("USER", "PREMIUM").contains(requestedRole)) {
            throw new RuntimeException("Unsupported role: " + requestedRole);
        }

        Role assignedRole = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new RuntimeException("Requested role not found: " + requestedRole));
        user.setRoles(new HashSet<>(Set.of(assignedRole)));
        
        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
        
        // Create authentication object manually for token generation 
        // to avoid transactional deadlock with authenticationManager
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, user.getRoles().stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList())
        );
        
        String token = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        return buildAuthResponse(user, token, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User logged in: {}", user.getUsername());

        return buildAuthResponse(user, accessToken, refreshToken);
    }
    
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .roles(new ArrayList<>(roleNames))
                .build();

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .type("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
                .user(userDTO)
                .roles(roleNames)
                .build();
    }

    /**
     * Refresh access token using a valid refresh token.
     * Returns new access + refresh token pair.
     */
    public AuthResponse refreshAccessToken(String refreshToken) {
        // Validate the refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("无效的刷新令牌");
        }
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("该令牌不是刷新令牌");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("用户已被禁用");
        }

        // Build authentication to generate new tokens
        String roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .collect(Collectors.joining(","));

        String newAccessToken = tokenProvider.generateAccessToken(username, roles);

        // Rotate refresh token for security
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, user.getRoles().stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList())
        );
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        log.info("Token refreshed for user: {}", user.getUsername());

        return AuthResponse.refreshOnly(newAccessToken, newRefreshToken, tokenProvider.getAccessTokenExpirationSeconds());
    }
}
