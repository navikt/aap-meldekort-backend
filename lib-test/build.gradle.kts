plugins {
    id("aap.conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":meldekortdomene"))
    implementation(project(":repositories"))
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.server)
    implementation(kelvinLibs.micrometer.prometheus)
    implementation(libs.httpklient)
    implementation(libs.verdityper)
    implementation(libs.dbconnect)
    implementation(libs.tilgangApiKontrakt)

    implementation(kelvinLibs.jackson.databind)
    implementation(kelvinLibs.jackson.datatype.jsr310)

    implementation(kelvinLibs.logback.classic)

    implementation(kelvinLibs.nimbus.jose.jwt)

    implementation(kelvinLibs.junit.jupiter.api)
    implementation(kelvinLibs.testcontainers.postgresql)
    implementation(libs.varselKotlinBuilder)
}