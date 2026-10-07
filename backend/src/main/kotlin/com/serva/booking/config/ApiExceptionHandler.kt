package com.serva.booking.config

import com.serva.booking.BookingRuleException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    // Any rule violation (club closed, date too far ahead, ...) becomes a 400 with a readable message
    @ExceptionHandler(BookingRuleException::class)
    fun handleRule(e: BookingRuleException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
}
