package no.nav.aap.statistikk

import no.nav.aap.komponenter.repository.Repository

data class Statistikk(
    val mottatteMeldekortTotalt: Long,
    val mottatteMeldekortIDag: Long,
    val varslerSendtIDag: Long,
    val varslerInaktivertIDag: Long,
    val varslerPlanlagt: Long,
)

interface StatistikkRepository : Repository {
    fun hentStatistikk(): Statistikk
}
