val ktorVersion = "3.1.1"
val komponenterVersjon = "1.0.159"
val tilgangVersjon = "0.0.72"
val junitVersjon = "5.11.4"

plugins {
    id("behandlingsflyt.conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":meldekortdomene"))
    implementation(project(":repositories"))
    implementation("io.micrometer:micrometer-core:1.14.4")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-common:4.1.119.Final")
    }
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    implementation("ch.qos.logback:logback-classic:1.5.17")

    implementation("com.nimbusds:nimbus-jose-jwt:10.0.2")

    implementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    implementation("org.testcontainers:postgresql:1.20.5")
}