package com.serva.booking.booking

import com.serva.booking.BookingRuleException
import com.serva.booking.PricingCalculator
import com.serva.booking.auth.AuthUser
import com.serva.booking.availability.AvailabilityRepository
import com.serva.booking.availability.AvailabilityService
import com.serva.booking.court.Court
import com.serva.booking.court.CourtRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class BookingService(
    private val bookings: BookingRepository,
    private val courts: CourtRepository,
    private val pricing: AvailabilityRepository,
) {
    private val zone = ZoneId.of("Asia/Amman")
    private val calculator = PricingCalculator(zone)

    companion object {
        const val MAX_DAYS_AHEAD = 7L
        const val CANCEL_CUTOFF_HOURS = 4L   // free cancellation up to 4 hours before start
    }

    @Transactional
    fun create(userId: Long, req: CreateBookingRequest): BookingResponse {
        val court = courts.findById(req.courtId).orElseThrow { BookingRuleException("Court not found") }
        if (!court.isAvailable) throw BookingRuleException("This court is currently unavailable")

        val start = req.startsAt.toInstant()
        val end = start.plus(req.hours.toLong(), ChronoUnit.HOURS)

        if (!start.isAfter(Instant.now())) throw BookingRuleException("Start time must be in the future")

        // The 12-2 AM slots belong to the previous club day, so shift back by the opening hour
        val clubDay = start.atZone(zone).minusHours(AvailabilityService.FIRST_HOUR.toLong()).toLocalDate()
        if (clubDay.isAfter(LocalDate.now(zone).plusDays(MAX_DAYS_AHEAD))) {
            throw BookingRuleException("You can only book up to $MAX_DAYS_AHEAD days ahead")
        }

        // Throws if the booking is not on the hour or touches hours the club is closed
        val rate = pricing.rates()[court.sport]
            ?: throw BookingRuleException("No pricing configured for ${court.sport}")
        val quote = calculator.quote(start, end, rate, pricing.offPeakWindows())

        // Friendly check first; the database exclusion constraint is the safety net for simultaneous clicks
        if (bookings.countOverlapping(court.id, start, end, BookingStatus.cancelled) > 0) throw SlotTakenException()

        val saved = try {
            bookings.saveAndFlush(
                Booking(
                    userId = userId,
                    courtId = court.id,
                    startsAt = start,
                    endsAt = end,
                    totalPrice = quote.total,
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw SlotTakenException()
        }
        return toResponse(saved, court)
    }

    @Transactional(readOnly = true)
    fun listFor(userId: Long): List<BookingResponse> {
        val list = bookings.findByUserIdOrderByStartsAtDesc(userId)
        val courtById = courts.findAllById(list.map { it.courtId }.distinct()).associateBy { it.id }
        return list.mapNotNull { b -> courtById[b.courtId]?.let { toResponse(b, it) } }
    }

    @Transactional
    fun cancel(user: AuthUser, bookingId: Long): BookingResponse {
        val booking = bookings.findById(bookingId).orElseThrow { BookingNotFoundException() }

        // Members can only see and cancel their own bookings
        if (booking.userId != user.id) throw BookingNotFoundException()

        if (booking.status != BookingStatus.confirmed) {
            throw BookingRuleException("This booking is already ${booking.status.name}")
        }
        val deadline = booking.startsAt.minus(CANCEL_CUTOFF_HOURS, ChronoUnit.HOURS)
        if (Instant.now().isAfter(deadline)) {
            throw BookingRuleException(
                "Bookings can only be cancelled up to $CANCEL_CUTOFF_HOURS hours before they start. Please contact the club."
            )
        }

        booking.status = BookingStatus.cancelled
        val saved = bookings.save(booking)
        return toResponse(saved, courts.findById(saved.courtId).orElseThrow())
    }

    private fun toResponse(b: Booking, court: Court): BookingResponse {
        val cancellable = b.status == BookingStatus.confirmed &&
                Instant.now().isBefore(b.startsAt.minus(CANCEL_CUTOFF_HOURS, ChronoUnit.HOURS))
        return BookingResponse(
            id = b.id,
            courtId = court.id,
            courtName = court.name,
            sport = court.sport,
            startsAt = b.startsAt.atZone(zone).toOffsetDateTime().toString(),
            endsAt = b.endsAt.atZone(zone).toOffsetDateTime().toString(),
            hours = Duration.between(b.startsAt, b.endsAt).toHours().toInt(),
            totalPrice = b.totalPrice,
            status = b.status,
            paid = b.paidAt != null,
            cancellable = cancellable,
        )
    }
}