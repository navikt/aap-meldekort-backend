plugins {
    id("meldekort.conventions")
}

val komponenterVersjon = "1.0.483"
val junitVersjon = "6.0.2"

dependencies {
    implementation(project(":meldekortdomene"))
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("ch.qos.logback:logback-classic:1.5.24")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:0.0.531")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
}