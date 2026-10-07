package com.serva.booking.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * TEMPORARY setup: courts and availability are public so we can test them.
 * Everything else requires authentication. We replace this with JWT login later.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/api/courts/**", "/api/availability/**").permitAll()
                    .anyRequest().authenticated()
            }
        return http.build()
    }
}