package com.annasuraksha.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final H2ConsoleInterceptor h2ConsoleInterceptor;

    @Autowired
    public WebMvcConfig(H2ConsoleInterceptor h2ConsoleInterceptor) {
        this.h2ConsoleInterceptor = h2ConsoleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(h2ConsoleInterceptor).addPathPatterns("/h2-console/**", "/dev/console/**");
    }
}
