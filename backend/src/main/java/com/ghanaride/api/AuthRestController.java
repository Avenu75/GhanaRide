package com.ghanaride.api;

import com.ghanaride.dto.RegisterRequestDTO;
import com.ghanaride.dto.response.ApiResponse;
import com.ghanaride.dto.response.JwtResponse;
import com.ghanaride.dto.response.UserDTO;
import com.ghanaride.entity.User;
import com.ghanaride.security.jwt.JwtTokenProvider;
import com.ghanaride.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://ghanaride.me"}, allowCredentials = "true")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userService.findByEmail(request.getEmail()).orElseThrow();
        String token = tokenProvider.generateToken(user.getUsername(), user.getId(), user.getRole().name());
        String refresh = tokenProvider.generateRefreshToken(user.getUsername());
        JwtResponse jwtResponse = new JwtResponse(token, refresh, UserDTO.fromEntity(user));
        return ResponseEntity.ok(ApiResponse.success("Login successful", jwtResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(@RequestBody RegisterRequestDTO dto) {
        User user = userService.registerPassenger(dto);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", UserDTO.fromEntity(user)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String,String>>> refresh(@RequestBody Map<String,String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid refresh token"));
        }
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userService.findByUsername(username).orElseThrow();
        String newToken = tokenProvider.generateToken(user.getUsername(), user.getId(), user.getRole().name());
        String newRefresh = tokenProvider.generateRefreshToken(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(Map.of("token", newToken, "refreshToken", newRefresh)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> me(Principal principal) {
        User user = userService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(UserDTO.fromEntity(user)));
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }
}
