package com.serva.booking.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank @field:Email
    val email: String,

    // BCrypt only uses the first 72 bytes, so cap the length
    @field:NotBlank @field:Size(min = 8, max = 72, message = "must be 8 to 72 characters")
    val password: String,

    @field:NotBlank @field:Size(max = 255)
    val fullName: String,

    @field:Size(max = 20)
    val phone: String? = null,
)

data class LoginRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String,
)

data class UserResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val phone: String?,
    val role: Role,
)

data class AuthResponse(val token: String, val user: UserResponse)

/** The logged-in user, rebuilt from the JWT on every request. */
data class AuthUser(val id: Long, val email: String, val role: Role)

fun User.toResponse() = UserResponse(id, email, fullName, phone, role)

class EmailAlreadyUsedException : RuntimeException("Email already registered")

class InvalidCredentialsException : RuntimeException("Invalid email or password")
