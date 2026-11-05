package no.nav.aap.meldekort.saker

import no.nav.aap.Ident
import no.nav.aap.Periode
import no.nav.aap.arena.ArenaoppslagGateway
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.VedtakForPersonRequest
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.prometheus
import no.nav.aap.sak.FagsakReferanse
import no.nav.aap.sak.Fagsaknummer
import no.nav.aap.sak.FagsystemNavn
import no.nav.aap.sak.Sak
import no.nav.aap.sak.Saker
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate

object ArenaoppslagGatewayImpl : ArenaoppslagGateway {
    private val log = LoggerFactory.getLogger(javaClass)
    private val vedtakUri = URI("${requiredConfigForKey("integrasjon.arenaoppslag.url")}/api/v1/person/vedtak")
    private val httpClient = RestClient.withDefaultResponseHandler(
        ClientConfig(scope = requiredConfigForKey("integrasjon.arenaoppslag.scope")),
        tokenProvider = AzureM2MTokenProvider,
        prometheus = prometheus,
    )

    private data class ArenaSak(
        override val referanse: FagsakReferanse,
        override val rettighetsperiode: Periode,
    ) : Sak

    override fun hentSaker(ident: Ident): Saker {
        val vedtak = try {
            requireNotNull(
                httpClient.post<VedtakForPersonRequest, List<ArenaVedtak>>(
                    vedtakUri,
                    PostRequest(
                        body = VedtakForPersonRequest(ident.asString),
                        additionalHeaders = listOf(Header("accept", "application/json")),
                        timeout = Duration.ofSeconds(10),
                    ),
                )
            ) { "Arenaoppslag-vedtak mangler respons" }
        } catch (_: IkkeFunnetException) {
            // Endepunktet returnerer 404 når personen ikke finnes i Arena.
            log.info("Personen finnes ikke i Arena")
            return Saker(emptyList())
        }

        return Saker(vedtak.mapNotNull {
            if (it.rettighetkode != "AAP" || it.utfallkode != "JA" ||
                it.statusKode !in setOf("IVERK", "AVSLU") ||
                it.vedtaktypeKode !in setOf("O", "E", "G") ||
                it.aktivitetsfaseKode !in setOf("UA", "AU")
            ) {
                return@mapNotNull null
            }
            val fom = it.fraOgMed
            // TODO sjekk ut dette
            if (fom == null) {
                log.info("Ignorerer Arena-vedtak uten fraOgMed")
                return@mapNotNull null
            }
            val tom = it.tilDato ?: LocalDate.MAX
            // TODO sjekk ut dette
            if (tom < fom) {
                log.info("Ignorerer ugyldiggjort Arena-vedtak med tilDato før fraOgMed")
                return@mapNotNull null
            }
            ArenaSak(
                referanse = FagsakReferanse(FagsystemNavn.ARENA, Fagsaknummer(it.sakId.toString())),
                rettighetsperiode = Periode(fom, tom),
            )
        })
    }
}
