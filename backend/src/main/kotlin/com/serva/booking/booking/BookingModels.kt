package com.serva.booking.booking

import com.serva.booking.court.Sport
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.math.BigDecimal
import java.time.OffsetDateTime

/** Example: {"courtId": 1, "startsAt": "2026-10-10T18:00+03:00", "hours": 2} */
data class CreateBookingRequest(
    val courtId: Long,
    val startsAt: OffsetDateTime,
    @field:Min(1, message = "minimum booking is 1 hour")
    @field:Max(18, message = "too many hours")
    val hours: Int,
)

data class BookingResponse(
    val id: Long,
    val courtId: Long,
    val courtName: String,
    val sport: Sport,
    val startsAt: String,
    val endsAt: String,
    val hours: Int,
    val totalPrice: BigDecimal,   // JD, to be paid at the club
    val status: BookingStatus,
    val paid: Boolean,
    val cancellable: Boolean,     // false once inside the free-cancellation cutoff
)

class SlotTakenException : RuntimeException("That time slot was just taken. Please choose another.")

class BookingNotFoundException : RuntimeException("Booking not found")
