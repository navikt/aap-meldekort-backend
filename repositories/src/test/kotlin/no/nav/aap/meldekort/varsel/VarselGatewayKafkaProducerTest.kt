package no.nav.aap.meldekort.varsel

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.meldekort.VarselGatewayKafkaProducerTestcontainers
import no.nav.aap.meldekort.fødselsnummerGenerator
import no.nav.aap.varsel.TEKSTER_OPPGAVE_MELDEPLIKTPERIODE
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.OpprettVarsel
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.BuilderEnvironment
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class VarselGatewayKafkaProducerTest {

    companion object {
        val kafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka-native:4.0.0"))
            .withReuse(true)

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            kafkaContainer.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            kafkaContainer.stop()
        }
    }

    @Test
    fun `sender varsel på topic`() {
        BuilderEnvironment.extend(
            mapOf(
                "NAIS_CLUSTER_NAME" to "lokalt",
                "NAIS_NAMESPACE" to "aap",
                "NAIS_APP_NAME" to "meldekort-backend"
            )
        )
        val topic = "brukervarsel-topic"
        System.setProperty("brukervarsel.topic", topic)
        System.setProperty("KAFKA_BROKERS", kafkaContainer.bootstrapServers)

        val brukerIdent = fødselsnummerGenerator.next()
        val varsel = byggVarsel()

        VarselGatewayKafkaProducerTestcontainers.sendVarsel(brukerIdent, varsel, TEKSTER_OPPGAVE_MELDEPLIKTPERIODE, "https://www.nav.no/aap/meldekort")

        val consumer = KafkaConsumer<String, String>(kafkaTestConsumerProperties(kafkaContainer.bootstrapServers))
        consumer.subscribe(listOf(topic))
        val records = consumer.poll(10.seconds.toJavaDuration())

        assertThat(records).hasSize(1)
        val record = records.first()

        assertThat(record.key()).isEqualTo(varsel.varselId.toString())

        val varselMelding = DefaultJsonMapper.fromJson<OpprettVarsel>(record.value())
        assertThat(varselMelding.type).isEqualTo(Varseltype.Oppgave)
        assertThat(varselMelding.varselId).isEqualTo(varsel.varselId.toString())
        assertThat(varselMelding.sensitivitet).isEqualTo(Sensitivitet.High)
        assertThat(varselMelding.ident).isEqualTo(brukerIdent.asString)
        assertThat(varselMelding.link).isEqualTo("https://www.nav.no/aap/meldekort")
        assertThat(varselMelding.eksternVarsling?.prefererteKanaler).containsExactly(EksternKanal.SMS)
        assertThat(varselMelding.aktivFremTil).isNull()
        assertThat(varselMelding.produsent).isEqualTo(
            Produsent(
                cluster = "lokalt",
                namespace = "aap",
                appnavn = "meldekort-backend"
            )
        )

        assertThat(varselMelding.tekster).containsExactlyInAnyOrder(
            Tekst("nb", TEKSTER_OPPGAVE_MELDEPLIKTPERIODE.nb, true),
            Tekst("nn", TEKSTER_OPPGAVE_MELDEPLIKTPERIODE.nn, false),
            Tekst("en", TEKSTER_OPPGAVE_MELDEPLIKTPERIODE.en, false)
        )
    }

    fun kafkaTestConsumerProperties(brokers: String): Properties {
        val props = Properties()
        props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokers
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.GROUP_ID_CONFIG] = "varsel-consumer"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return props
    }
}
