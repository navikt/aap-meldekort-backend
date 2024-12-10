package no.nav.aap.meldekort.arena

class Flyt private constructor(
    private val steg: List<Steg>
) {

    constructor(vararg steg: Steg) : this(listOf(*steg))

    fun stegForNavn(navn: StegNavn): Steg {
        return requireNotNull(steg.find { it.navn == navn }) {
            "steg $navn finnes ikke i flyt $this"
        }
    }

    override fun toString(): String {
        return steg.joinToString(prefix = "(", postfix = ")") { steg -> steg.navn.toString() }
    }
}