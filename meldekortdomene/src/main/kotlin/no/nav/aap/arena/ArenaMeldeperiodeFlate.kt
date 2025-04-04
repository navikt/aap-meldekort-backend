package no.nav.aap.arena

import no.nav.aap.Ident
import no.nav.aap.InnloggetBruker
import no.nav.aap.Periode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.meldeperiode.MeldeperiodeFlate
import no.nav.aap.utfylling.Svar

class ArenaMeldeperiodeFlate() : MeldeperiodeFlate {
    override fun aktuelleMeldeperioder(innloggetBruker: InnloggetBruker): MeldeperiodeFlate.KommendeMeldeperioder {
        return MeldeperiodeFlate.KommendeMeldeperioder(
            antallUbesvarteMeldeperioder = 0,
            manglerOpplysninger = null,
            nesteMeldeperiode = null,
        )
//        val ventendeOgNeste = sakService?.ventendeOgNesteMeldeperioder(innloggetBruker)
//            ?: SakService.VentendeOgNeste(
//                ventende = listOf(),
//                neste = null,
//            )
//
//        val manglerOpplysninger = ventendeOgNeste.ventende
//            .takeIf { it.isNotEmpty() }
//            ?.let {
//                Periode(
//                    fom = it.first().meldeperioden.fom,
//                    tom = it.last().meldeperioden.tom,
//                )
//            }
//        return MeldeperiodeFlate.KommendeMeldeperioder(
//            antallUbesvarteMeldeperioder = ventendeOgNeste.ventende.size,
//            manglerOpplysninger = manglerOpplysninger,
//            nesteMeldeperiode = ventendeOgNeste.ventende.firstOrNull() ?: ventendeOgNeste.neste
//        )
    }

    override fun historiskeMeldeperioder(innloggetBruker: InnloggetBruker): List<MeldeperiodeFlate.HistoriskMeldeperiode> {
        return listOf()
//        return sakService?.historiskeMeldeperioder(innloggetBruker).orEmpty()
//            .map {
//                MeldeperiodeFlate.HistoriskMeldeperiode(
//                    meldeperiode = it,
//                    totaltAntallTimerIPerioden = totaltAntallTimerIPerioden(innloggetBruker, it.meldeperioden) ?: 0.0
//                )
//            }
    }

    override fun periodedetaljer(innloggetBruker: InnloggetBruker, periode: Periode): MeldeperiodeFlate.PeriodeDetaljer {
        return MeldeperiodeFlate.PeriodeDetaljer(
            periode = periode,
            svar = Svar.tomt(periode),
        )
//        return sakService?.detaljer(innloggetBruker, periode)
//            ?: MeldeperiodeFlate.PeriodeDetaljer(
//                periode = periode,
//                svar = Svar.tomt(periode),
//            )
    }

    private fun totaltAntallTimerIPerioden(innloggetBruker: InnloggetBruker, periode: Periode): Double? {
        return null
//        val detaljer = sakService?.detaljer(innloggetBruker, periode)
//        if (detaljer !== null) {
//            return sakService?.totaltAntallTimerArbeidet(detaljer)
//        }
//        return null
    }

    companion object {
        fun konstruer(connection: DBConnection): ArenaMeldeperiodeFlate {
            return ArenaMeldeperiodeFlate()
        }
    }
}
