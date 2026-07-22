plugins {
    id("aap.conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":meldekortdomene"))

    implementation(libs.micrometerCore)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)

    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.motor)
    implementation(libs.infrastructure)
    implementation(libs.json)
    implementation(libs.varselKotlinBuilder)
    implementation(libs.kafkaClients)

    implementation(libs.hikaricp)
    implementation(libs.flywayDatabasePostgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.dbtest)
    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.junitJupiterEngine)
    testImplementation(libs.assertjCore)
    constraints {
        implementation(libs.commonsCompress) {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testFixturesImplementation(libs.micrometerCore)
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
    testImplementation(libs.testcontainersKafka)
}
