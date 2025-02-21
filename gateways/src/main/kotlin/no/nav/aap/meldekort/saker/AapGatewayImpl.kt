package no.nav.aap.meldekort.saker

import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.Sak
import no.nav.aap.sak.Saker
import no.nav.aap.sak.AapGateway
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

object AapGatewayImpl : AapGateway {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val aapApiUrl = requiredConfigForKey("aap.api.intern.url")

    private val httpClient = RestClient.withDefaultResponseHandler(
        ClientConfig(scope = requiredConfigForKey("aap.api.intern.scope")),
        tokenProvider = ClientCredentialsTokenProvider
    )

    private val sakerByFnrUrl = URI("$aapApiUrl/sakerByFnr")
    private val perioderMeldekortUrl = URI("$aapApiUrl/perioder/meldekort")

    /** Kontrakt? */
    class SakStatus(
        val kilde: String,
        val periode: Periode,
        val sakId: String,
        val vedtakStatusKode: VedtakStatusKode,
    ) {
        class Periode(
            val fraOgMedDato: String?,
            val tilOgMedDato: String?,
        )

        enum class VedtakStatusKode {
            AVSLU, FORDE, GODKJ, INNST, IVERK, KONT, MOTAT, OPPRE, REGIS, UKJENT
        }
    }

    override fun hentSaker(innloggetBruker: InnloggetBruker): Saker {
        val saker = httpClient.post<_, List<SakStatus>>(
            uri = sakerByFnrUrl,
            request = PostRequest(
                body = mapOf("personidentifikatorer" to listOf(innloggetBruker.ident.asString)),
                additionalHeaders = listOf(Header("accept", "application/json")),
            ),
        )!!
            .map {
                Sak(
                    referanse = FagsakReferanse(
                        system = when (it.kilde) {
                            "Kelvin" -> FagsystemNavn.KELVIN
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

    override fun hentMeldeperioder(innloggetBruker: InnloggetBruker, periode: Periode): List<Periode> {
        class KelvinPeriode(val fom: LocalDate, val tom: LocalDate)

        return httpClient.post<_, List<KelvinPeriode>>(
            uri = perioderMeldekortUrl,
            request = PostRequest(
                body = mapOf(
                    "personidentifikator" to innloggetBruker.ident.asString,
                    "fraOgMedDato" to periode.fom,
                    "tilOgMedDato" to periode.tom,
                ),
                additionalHeaders = listOf(Header("accept", "application/json")),
            ),
        )!!
            .map { Periode(it.fom, it.tom) }
    }
}