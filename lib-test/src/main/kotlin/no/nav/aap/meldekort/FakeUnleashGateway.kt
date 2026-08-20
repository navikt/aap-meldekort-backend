package no.nav.aap.meldekort

import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.UnleashGateway

object FakeUnleashGateway : UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle): Boolean = false
    override fun isEnabled(featureToggle: FeatureToggle, userId: String): Boolean = false
}
