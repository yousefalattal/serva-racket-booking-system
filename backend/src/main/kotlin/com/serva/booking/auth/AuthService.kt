package com.serva.booking.auth

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val users: UserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService,
) {

    fun register(req: RegisterRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        if (users.existsByEmail(email)) throw EmailAlreadyUsedException()

        // Everyone who registers through the API is a member. Admins are promoted manually in the database.
        val user = users.save(
            User(
                email = email,
                passwordHash = requireNotNull(encoder.encode(req.password)),
                fullName = req.fullName.trim(),
                phone = req.phone?.trim()?.ifBlank { null },
                role = Role.member,
            )
        )
        return AuthResponse(jwt.generate(user), user.toResponse())
    }

    fun login(req: LoginRequest): AuthResponse {
        val user = users.findByEmail(req.email.trim().lowercase())

        // Same error for "no such email" and "wrong password" so attackers can't probe which emails exist
        if (user == null || !user.isActive || !encoder.matches(req.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return AuthResponse(jwt.generate(user), user.toResponse())
    }

    fun me(userId: Long): UserResponse =
        users.findById(userId).orElseThrow { InvalidCredentialsException() }.toResponse()
}
