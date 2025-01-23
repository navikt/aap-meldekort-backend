import org.slf4j.MDC

plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.118"

dependencies {
    implementation(project(":meldekortdomene"))
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("ch.qos.logback:logback-classic:1.5.16")
}