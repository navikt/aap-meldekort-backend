package no.nav.aap.meldekort.saker

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.retryablePost
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.prometheus
import no.nav.aap.sak.AapGateway
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.Sak
import no.nav.aap.sak.Saker
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

object AapGatewayImpl : AapGateway {
    private class GatewaySak(
        override val referanse: FagsakReferanse,
        override val rettighetsperiode: Periode
    ) : Sak

    private val log = LoggerFactory.getLogger(this::class.java)

    private val aapApiUrl = requiredConfigForKey("aap.api.intern.url")

    private val httpClient = RestClient.withDefaultResponseHandler(
        ClientConfig(scope = requiredConfigForKey("aap.api.intern.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    private val sakerByFnrUrl = URI("$aapApiUrl/sakerByFnr")
    private val perioderMeldekortUrl = URI("$aapApiUrl/perioder/meldekort")

    /** Kontrakt? */
    class SakStatus(
        val kilde: String,
        val periode: Periode,
        val sakId: String,
    ) {
        class Periode(
            val fraOgMedDato: String?,
            val tilOgMedDato: String?,
        )
    }

    override fun hentSaker(ident: Ident): Saker {
        val saker = httpClient.retryablePost<_, List<SakStatus>>(
            uri = sakerByFnrUrl,
            request = PostRequest(
                body = mapOf("personidentifikatorer" to listOf(ident.asString)),
                additionalHeaders = listOf(Header("accept", "application/json")),
            ),
        )!!
            .map {
                GatewaySak(
                    referanse = FagsakReferanse(
                        system = when (it.kilde) {
                            "KELVIN" -> FagsystemNavn.KELVIN
                            "ARENA" -> FagsystemNavn.ARENA
                            else -> error("ukjent fagsystem ${it.kilde}")
                        },
                        nummer = Fagsaknummer(it.sakId),
                    ),
                    rettighetsperiode = Periode(
                        fom = it.periode.fraOgMedDato?.let(LocalDate::parse)
                            ?: LocalDate.MIN.also {
                                log.info("fraOgMedDato blir ikke satt. vurder å eksponer nullable")
                            },
                        tom = it.periode.tilOgMedDato?.let(LocalDate::parse)
                            ?: LocalDate.MAX.also {
                                log.info("tilOgMedDato blir ikke satt. vurder a eksponer nullable")
                            },
                    ),
                )
            }

        return Saker(saker)
    }

    override fun hentMeldeperioder(ident: Ident, periode: Periode): List<Periode> {
        class KelvinPeriode(val fom: LocalDate, val tom: LocalDate)

        return httpClient.retryablePost<_, List<KelvinPeriode>>(
            uri = perioderMeldekortUrl,
            request = PostRequest(
                body = mapOf(
                    "personidentifikator" to ident.asString,
                    "fraOgMedDato" to periode.fom,
                    "tilOgMedDato" to periode.tom,
                ),
                additionalHeaders = listOf(Header("accept", "application/json")),
            ),
        )!!
            .map { Periode(it.fom, it.tom) }
    }
}