package no.nav.aap

import no.nav.aap.komponenter.config.requiredConfigForKey
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

data class KafkaProducerConfig(
    val clientId: String = requiredConfigForKey("NAIS_APP_NAME"),
    val brokers: String = requiredConfigForKey("KAFKA_BROKERS"),
    val sslConfig: KafkaSslConfig? = KafkaSslConfig(),
) {
    fun properties(): Properties {
        return Properties().apply {
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.CLIENT_ID_CONFIG] = clientId
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            this[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers

            sslConfig?.let { putAll(it.properties()) }
        }
    }
}

data class KafkaSslConfig(
    val truststorePath: String = requiredConfigForKey("KAFKA_TRUSTSTORE_PATH"),
    val keystorePath: String = requiredConfigForKey("KAFKA_KEYSTORE_PATH"),
    val credstorePassword: String = requiredConfigForKey("KAFKA_CREDSTORE_PASSWORD")
) {
    fun properties(): Properties {
        return Properties().apply {
            this[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
            this[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
            this[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath
            this[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePassword
            this[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePassword
            this[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
            this[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = truststorePath
            this[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePassword
        }
    }
}