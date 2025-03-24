plugins {
    id("meldekort.conventions")
}

val tilgangVersjon = "1.0.29"
val komponenterVersjon = "1.0.183"
val ktorVersion = "3.1.1"

dependencies {
    api("no.nav:ktor-openapi-generator:1.0.98")
    implementation(project(":meldekortdomene"))
    implementation(project(":kontrakt"))

    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")

    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
}