package com.ghanaride.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ProfileCompletionInterceptor profileCompletionInterceptor;
    private final GhanaOnlyInterceptor ghanaOnlyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // v5.2 GHANA ONLY – runs first, adds GH headers, timezone Africa/Accra
        registry.addInterceptor(ghanaOnlyInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/css/**","/js/**","/images/**","/uploads/**","/actuator/**","/error/**");

        registry.addInterceptor(profileCompletionInterceptor)
                .addPathPatterns("/booking/**", "/payment/**", "/reviews/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/uploads/**",
                        "/error/**", "/actuator/**",
                        "/login", "/register", "/logout",
                        "/profile", "/profile/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
