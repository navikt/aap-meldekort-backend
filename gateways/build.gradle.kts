plugins {
    id("aap.conventions")
}

val komponenterVersjon = "2.0.1"
val junitVersjon = "6.0.2"

dependencies {
    implementation(project(":meldekortdomene"))
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("ch.qos.logback:logback-classic:1.5.29")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:0.0.551")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
}