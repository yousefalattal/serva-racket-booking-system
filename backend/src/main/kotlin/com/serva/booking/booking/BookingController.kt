package com.serva.booking.booking

import com.serva.booking.auth.AuthUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bookings")
class BookingController(private val service: BookingService) {

    // POST /api/bookings   (book a court)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal user: AuthUser,
        @Valid @RequestBody req: CreateBookingRequest,
    ): BookingResponse = service.create(user.id, req)

    // GET /api/bookings   (my bookings, newest first)
    @GetMapping
    fun mine(@AuthenticationPrincipal user: AuthUser): List<BookingResponse> =
        service.listFor(user.id)

    // DELETE /api/bookings/5   (cancel; returns the updated booking)
    @DeleteMapping("/{id}")
    fun cancel(
        @AuthenticationPrincipal user: AuthUser,
        @PathVariable id: Long,
    ): BookingResponse = service.cancel(user, id)
}
