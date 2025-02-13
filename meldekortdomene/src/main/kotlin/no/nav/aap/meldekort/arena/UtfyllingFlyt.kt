package no.nav.aap.meldekort.arena

import no.nav.aap.meldekort.Ident
import no.nav.aap.meldekort.InnloggetBruker
import no.nav.aap.meldekort.arena.SkjemaTilstand.UTKAST

interface Steg {
    val navn: StegNavn
    fun kjør(innloggetBruker: InnloggetBruker, skjema: Skjema): Stegutfall

    fun skalKjøres(skjema: Skjema): Boolean {
        return true
    }

    fun erTekniskSteg(): Boolean {
        return false
    }
}

sealed interface Stegutfall {
    class Feil(val feil: Exception): Stegutfall
    class Fortsett(val skjema: Skjema): Stegutfall
    class Avklaringspunkt(val skjema: Skjema): Stegutfall
}

class UtfyllingFlyt(
    val utfyllingRepository: UtfyllingRepository,
    val stegene: List<Steg>,
) {
    fun kjør(innloggetBruker: InnloggetBruker, utfylling: Utfylling): Result<Utfylling> {
        fun ferdig(skjema: Skjema, steg: StegNavn): Result<Utfylling> {
            val nyUtfylling = utfylling.medSteg(steg).copy(skjema = skjema)
            utfyllingRepository.lagrUtfylling(nyUtfylling)
            return Result.success(nyUtfylling)
        }

        var skjema = utfylling.skjema
        val utførteSteg = mutableSetOf<StegNavn>()

        for (steg in stegene) {
            if (!steg.skalKjøres(skjema)) {
                continue
            }

            when (val utfall = steg.kjør(innloggetBruker, skjema)) {
                is Stegutfall.Fortsett -> {
                    skjema = utfall.skjema
                }

                is Stegutfall.Avklaringspunkt -> {
                    return ferdig(utfall.skjema, steg.navn)
                }

                is Stegutfall.Feil -> {
                    return Result.failure(utfall.feil)
                }
            }

            if (utfylling.steg.navn in utførteSteg && !steg.erTekniskSteg()) {
                return ferdig(skjema, steg.navn)
            }

            utførteSteg.add(steg.navn)
        }

        return ferdig(skjema, stegene.last().navn)
    }

    fun stegForNavn(navn: StegNavn): Steg {
        return requireNotNull(stegene.find { it.navn == navn }) {
            "steg $navn finnes ikke i flyt $this"
        }
    }

    override fun toString(): String {
        return stegene.joinToString(prefix = "(", postfix = ")") { steg -> steg.navn.toString() }
    }
}

data class Utfylling(
    val ident: Ident,
    val meldekortId: MeldekortId,
    val flyt: UtfyllingFlyt,
    val steg: Steg,
    val skjema: Skjema,
) {
    init {
        check(ident == skjema.ident)
        check(meldekortId == skjema.meldekortId)
    }

    constructor(flyt: UtfyllingFlyt, steg: Steg, skjema: Skjema) : this(
        ident = skjema.ident,
        meldekortId = skjema.meldekortId,
        flyt = flyt,
        steg = steg,
        skjema = skjema,
    )

    fun nyPayload(payload: InnsendingPayload): Utfylling {
        return copy(skjema = skjema.copy(payload = payload))
    }

    fun medSteg(steg: StegNavn): Utfylling {
        return copy(steg = flyt.stegForNavn(steg))
    }

    fun nesteSteg(innloggetBruker: InnloggetBruker): Result<Utfylling> {
        require(steg in flyt.stegene) { "steg $steg er ikke i flyt" }
        require(skjema.tilstand == UTKAST)
        return flyt.kjør(innloggetBruker, this)
    }

    fun validerUtkast() {
        check(skjema.tilstand == UTKAST)
        // TODO: kast exception hvis validering feil?
    }
}

