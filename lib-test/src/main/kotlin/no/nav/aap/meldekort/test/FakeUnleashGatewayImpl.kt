package no.nav.aap.meldekort.test

import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.UnleashGateway

object FakeUnleashGatewayImpl : UnleashGateway {

    private val enabled: Boolean = false

    override fun isEnabled(featureToggle: FeatureToggle): Boolean = enabled

    override fun isEnabled(featureToggle: FeatureToggle, userId: String): Boolean = enabled
}