package no.nav.aap.meldekort
import com.fasterxml.jackson.annotation.JsonCreator
import no.nav.aap.utfylling.Fravær
import no.nav.aap.utfylling.FraværSvar
import no.nav.aap.utfylling.Svar
import no.nav.aap.utfylling.TimerArbeidet
import no.nav.aap.utfylling.Utfylling
import no.nav.aap.utfylling.UtfyllingFlate
import no.nav.aap.utfylling.UtfyllingStegNavn
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class StartUtfyllingRequest(
    val fom: LocalDate,
    val tom: LocalDate,
)

class StartUtfyllingResponse(
    val metadata: UtfyllingMetadataDto?,
    val tilstand: UtfyllingTilstandDto?,
    val feil: String?,
) {
    companion object {
        fun fraDomene(domene: UtfyllingFlate.StartUtfyllingResponse): StartUtfyllingResponse {
            val utfylling = domene.utfylling
            val metadata = domene.metadata
            if (utfylling == null || metadata == null) {
                return StartUtfyllingResponse(
                    metadata = null,
                    tilstand = null,
                    feil = domene.feil ?: "ukjent feil",
                )
            }

            return StartUtfyllingResponse(
                metadata = UtfyllingMetadataDto.fraDomene(utfylling, metadata),
                tilstand = UtfyllingTilstandDto(utfylling),
                feil = domene.feil,
            )
        }
    }
}

class EndreUtfyllingRequest @JsonCreator constructor(
    val nyTilstand: UtfyllingTilstandDto,
)

class UtfyllingResponseDto(
    val metadata: UtfyllingMetadataDto,
    val tilstand: UtfyllingTilstandDto,
    val feil: String?
) {
    companion object {
        fun fraDomene(domene: UtfyllingFlate.UtfyllingResponse): UtfyllingResponseDto {
            return UtfyllingResponseDto(
                metadata = UtfyllingMetadataDto.fraDomene(domene.utfylling, domene.metadata),
                tilstand = UtfyllingTilstandDto(domene.utfylling),
                feil = domene.feil,
            )
        }
    }
}

class UtfyllingTilstandDto(
    val aktivtSteg: StegDto,
    val svar: SvarDto,
) {
    constructor(utfylling: Utfylling) : this(
        aktivtSteg = StegDto.fraDomene(utfylling.aktivtSteg),
        svar = SvarDto(utfylling.svar)
    )
}

class UtfyllingMetadataDto(
    val harBrukerVedtakIKelvin: Boolean? = null,
    val harBrukerSakUnderBehandling: Boolean? = null,
    val referanse: UUID,
    val periode: PeriodeDto,
    val antallUbesvarteMeldeperioder: Int,

    /** Hvis `null` skal det tolkes som at det kan sendes inn når som helst.
     * Foreløpig er den alltid satt, siden logikken ikke er implementert skikkelig
     * enda.
     */
    val tidligsteInnsendingstidspunkt: LocalDateTime?,

    /** Hvis `null` skal det tolkes som at det ikke er en bestemt frist som i seg selv
     * påvirker ytelsen.
     * Settes til `null` når perioden man ser på ikke har vært en del av en periode med meldeplikt
     */
    val fristForInnsending: LocalDateTime?,
    val kanSendesInn: Boolean,
    val visFrist: Boolean
) {
    companion object {
        fun fraDomene(utfylling: Utfylling, metadata: UtfyllingFlate.Metadata): UtfyllingMetadataDto {
            return UtfyllingMetadataDto(
                referanse = utfylling.referanse.asUuid,
                periode = PeriodeDto(utfylling.periode),
                tidligsteInnsendingstidspunkt = metadata.tidligsteInnsendingstidspunkt,
                fristForInnsending = metadata.fristForInnsending,
                kanSendesInn = metadata.kanSendesInn,
                antallUbesvarteMeldeperioder = metadata.antallUbesvarteMeldeperioder,
                harBrukerVedtakIKelvin = metadata.brukerHarVedtakIKelvin,
                harBrukerSakUnderBehandling = metadata.brukerHarSakUnderBehandling,
                visFrist = metadata.visFrist
            )
        }

    }
}

enum class StegDto(val tilDomene: UtfyllingStegNavn) {
    INTRODUKSJON(UtfyllingStegNavn.INTRODUKSJON),
    SPØRSMÅL(UtfyllingStegNavn.SPØRSMÅL),
    UTFYLLING(UtfyllingStegNavn.UTFYLLING),
    FRAVÆR_SPØRSMÅL(UtfyllingStegNavn.FRAVÆR_SPØRSMÅL),
    FRAVÆR_UTFYLLING(UtfyllingStegNavn.FRAVÆR_UTFYLLING),
    BEKREFT(UtfyllingStegNavn.BEKREFT),
    KVITTERING(UtfyllingStegNavn.KVITTERING),
    ;

