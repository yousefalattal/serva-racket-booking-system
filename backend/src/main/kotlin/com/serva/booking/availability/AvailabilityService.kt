package com.serva.booking.availability

import com.serva.booking.BookingRuleException
import com.serva.booking.PricingCalculator
import com.serva.booking.court.CourtRepository
import com.serva.booking.court.Sport
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SlotResponse(
    val startsAt: String,   // e.g. 2026-10-15T17:00+03:00
    val endsAt: String,
    val label: String,      // e.g. 17:00
    val peak: Boolean,
    val price: BigDecimal,  // JD for this hour
    val status: String,     // free | booked | past
)

data class CourtAvailability(
    val courtId: Long,
    val courtName: String,
    val slots: List<SlotResponse>,
)

data class AvailabilityResponse(
    val date: String,
    val sport: Sport,
    val courts: List<CourtAvailability>,
)

@Service
class AvailabilityService(
    private val courts: CourtRepository,
    private val repo: AvailabilityRepository,
) {
    private val zone = ZoneId.of("Asia/Amman")
    private val calculator = PricingCalculator(zone)

    companion object {
        const val MAX_DAYS_AHEAD = 7L
        const val FIRST_HOUR = 8        // club opens at 08:00
        const val SLOTS_PER_DAY = 18    // 08:00 -> 02:00 next day
    }

    /**
     * One "club day" runs from 08:00 on [date] to 02:00 the next morning,
     * so the 12 AM - 2 AM slots belong to the previous day's session.
     */
    fun forDay(date: LocalDate, sport: Sport): AvailabilityResponse {
        val today = LocalDate.now(zone)
        // yesterday is allowed because its late-night slots (12-2 AM) may still be upcoming
        if (date.isBefore(today.minusDays(1)) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw BookingRuleException("Date must be within the next $MAX_DAYS_AHEAD days")
        }

        val rate = repo.rates()[sport]
            ?: throw BookingRuleException("No pricing configured for $sport")
        val windows = repo.offPeakWindows()
        val sportCourts = courts.findBySport(sport).filter { it.isAvailable }

        val dayStart = date.atTime(FIRST_HOUR, 0).atZone(zone)
        val dayEnd = dayStart.plusHours(SLOTS_PER_DAY.toLong())
        val bookedByCourt = repo
            .bookedRanges(dayStart.toInstant(), dayEnd.toInstant())
            .groupBy { it.courtId }
        val now = Instant.now()

        val result = sportCourts.map { court ->
            val ranges = bookedByCourt[court.id].orEmpty()

            val slots = (0 until SLOTS_PER_DAY).map { i ->
                val start = dayStart.plusHours(i.toLong())
                val end = start.plusHours(1)

                // Reuse the pricing calculator so prices always match booking prices
                val line = calculator
                    .quote(start.toInstant(), end.toInstant(), rate, windows)
                    .lines.first()

                val isBooked = ranges.any {
                    it.startsAt < end.toInstant() && it.endsAt > start.toInstant()
                }
                val status = when {
                    isBooked -> "booked"
                    !start.toInstant().isAfter(now) -> "past"
                    else -> "free"
                }

                SlotResponse(
                    startsAt = start.toOffsetDateTime().toString(),
                    endsAt = end.toOffsetDateTime().toString(),
                    label = "%02d:00".format(start.hour),
                    peak = line.isPeak,
                    price = line.price,
                    status = status,
                )
            }
            CourtAvailability(court.id, court.name, slots)
        }

        return AvailabilityResponse(date.toString(), sport, result)
    }
}
