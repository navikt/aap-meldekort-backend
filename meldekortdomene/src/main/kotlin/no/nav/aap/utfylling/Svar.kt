package no.nav.aap.utfylling

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.aap.Periode
import java.time.LocalDate

data class Svar(
    val svarerDuSant: Boolean?,
    val harDuJobbet: Boolean?,
    @field:JsonAlias("timerArbeidet")
    val aktivitetsInformasjon: List<AktivitetsInformasjon>,
    val stemmerOpplysningene: Boolean?,
    val harDuGjennomførtAvtaltAktivitet: FraværSvar? = null,
) {
    companion object {
        fun tomt(periode: Periode): Svar {
            return Svar(
                svarerDuSant = null,
                harDuJobbet = null,
                aktivitetsInformasjon = periode.map { AktivitetsInformasjon(it, null, null) },
                stemmerOpplysningene = null,
                harDuGjennomførtAvtaltAktivitet = null
            )
        }
    }
}

data class  AktivitetsInformasjon(
    val dato: LocalDate,
    val timer: Double?,
    val fravær: Fravær? = null
)

enum class Fravær {
    SYKDOM_ELLER_SKADE,
    OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN,
    OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE,
    OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS,
    OMSORG_MEDDOMMER_ELLER_ANDRE_OFFENTLIGE_PLIKTER,
    OMSORG_ANNEN_STERK_GRUNN,
    ANNEN,
    ;
}

enum class FraværSvar {
    GJENNOMFØRT_AVTALT_AKTIVITET,
    NEI_IKKE_GJENNOMFORT_AVTALT_AKTIVITET,
    INGEN_AVTALTE_AKTIVITETER,
    ;
}