    companion object {
        fun fraDomene(navn: UtfyllingStegNavn): StegDto {
            return when (navn) {
                UtfyllingStegNavn.INTRODUKSJON -> INTRODUKSJON
                UtfyllingStegNavn.SPØRSMÅL -> SPØRSMÅL
                UtfyllingStegNavn.UTFYLLING -> UTFYLLING
                UtfyllingStegNavn.FRAVÆR_SPØRSMÅL -> FRAVÆR_SPØRSMÅL
                UtfyllingStegNavn.FRAVÆR_UTFYLLING -> FRAVÆR_UTFYLLING
                UtfyllingStegNavn.BEKREFT -> BEKREFT
                UtfyllingStegNavn.KVITTERING -> KVITTERING

                UtfyllingStegNavn.PERSISTER_OPPLYSNINGER,
                UtfyllingStegNavn.BESTILL_JOURNALFØRING,
                UtfyllingStegNavn.INAKTIVER_VARSEL -> error("skal ikke stoppe i teknisk steg")
            }
        }
    }
}

class SvarDto(
    val vilSvareRiktig: Boolean?,
    val harDuJobbet: Boolean?,
    val dager: List<DagSvarDto>,
    val stemmerOpplysningene: Boolean?,
    val harDuGjennomførtAvtaltAktivitet: FraværSvarDto? = null,
) {
    fun tilDomene(): Svar {
        return Svar(
            svarerDuSant = vilSvareRiktig,
            harDuJobbet = harDuJobbet,
            timerArbeidet = dager.map { it.tilTimerArbeidet() },
            stemmerOpplysningene = stemmerOpplysningene,
            harDuGjennomførtAvtaltAktivitet = harDuGjennomførtAvtaltAktivitet?.tilDomene
        )
    }

    constructor(svar: Svar) : this(
        vilSvareRiktig = svar.svarerDuSant,
        harDuJobbet = svar.harDuJobbet,
        dager = svar.timerArbeidet.map { DagSvarDto(it) },
        stemmerOpplysningene = svar.stemmerOpplysningene,
        harDuGjennomførtAvtaltAktivitet = svar.harDuGjennomførtAvtaltAktivitet?.let { FraværSvarDto.fraDomene(it) }
    )
}

class DagSvarDto(
    val dato: LocalDate,
    val timerArbeidet: Double?,
    val fravær: FraværDto? = null,
) {
    fun tilTimerArbeidet(): TimerArbeidet {
        return TimerArbeidet(
            timer = timerArbeidet,
            dato = dato,
            fravær = fravær?.tilDomene
        )
    }

    constructor(timerArbeidet: TimerArbeidet) : this(
        dato = timerArbeidet.dato,
        timerArbeidet = timerArbeidet.timer,
        fravær = timerArbeidet.fravær?.let { FraværDto.fraDomene(it) }
    )
}

enum class FraværDto(val tilDomene: Fravær) {
    SYKDOM_ELLER_SKADE(Fravær.SYKDOM_ELLER_SKADE),
    OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN(Fravær.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN),
    OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE(Fravær.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE),
    OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS(Fravær.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS),
    OMSORG_MEDDOMMER_ELLER_ANDRE_OFFENTLIGE_PLIKTER(Fravær.OMSORG_MEDDOMMER_ELLER_ANDRE_OFFENTLIGE_PLIKTER),
    OMSORG_ANNEN_STERK_GRUNN(Fravær.OMSORG_ANNEN_STERK_GRUNN),
    ANNEN(Fravær.ANNEN),
    ;

    companion object{
        fun fraDomene(fravær: Fravær): FraværDto {
            return when (fravær) {
                Fravær.SYKDOM_ELLER_SKADE -> SYKDOM_ELLER_SKADE
                Fravær.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN -> OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN
                Fravær.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE -> OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE
                Fravær.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS -> OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS
                Fravær.OMSORG_MEDDOMMER_ELLER_ANDRE_OFFENTLIGE_PLIKTER -> OMSORG_MEDDOMMER_ELLER_ANDRE_OFFENTLIGE_PLIKTER
                Fravær.OMSORG_ANNEN_STERK_GRUNN -> OMSORG_ANNEN_STERK_GRUNN
                Fravær.ANNEN -> ANNEN
            }
        }
    }
}

enum class FraværSvarDto(val tilDomene: FraværSvar) {
    GJENNOMFØRT_AVTALT_AKTIVITET(FraværSvar.GJENNOMFØRT_AVTALT_AKTIVITET),
    NEI_IKKE_GJENNOMFORT_AVTALT_AKTIVITET(FraværSvar.NEI_IKKE_GJENNOMFORT_AVTALT_AKTIVITET),
    INGEN_AVTALTE_AKTIVITETER(FraværSvar.INGEN_AVTALTE_AKTIVITETER),
    ;

    companion object {
        fun fraDomene(fraværSvar: FraværSvar): FraværSvarDto {
            return when (fraværSvar) {
                FraværSvar.GJENNOMFØRT_AVTALT_AKTIVITET -> GJENNOMFØRT_AVTALT_AKTIVITET
                FraværSvar.NEI_IKKE_GJENNOMFORT_AVTALT_AKTIVITET -> NEI_IKKE_GJENNOMFORT_AVTALT_AKTIVITET
                FraværSvar.INGEN_AVTALTE_AKTIVITETER -> INGEN_AVTALTE_AKTIVITETER
            }
        }
    }
}

