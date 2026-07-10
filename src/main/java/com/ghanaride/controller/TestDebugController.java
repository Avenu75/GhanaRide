package com.ghanaride.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestDebugController {

    private final AuthenticationManager authenticationManager;

    @GetMapping("/api/debug/login")
    public String debugLogin(@RequestParam String username, @RequestParam String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            return "SUCCESS: Authenticated as " + auth.getName();
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getClass().getName() + " - " + e.getMessage();
        }
    }
}
