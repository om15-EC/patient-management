package com.gl.authservice.service;

import com.gl.authservice.dto.LoginRequestDTO;
import com.gl.authservice.dto.LoginResponseDTO;
import com.gl.authservice.dto.RegisterRequestDTO;
import com.gl.authservice.dto.RegisterResponseDTO;
import com.gl.authservice.exception.EmailAlreadyExistsException;
import com.gl.authservice.exception.InvalidCredentialsException;
import com.gl.authservice.exception.UserDisabledException;
import com.gl.authservice.model.Role;
import com.gl.authservice.model.User;
import com.gl.authservice.repository.UserRepository;
import com.gl.authservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;


    public RegisterResponseDTO register(RegisterRequestDTO registerRequestDTO){

        if(userRepository.existsByEmail(registerRequestDTO.email())){
            throw new EmailAlreadyExistsException("Email already registered" + registerRequestDTO.email());
        }

        Role role;
        try {
            role = Role.valueOf(registerRequestDTO.role().toUpperCase());
        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Invalid role, accepted values are: ROLE_USER, ROLE_ADMIN");
        }

        User user = User.builder()
                .email(registerRequestDTO.email())
                .password(passwordEncoder.encode(registerRequestDTO.password()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with email {}:", savedUser.getEmail());
        return new RegisterResponseDTO(
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getCreatedAt()
        );

    }


    public LoginResponseDTO authenticate(LoginRequestDTO loginRequestDTO) {

        User user = userRepository.findByEmail(loginRequestDTO.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

//        log.info("input password: {}", loginRequestDTO.password());
//        log.info("stored hash: {}", user.getPassword());
//        log.info("Match result: {}",passwordEncoder.matches(loginRequestDTO.password(), user.getPassword() ));


        if(!passwordEncoder.matches(loginRequestDTO.password(), user.getPassword())){
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if(!user.isEnabled()){
            throw new UserDisabledException("user account is disabled");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
//        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        long expiresIn = jwtUtil.getExpiration();
        String role = user.getRole().name();

        tokenBlacklistService.storeRefreshToken(user.getEmail(),refreshToken);

        return LoginResponseDTO.of(accessToken,refreshToken, expiresIn, role);
    }
}
