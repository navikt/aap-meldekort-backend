plugins {
    id("aap.conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":meldekortdomene"))
    implementation(project(":repositories"))
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.server)
    implementation(libs.micrometerCore)
    implementation(libs.httpklient)
    implementation(libs.verdityper)
    implementation(libs.dbconnect)
    implementation(libs.tilgangApiKontrakt)

    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDatatypeJsr310)

    implementation(libs.logbackClassic)

    implementation(libs.nimbusJoseJwt)

    implementation(libs.junitJupiterApi)
    implementation(libs.testcontainersPostgresql)
    implementation(libs.varselKotlinBuilder)
}