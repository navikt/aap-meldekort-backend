plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.120"

dependencies {
    implementation(project(":meldekortdomene"))
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("ch.qos.logback:logback-classic:1.5.16")
}