val ktorVersion = "3.1.3"
val komponenterVersjon = "1.0.238"
val junitVersjon = "5.12.2"
val jacksonVersjon = "2.19.0"

plugins {
    id("meldekort.conventions")
    id("java-test-fixtures")
}


dependencies {
    implementation(project(":meldekortdomene"))
    implementation(project(":repositories"))
    implementation("no.nav.aap.behandlingsflyt:kontrakt:0.0.177")
    implementation("io.micrometer:micrometer-core:1.14.6")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-common:4.2.1.Final")
    }
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersjon")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")

    implementation("ch.qos.logback:logback-classic:1.5.18")

    implementation("com.nimbusds:nimbus-jose-jwt:10.2")

    implementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    implementation("org.testcontainers:postgresql:1.21.0")
}