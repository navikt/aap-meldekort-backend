package no.nav.aap.flyt

enum class StegNavn {

}

sealed interface Steg {
    val stegNavn: StegNavn

    /** Gitt tidligere spørsmål, er dette steget relevant? */
    fun erRelevant(): Boolean {
        return true
    }

    fun valider(): Boolean

}