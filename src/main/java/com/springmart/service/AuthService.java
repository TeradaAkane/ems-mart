package com.springmart.service;

import com.springmart.dto.LoginRequest;
import com.springmart.dto.LoginResponse;
import com.springmart.entity.User;
import com.springmart.repository.UserRepository;
import com.springmart.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ユーザー名またはパスワードが正しくありません"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ユーザー名またはパスワードが正しくありません");
        }
        
        String token = jwtTokenProvider.generateToken(user.getUserName(), user.getRole());
        return new LoginResponse(token);
    }
}

