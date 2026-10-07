package com.serva.booking.availability

import com.serva.booking.court.Sport
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/availability")
class AvailabilityController(private val service: AvailabilityService) {

    // GET /api/availability?date=2026-10-15&sport=padel
    @GetMapping
    fun get(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam sport: Sport,
    ): AvailabilityResponse = service.forDay(date, sport)
}
