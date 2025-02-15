package no.nav.aap.meldekort.arena

import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.OnBehalfOfTokenProvider
import no.nav.aap.InnloggetBruker
import org.slf4j.MDC
import java.net.URI
import java.time.LocalDate
import java.util.*
import no.nav.aap.arena.ArenaGateway
import no.nav.aap.arena.ArenaMeldegruppe
import no.nav.aap.arena.ArenaMeldekort
import no.nav.aap.arena.ArenaMeldekortdetaljer
import no.nav.aap.arena.ArenaMeldekortkontrollRequest
import no.nav.aap.arena.ArenaPerson
import no.nav.aap.arena.MeldekortId
import no.nav.aap.arena.MeldekortkontrollResponse
import no.nav.aap.komponenter.config.requiredConfigForKey

object ArenaGatewayImpl : ArenaGateway {
    private val meldekortserviceScope = requiredConfigForKey("meldekortservice.scope")
    private val meldekortkontrollScope = requiredConfigForKey("meldekortkontroll.scope")
    private val meldekortserviceUrl = requiredConfigForKey("meldekortservice.url")
    private val meldekortkontrollUrl = requiredConfigForKey("meldekortkontroll.url")

    private val meldekortserviceClient = RestClient.withDefaultResponseHandler(
        ClientConfig(scope = meldekortserviceScope),
        tokenProvider = OnBehalfOfTokenProvider()
    )
    private val meldekortkontrollClient = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = meldekortkontrollScope),
        tokenProvider = OnBehalfOfTokenProvider()
    )

    override fun meldegrupper(innloggetBruker: InnloggetBruker): List<ArenaMeldegruppe> {
        return requireNotNull(getMeldekortservice<List<MeldegruppeDto>>("/v2/meldegrupper", innloggetBruker))
            .map { it.tilDomene() }
    }

    override fun person(innloggetBruker: InnloggetBruker): ArenaPerson? {
        return getMeldekortservice<PersonDto?>("/v2/meldekort", innloggetBruker)
            ?.tilDomene()
    }

    override fun historiskeMeldekort(innloggetBruker: InnloggetBruker, antallMeldeperioder: Int): ArenaPerson {
        return requireNotNull(
            getMeldekortservice<PersonDto>(
                "/v2/historiskemeldekort?antallMeldeperioder=$antallMeldeperioder",
                innloggetBruker
            )
        )
            .tilDomene()
    }

    override fun meldekortdetaljer(innloggetBruker: InnloggetBruker, meldekortId: MeldekortId): ArenaMeldekortdetaljer {
        return requireNotNull(
            getMeldekortservice<MeldekortdetaljerDto>(
                "/v2/meldekortdetaljer?meldekortId=${meldekortId.asLong}",
                innloggetBruker
            )
        )
            .tilDomene()
    }

    override fun korrigertMeldekort(innloggetBruker: InnloggetBruker, meldekortId: MeldekortId): MeldekortId {
        return MeldekortId(requireNotNull(getMeldekortservice("/v2/korrigertMeldekort?meldekortId=${meldekortId.asLong}", innloggetBruker)))
    }

    override fun sendInn(
        innloggetBruker: InnloggetBruker,
        request: ArenaMeldekortkontrollRequest
    ): MeldekortkontrollResponse {
        return requireNotNull(
            meldekortkontrollClient.post<_, MeldekortkontrollResponseDto>(
                URI("$meldekortkontrollUrl/api/v1/kontroll"), PostRequest(
                    body = MeldekortkontrollRequestDto(request),
                    currentToken = OidcToken(innloggetBruker.token),
                    additionalHeaders = listOf(
                        Header("accept", "application/json"),
                        Header("x-request-id", MDC.get("callId") ?: "aap-meldekort-${UUID.randomUUID()}"),
                    )
                )
            )
        ).tilDomene()
    }

    private inline fun <reified Response> getMeldekortservice(
        path: String,
        innloggetBruker: InnloggetBruker
    ): Response? {
        return meldekortserviceClient.get(URI(meldekortserviceUrl + path), getRequest(innloggetBruker))
    }

    private fun getRequest(innloggetBruker: InnloggetBruker): GetRequest {
        return GetRequest(
            currentToken = OidcToken(innloggetBruker.token),
            additionalHeaders = listOf(
                Header("ident", innloggetBruker.ident.asString),
                Header("accept", "application/json"),
                Header("x-request-id", MDC.get("callId") ?: "aap-meldekort-${UUID.randomUUID()}")
            ),
        )
    }

    data class MeldegruppeDto(
        val fodselsnr: String,
        val meldegruppeKode: String,
        val datoFra: LocalDate,
        val datoTil: LocalDate? = null,
        val hendelsesdato: LocalDate,
        val statusAktiv: String,
        val begrunnelse: String,
        val styrendeVedtakId: Long? = null
    ) {
        fun tilDomene() = ArenaMeldegruppe(
            fodselsnr = fodselsnr,
            meldegruppeKode = meldegruppeKode,
            datoFra = datoFra,
            datoTil = datoTil,
            hendelsesdato = hendelsesdato,
            statusAktiv = statusAktiv,
            begrunnelse = begrunnelse,
            styrendeVedtakId = styrendeVedtakId,
        )
    }

    data class PersonDto(
        val personId: Long,
        val etternavn: String,
        val fornavn: String,
        val maalformkode: String,
        val meldeform: String,
        val meldekortListe: List<MeldekortDto>? = null,
    ) {
        fun tilDomene() = ArenaPerson(
            personId = personId,
            etternavn = etternavn,
            fornavn = fornavn,
            maalformkode = maalformkode,
            meldeform = meldeform,
            arenaMeldekortListe = meldekortListe.orEmpty().map { it.tilDomene() },
        )
    }

    data class MeldekortDto(
        val meldekortId: Long,

        val kortType: String,
        val meldeperiode: String,
        val fraDato: LocalDate,
        val tilDato: LocalDate,
        val hoyesteMeldegruppe: String,

        val beregningstatus: String,
        val forskudd: Boolean,
        val mottattDato: LocalDate? = null,
        val bruttoBelop: Double = 0.0
    ) {
        fun tilDomene() = ArenaMeldekort(
            meldekortId = MeldekortId(meldekortId),
            kortType = ArenaGateway.KortType.getByCode(kortType),
            meldeperiode = meldeperiode,
            fraDato = fraDato,
            tilDato = tilDato,
            hoyesteMeldegruppe = hoyesteMeldegruppe,
            beregningstatus = ArenaMeldekort.ArenaStatus.valueOf(beregningstatus),
            forskudd = forskudd,
            mottattDato = mottattDato,
            bruttoBelop = bruttoBelop,
        )
    }

    data class MeldekortdetaljerDto(
        val id: String,
        val personId: Long,
        val fodselsnr: String,
        val meldekortId: Long,
        val meldeperiode: String,
        val meldegruppe: String,
        val arkivnokkel: String,
        val kortType: String,
        val meldeDato: LocalDate? = null,
        val lestDato: LocalDate? = null,
        val sporsmal: SporsmalDto? = null,
        val begrunnelse: String? = ""
    ) {
        fun tilDomene() = ArenaMeldekortdetaljer(
            id = id,
            personId = personId,
            fodselsnr = fodselsnr,
            meldekortId = MeldekortId(meldekortId),
            meldeperiode = meldeperiode,
            meldegruppe = meldegruppe,
            arkivnokkel = arkivnokkel,
            kortType = ArenaGateway.KortType.valueOf(kortType),
            meldeDato = meldeDato,
            lestDato = lestDato,
            sporsmal = sporsmal?.tilDomene(),
            begrunnelse = begrunnelse,
        )
    }

    data class SporsmalDto(
        val arbeidssoker: Boolean? = null,
        val arbeidet: Boolean? = null,
        val syk: Boolean? = null,
        val annetFravaer: Boolean? = null,
        val kurs: Boolean? = null,
        val forskudd: Boolean? = null,
        val signatur: Boolean? = null,
        val meldekortDager: List<MeldekortDagDto>? = null
    ) {
        fun tilDomene() = ArenaMeldekortdetaljer.Sporsmal(
            arbeidssoker = arbeidssoker,
            arbeidet = arbeidet,
            syk = syk,
            annetFravaer = annetFravaer,
            kurs = kurs,
            forskudd = forskudd,
            signatur = signatur,
            meldekortDager = meldekortDager?.map { it.tilDomene() },
        )
    }

    data class MeldekortDagDto(
        val dag: Int = 0,
        val arbeidetTimerSum: Double? = null,
        val syk: Boolean? = null,
        val annetFravaer: Boolean? = null,
        val kurs: Boolean? = null,
        val meldegruppe: String? = null
    ) {
        fun tilDomene() = ArenaMeldekortdetaljer.MeldekortDag(
            dag = dag,
            arbeidetTimerSum = arbeidetTimerSum,
            syk = syk,
            annetFravaer = annetFravaer,
            kurs = kurs,
            meldegruppe = meldegruppe,
        )
    }

    data class MeldekortkontrollRequestDto(
        val meldekortId: Long,
        val fnr: String,
        val personId: Long,
        val kortType: String,
        val meldedato: LocalDate,
        val periodeFra: LocalDate,
        val periodeTil: LocalDate,
        val meldegruppe: String,
        val begrunnelse: String?,
        val meldekortdager: List<MeldekortkontrollFravaerDto>,
        val arbeidet: Boolean,
        val kilde: String,
        val arbeidssoker: Boolean = false,
        val annetFravaer: Boolean = false,
        val kurs: Boolean = false,
        val syk: Boolean = false,
    ) {
        constructor(domene: ArenaMeldekortkontrollRequest) : this(
            meldekortId = domene.meldekortId.asLong,
            fnr = domene.fnr,
            personId = domene.personId,
            kortType = domene.kortType.code,
            meldedato = domene.meldedato,
            periodeFra = domene.periodeFra,
            periodeTil = domene.periodeTil,
            meldegruppe = domene.meldegruppe,
            begrunnelse = domene.begrunnelse,
            meldekortdager = domene.meldekortdager.map { MeldekortkontrollFravaerDto(it) },
            arbeidet = domene.arbeidet,
            kilde = domene.kilde,
            arbeidssoker = domene.arbeidssoker,
            annetFravaer = domene.annetFravaer,
            kurs = domene.kurs,
            syk = domene.syk,
        )
    }

    data class MeldekortkontrollFravaerDto(
        val dato: LocalDate,
        val arbeidTimer: Double,
        val syk: Boolean = false,
        val kurs: Boolean = false,
        val annetFravaer: Boolean = false,
    ) {
        constructor(domene: ArenaMeldekortkontrollRequest.MeldekortkontrollFravaer) : this(
            dato = domene.dato,
            arbeidTimer = domene.arbeidTimer,
            syk = domene.syk,
            kurs = domene.kurs,
            annetFravaer = domene.annetFravaer,
        )
    }

    data class MeldekortkontrollResponseDto(
        var meldekortId: Long = 0,
        var kontrollStatus: String = "",
        var feilListe: List<MeldekortkontrollFeilDto> = emptyList(),
        var oppfolgingListe: List<MeldekortkontrollFeilDto> = emptyList()
    ) {
        fun tilDomene() = MeldekortkontrollResponse(
            meldekortId = meldekortId,
            kontrollStatus = kontrollStatus,
            feilListe = feilListe.map { it.tilDomene() },
            oppfolgingListe = oppfolgingListe.map { it.tilDomene()},
        )
    }

    data class MeldekortkontrollFeilDto(
        var kode: String,
        var params: List<String>? = null
    ) {
        fun tilDomene() = MeldekortkontrollResponse.MeldekortkontrollFeil(
            kode = kode,
            params = params,
        )
    }
}