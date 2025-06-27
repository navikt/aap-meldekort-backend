package no.nav.aap.varsel

import no.nav.aap.Ident
import no.nav.aap.KafkaProducerConfig
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.lookup.gateway.Factory
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

class VarselGatewayKafkaProducer(producerConfig: KafkaProducerConfig) : VarselGateway {

    companion object : Factory<VarselGatewayKafkaProducer> {
        override fun konstruer(): VarselGatewayKafkaProducer {
            return VarselGatewayKafkaProducer(KafkaProducerConfig())
        }
    }

    private val topic = requiredConfigForKey("brukervarel.topic")
    private val producer = KafkaProducer(producerConfig.properties(), StringSerializer(), StringSerializer())

    override fun sendVarsel(
        brukerId: Ident,
        varsel: Varsel,
        varselTekster: VarselTekster,
        lenke: String
    ) {
        val melding = opprettKafkaJson(brukerId, varsel, varselTekster, lenke)
        producer.send(ProducerRecord(topic, varsel.varselId.toString(), melding))
    }

    override fun inaktiverVarsel(varsel: Varsel) {
        val melding = VarselActionBuilder.inaktiver { varselId = varsel.varselId.id.toString() }
        producer.send(ProducerRecord(topic, varsel.varselId.toString(), melding))
    }

    private fun opprettKafkaJson(
        brukerId: Ident,
        varsel: Varsel,
        varselTekster: VarselTekster,
        lenke: String
    ): String {
        return VarselActionBuilder.opprett {
            type = when (varsel.typeVarsel) {
                TypeVarsel.BESKJED -> Varseltype.Beskjed
                TypeVarsel.OPPGAVE -> Varseltype.Oppgave
            }
            varselId = varsel.varselId.id.toString()
            sensitivitet = Sensitivitet.High
            ident = brukerId.asString
            tekster += Tekst(
                spraakkode = "nb",
                tekst = varselTekster.nb,
                default = true
            )
            tekster += Tekst(
                spraakkode = "nn",
                tekst = varselTekster.nn,
                default = false
            )
            tekster += Tekst(
                spraakkode = "en",
                tekst = varselTekster.en,
                default = false
            )
            link = lenke
            aktivFremTil = null
            eksternVarsling {
                preferertKanal = EksternKanal.SMS
                /*
                utsettSendingTil = ... kan eventuelt brukes til å styre når varsel sendes i stede for når
                Kafka-meldingen blir sendt. SMS sendes mellom 9-17.
                */
            }
        }
    }
}
