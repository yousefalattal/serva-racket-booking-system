package com.serva.booking.court

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class CourtResponse(
    val id: Long,
    val name: String,
    val sport: Sport,
    val available: Boolean,
)

private fun Court.toResponse() = CourtResponse(id, name, sport, isAvailable)

@RestController
@RequestMapping("/api/courts")
class CourtController(private val courts: CourtRepository) {

    // GET /api/courts            -> all courts
    // GET /api/courts?sport=padel -> only padel courts
    @GetMapping
    fun list(@RequestParam(required = false) sport: Sport?): List<CourtResponse> {
        val result = if (sport != null) courts.findBySport(sport) else courts.findAll()
        return result.map { it.toResponse() }
    }
}
