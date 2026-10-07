package com.serva.booking.booking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant

// Lowercase names match the booking_status enum in PostgreSQL
enum class BookingStatus { confirmed, cancelled, no_show }

@Entity
@Table(name = "bookings")
class Booking(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id")
    val userId: Long = 0,

    @Column(name = "court_id")
    val courtId: Long = 0,

    @Column(name = "starts_at")
    val startsAt: Instant = Instant.EPOCH,

    @Column(name = "ends_at")
    val endsAt: Instant = Instant.EPOCH,

    @Column(name = "total_price")
    val totalPrice: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    var status: BookingStatus = BookingStatus.confirmed,

    // Set by the front desk when the member pays at the club
    @Column(name = "paid_at")
    val paidAt: Instant? = null,
)

interface BookingRepository : JpaRepository<Booking, Long> {

    fun findByUserIdOrderByStartsAtDesc(userId: Long): List<Booking>

    /**
     * How many bookings overlap [start, end) on this court, ignoring those with status [excluded]
     * (pass BookingStatus.cancelled). The status is a parameter, not a literal in the query,
     * because Hibernate would cast a literal to the wrong PostgreSQL enum type name.
     */
    @Query(
        """
        select count(b) from Booking b
        where b.courtId = :courtId
          and b.status <> :excluded
          and b.startsAt < :end
          and b.endsAt > :start
        """
    )
    fun countOverlapping(
        @Param("courtId") courtId: Long,
        @Param("start") start: Instant,
        @Param("end") end: Instant,
        @Param("excluded") excluded: BookingStatus,
    ): Long
}