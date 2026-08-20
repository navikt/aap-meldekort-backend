package no.nav.aap.meldekort.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.UnleashGateway

object UnleashGatewayImpl : UnleashGateway {
    private val unleash by lazy {
        DefaultUnleash(
            UnleashConfig
                .builder()
                .appName(requiredConfigForKey("nais.app.name"))
                .unleashAPI("${requiredConfigForKey("unleash.server.api.url")}/api")
                .apiKey(requiredConfigForKey("unleash.server.api.token"))
                .build()
        )
    }

    override fun isEnabled(featureToggle: FeatureToggle): Boolean {
        return unleash.isEnabled(featureToggle.key())
    }

    override fun isEnabled(featureToggle: FeatureToggle, userId: String): Boolean {
        return unleash.isEnabled(
            featureToggle.key(),
            UnleashContext.builder()
                .userId(userId)
                .build()
        )
    }
}
