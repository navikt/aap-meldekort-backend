package no.nav.aap.kallogg

import no.nav.aap.Ident
import no.nav.aap.komponenter.repository.Repository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class KallLoggStart(
    val id: UUID,
    val receivedAt: Instant,
    val method: String,
    val path: String,
    val queryParameters: Map<String, List<String>>,
)

enum class KallLoggUtfall {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    TIMED_OUT,
    INTERRUPTED,
}

interface KallLoggRepository : Repository {
    fun start(ident: Ident, paaDag: LocalDate, entry: KallLoggStart): UUID?

    fun complete(
        id: UUID,
        completedAt: Instant,
        status: Int?,
        responseBody: String?,
        outcome: KallLoggUtfall,
    )
}
