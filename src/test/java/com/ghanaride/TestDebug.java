package com.ghanaride;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import com.ghanaride.service.CustomUserDetailsService;

@SpringBootTest
public class TestDebug {
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Test
    public void testLogin() {
        try {
            customUserDetailsService.loadUserByUsername("admin");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
