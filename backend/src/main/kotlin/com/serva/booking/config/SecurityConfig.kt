package com.serva.booking.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * TEMPORARY setup: /api/courts is public so we can test the endpoint.
 * Everything else requires authentication. We replace this with JWT login later.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/api/courts/**").permitAll()
                    .anyRequest().authenticated()
            }
        return http.build()
    }
}
