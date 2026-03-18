package no.nav.aap

import no.nav.aap.kelvin.KelvinSakRepositoryPostgres
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.opplysningsplikt.AktivitetsInformasjonRepositoryPostgres
import no.nav.aap.utfylling.UtfyllingRepositoryPostgres
import no.nav.aap.varsel.VarselRepositoryPostgres

val postgresRepositoryRegistry = RepositoryRegistry()
    .register<UtfyllingRepositoryPostgres>()
    .register<AktivitetsInformasjonRepositoryPostgres>()
    .register<KelvinSakRepositoryPostgres>()
    .register<FlytJobbRepositoryImpl>()
    .register<VarselRepositoryPostgres>()
