package com.astra.backend.auth;

import com.astra.backend.user.UserEntity;
import com.astra.backend.user.UserRepository;
import com.astra.backend.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {}

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim();
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("code", "EMAIL_TAKEN", "message", "이미 사용 중인 이메일입니다.")
            );
        }

        UserEntity u = new UserEntity();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(UserRole.USER);
        UserEntity saved = users.save(u);
        users.flush();
        if (saved.getId() == null) {
            throw new IllegalStateException("User id not assigned after persist");
        }

        String token = jwtService.createToken(saved.getId(), saved.getEmail(), saved.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "accessToken", token,
                "tokenType", "Bearer"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        UserEntity user = users.findByEmail(req.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("code", "INVALID_CREDENTIALS", "message", "이메일 또는 비밀번호가 올바르지 않습니다.")
            );
        }

        String token = jwtService.createToken(user.getId(), user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "tokenType", "Bearer"
        ));
    }
}

