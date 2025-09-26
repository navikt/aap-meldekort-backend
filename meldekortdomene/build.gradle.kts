val komponenterVersjon = "1.0.373"
val junitVersjon = "5.13.4"

plugins {
    id("meldekort.conventions")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:0.0.446")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:${komponenterVersjon}")
    implementation(kotlin("reflect"))

    testImplementation(project(":repositories"))
    testImplementation(project(":lib-test"))
    testImplementation("no.nav.aap.kelvin:dbtest:${komponenterVersjon}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.6")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.14.5")
    testImplementation(kotlin("test"))
}