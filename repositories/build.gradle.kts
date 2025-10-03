val komponenterVersjon = "1.0.386"
val junitVersjon = "6.0.0"

plugins {
    id("meldekort.conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":meldekortdomene"))

    implementation("io.micrometer:micrometer-core:1.15.4")
    implementation("ch.qos.logback:logback-classic:1.5.19")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:json:$komponenterVersjon")
    implementation("no.nav.tms.varsel:kotlin-builder:2.1.1")
    implementation("org.apache.kafka:kafka-clients:4.1.0")

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.13.2")
    runtimeOnly("org.postgresql:postgresql:42.7.8")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.6")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testFixturesImplementation("io.micrometer:micrometer-core:1.15.4")
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
    testImplementation("org.testcontainers:kafka:1.21.3")
}
