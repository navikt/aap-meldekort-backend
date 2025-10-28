package no.nav.aap.meldekort

import java.util.*

object ApplikasjonsVersjon {

    val versjon: String

    init {
        val file = this::class.java.classLoader.getResourceAsStream("meldekort-backend-version.properties")
        val properties = Properties()
        properties.load(file)
        versjon = properties.getProperty("project.version")
    }
}