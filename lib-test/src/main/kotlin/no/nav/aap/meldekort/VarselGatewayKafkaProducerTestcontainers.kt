package no.nav.aap.meldekort

import no.nav.aap.KafkaProducerConfig
import no.nav.aap.varsel.VarselGatewayKafkaProducer
import no.nav.tms.varsel.builder.BuilderEnvironment

object VarselGatewayKafkaProducerTestcontainers : VarselGatewayKafkaProducer(KafkaProducerConfig(sslConfig = null)) {
    init {
        BuilderEnvironment.extend(
            mapOf(
                "NAIS_CLUSTER_NAME" to "lokalt",
                "NAIS_NAMESPACE" to "aap",
                "NAIS_APP_NAME" to "meldekort-backend"
            )
        )
    }
}
