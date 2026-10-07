package com.serva.booking.availability

import com.serva.booking.OffPeakWindow
import com.serva.booking.SportRate
import com.serva.booking.court.Sport
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class BookedRange(val courtId: Long, val startsAt: Instant, val endsAt: Instant)

/**
 * Plain SQL reads used to build the availability calendar.
 */
@Repository
class AvailabilityRepository(private val jdbc: JdbcTemplate) {

    fun rates(): Map<Sport, SportRate> =
        jdbc.query(
            "SELECT sport::text AS sport, peak_price, off_peak_price FROM pricing",
            RowMapper<Pair<Sport, SportRate>> { rs, _ ->
                Sport.valueOf(rs.getString("sport")) to
                    SportRate(rs.getBigDecimal("peak_price"), rs.getBigDecimal("off_peak_price"))
            },
        ).toMap()

    fun offPeakWindows(): List<OffPeakWindow> =
        jdbc.query(
            "SELECT start_hour, end_hour FROM off_peak_windows",
            RowMapper<OffPeakWindow> { rs, _ ->
                OffPeakWindow(rs.getInt("start_hour"), rs.getInt("end_hour"))
            },
        )

    /** Active (not cancelled) bookings that overlap the window [from, to). */
    fun bookedRanges(from: Instant, to: Instant): List<BookedRange> =
        jdbc.query(
            """
            SELECT court_id, starts_at, ends_at
            FROM bookings
            WHERE status <> 'cancelled' AND starts_at < ? AND ends_at > ?
            """.trimIndent(),
            RowMapper<BookedRange> { rs, _ ->
                BookedRange(
                    rs.getLong("court_id"),
                    rs.getObject("starts_at", OffsetDateTime::class.java).toInstant(),
                    rs.getObject("ends_at", OffsetDateTime::class.java).toInstant(),
                )
            },
            to.atOffset(ZoneOffset.UTC),
            from.atOffset(ZoneOffset.UTC),
        )
}
