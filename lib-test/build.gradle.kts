val ktorVersion = "3.4.2"
val komponenterVersjon = "2.0.26"
val junitVersjon = "6.0.3"
val jacksonVersjon = "2.21.2"

plugins {
    id("aap.conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":meldekortdomene"))
    implementation(project(":repositories"))
    implementation("no.nav.aap.behandlingsflyt:kontrakt:0.0.582")
    implementation("io.micrometer:micrometer-core:1.16.4")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersjon")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")

    implementation("ch.qos.logback:logback-classic:1.5.32")

    implementation("com.nimbusds:nimbus-jose-jwt:10.9")

    implementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    implementation("org.testcontainers:testcontainers-postgresql:2.0.4")
    implementation("no.nav.tms.varsel:kotlin-builder:2.2.0")
}