package com.serva.booking.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.expiration-minutes}") private val expirationMinutes: Long,
) {
    // The secret must be at least 32 characters for HS256
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    fun generate(user: User): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact()
    }

    /** Returns the user inside a valid token, or null if the token is invalid or expired. */
    fun parse(token: String): AuthUser? = try {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        AuthUser(
            id = claims.subject.toLong(),
            email = claims["email"] as String,
            role = Role.valueOf(claims["role"] as String),
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Runs on every request: if there is a valid "Authorization: Bearer <token>" header,
 * mark the request as logged in. Otherwise leave it anonymous (Spring Security then blocks it).
 */
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.parse(header.substring(7))?.let { user ->
                val authority = SimpleGrantedAuthority("ROLE_${user.role.name.uppercase()}")
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(user, null, listOf(authority))
            }
        }
        filterChain.doFilter(request, response)
    }
}
