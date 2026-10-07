package com.serva.booking

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Off-peak window in local whole hours: startHour inclusive, endHour exclusive. */
data class OffPeakWindow(val startHour: Int, val endHour: Int)

data class SportRate(val peakPrice: BigDecimal, val offPeakPrice: BigDecimal)

data class PriceLine(val hourStart: ZonedDateTime, val isPeak: Boolean, val price: BigDecimal)

data class PriceQuote(val lines: List<PriceLine>, val total: BigDecimal)

class BookingRuleException(message: String) : RuntimeException(message)

/**
 * Prices a booking hour by hour.
 * Club is open 08:00 to 02:00 (next day), so a booking is valid only if
 * every hour it covers starts at 08..23 or 00..01 local time.
 */
class PricingCalculator(
    private val zone: ZoneId = ZoneId.of("Asia/Amman"),
    private val openHour: Int = 8,
    private val closeHour: Int = 2,
) {

    fun isOpenAt(hour: Int): Boolean = hour >= openHour || hour < closeHour

    fun quote(
        startsAt: Instant,
        endsAt: Instant,
        rate: SportRate,
        offPeakWindows: List<OffPeakWindow>,
    ): PriceQuote {
        val start = startsAt.atZone(zone)
        val end = endsAt.atZone(zone)

        if (!end.isAfter(start)) throw BookingRuleException("End must be after start")
        if (start.minute != 0 || start.second != 0 || end.minute != 0 || end.second != 0) {
            throw BookingRuleException("Bookings must start and end on the hour")
        }

        val lines = mutableListOf<PriceLine>()
        var cursor = start
        while (cursor.isBefore(end)) {
            val hour = cursor.hour
            if (!isOpenAt(hour)) {
                throw BookingRuleException("Club is closed at %02d:00".format(hour))
            }
            val offPeak = offPeakWindows.any { hour >= it.startHour && hour < it.endHour }
            lines += PriceLine(
                hourStart = cursor,
                isPeak = !offPeak,
                price = if (offPeak) rate.offPeakPrice else rate.peakPrice,
            )
            cursor = cursor.plusHours(1)
        }

        return PriceQuote(lines, lines.fold(BigDecimal.ZERO) { acc, l -> acc + l.price })
    }
}
