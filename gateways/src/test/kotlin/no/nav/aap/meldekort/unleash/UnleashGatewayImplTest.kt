package no.nav.aap.meldekort.unleash

import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.unleash.UnleashGateway
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class UnleashGatewayImplTest {
    @Test
    fun `gateway kan registreres og hentes`() {
        GatewayRegistry.register<UnleashGatewayImpl>()

        val gateway = GatewayProvider.provide<UnleashGateway>()

        assertSame(UnleashGatewayImpl, gateway)
    }
}
