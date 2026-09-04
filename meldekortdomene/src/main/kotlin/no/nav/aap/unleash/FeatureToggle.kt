package no.nav.aap.unleash

interface FeatureToggle {
    fun key(): String
}

enum class MeldekortFeature : FeatureToggle {
    // Eksempel på feature toggle. Kan fjernes når det legges til nye.
    // Se: https://aap-unleash-web.iap.nav.cloud.nais.io/projects/default
    MeldekortBackendTest,
    MeldekortKvitteringFraNyPdfgenerator,
    ;

    override fun key(): String = name
}
