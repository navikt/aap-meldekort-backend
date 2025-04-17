val komponenterVersjon = "1.0.226"
val junitVersjon = "5.12.2"

plugins {
    id("meldekort.conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":meldekortdomene"))

    implementation("io.micrometer:micrometer-core:1.14.6")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:json:$komponenterVersjon")

    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.7.1")
    runtimeOnly("org.postgresql:postgresql:42.7.5")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testFixturesImplementation("io.micrometer:micrometer-core:1.14.6")
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
}
