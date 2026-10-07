package com.serva.booking.config

import com.serva.booking.BookingRuleException
import com.serva.booking.auth.EmailAlreadyUsedException
import com.serva.booking.auth.InvalidCredentialsException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    // Rule violations (club closed, date too far ahead, ...) -> 400
    @ExceptionHandler(BookingRuleException::class)
    fun handleRule(e: BookingRuleException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))

    // Bad form input (invalid email, short password, ...) -> 400 with a message per field
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val fields = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return ResponseEntity.badRequest().body(mapOf("error" to "Validation failed", "fields" to fields))
    }

    @ExceptionHandler(EmailAlreadyUsedException::class)
    fun handleEmailUsed(e: EmailAlreadyUsedException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to (e.message ?: "Conflict")))

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleBadLogin(e: InvalidCredentialsException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to (e.message ?: "Unauthorized")))
}