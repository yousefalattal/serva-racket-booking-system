package com.serva.booking.court

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

// Lowercase names match the values of the sport_type enum in PostgreSQL
enum class Sport { padel, tennis, pickleball }

@Entity
@Table(name = "courts")
class Court(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val name: String = "",

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    val sport: Sport = Sport.padel,

    @Column(name = "is_available")
    val isAvailable: Boolean = true,
)

interface CourtRepository : JpaRepository<Court, Long> {
    fun findBySport(sport: Sport): List<Court>
}