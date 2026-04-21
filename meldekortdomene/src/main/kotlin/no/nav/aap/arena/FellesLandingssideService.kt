package no.nav.aap.arena

import no.nav.aap.kelvin.MeldekortTilUtfylling
import no.nav.aap.kelvin.Meldekortstatus
import no.nav.aap.lookup.gateway.GatewayProvider

class FellesLandingssideService(
    private val meldekortServiceGateway: MeldekortServiceGateway,
) {
    constructor(gatewayProvider: GatewayProvider) : this(
        meldekortServiceGateway = gatewayProvider.provide(),
    )

    fun hentFraArena(fnr: String): Meldekortstatus? {
        val alleMeldekort = meldekortServiceGateway.hentMeldekort(fnr) ?: return null
        val aapMeldekort = alleMeldekort.filter { it.erAAPMeldekort() }

        val harInnsendteMeldekort =
            aapMeldekort.any { it.mottattDato != null } || harInnsendteHistoriskeArenaMeldekort(fnr)

        if (!harInnsendteMeldekort && aapMeldekort.isEmpty()) return null

        return Meldekortstatus(
            harInnsendteMeldekort = harInnsendteMeldekort,
            meldekortTilUtfylling = aapMeldekort
                .filter { it.mottattDato == null }
                .sortedBy { it.kanSendesFra() }
                .map {
                    MeldekortTilUtfylling(
                        kanSendesFra = it.kanSendesFra().toLocalDate(),
                        fristForInnsending = null,
                        kanFyllesUtFra = it.kanSendesFra().toLocalDate(),
                    )
                },
        )
    }

    private fun harInnsendteHistoriskeArenaMeldekort(fnr: String): Boolean {
        return meldekortServiceGateway.hentHistoriskeMeldekort(fnr)
            ?.any { it.erAAPMeldekort() && it.mottattDato != null } == true
    }
}
