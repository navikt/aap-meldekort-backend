package no.nav.aap

import no.nav.aap.arena.MeldekortRepositoryPostgres
import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.opplysningsplikt.TimerArbeidetRepositoryPostgres
import no.nav.aap.utfylling.UtfyllingRepositoryPostgres
import no.nav.aap.varsel.VarselRepositoryPostgres

val postgresRepositoryRegistry = RepositoryRegistry()
    .register<MeldekortRepositoryPostgres>()
    .register<UtfyllingRepositoryPostgres>()
    .register<TimerArbeidetRepositoryPostgres>()
    .register<KelvinSakRepositoryPostgres>()
    .register<FlytJobbRepositoryImpl>()
    .register<VarselRepositoryPostgres>()
