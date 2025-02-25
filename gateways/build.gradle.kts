plugins {
    id("meldekort.conventions")
}

val komponenterVersjon = "1.0.159"
val junitVersjon = "5.11.4"

dependencies {
    implementation(project(":meldekortdomene"))
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("ch.qos.logback:logback-classic:1.5.17")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
}