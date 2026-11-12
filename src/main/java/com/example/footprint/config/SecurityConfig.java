package com.example.footprint.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
