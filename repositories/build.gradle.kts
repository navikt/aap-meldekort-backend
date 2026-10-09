plugins {
    id("aap.conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":meldekortdomene"))

    implementation(kelvinLibs.micrometer.prometheus)
    implementation(kelvinLibs.logback.classic)
    implementation(kelvinLibs.logstash.logback.encoder)

    implementation(libs.dbconnect)
    implementation(libs.verdityper)
    implementation(libs.dbmigrering)
    implementation(libs.motor)
    implementation(libs.infrastructure)
    implementation(libs.json)
    implementation(libs.varselKotlinBuilder)
    implementation(kelvinLibs.kafka.clients)

    implementation(kelvinLibs.hikaricp)

    testImplementation(libs.dbtest)
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
    testImplementation(kelvinLibs.testcontainers.kafka)
}
