plugins {
    id("aap.conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":meldekortdomene"))
    implementation(project(":repositories"))
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.micrometerCore)
    implementation(libs.httpklient)
    implementation(libs.verdityper)
    implementation(libs.dbconnect)
    implementation(libs.tilgangApiKontrakt)
    implementation(libs.ktorServerAuth)
    implementation(libs.ktorServerAuthJwt)
    implementation(libs.ktorServerCallLogging)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorServerMetricsMicrometer)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerStatusPages)

    implementation(libs.ktorSerializationJackson)
    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDatatypeJsr310)

    implementation(libs.logbackClassic)

    implementation(libs.nimbusJoseJwt)

    implementation(libs.junitJupiterApi)
    implementation(libs.testcontainersPostgresql)
    implementation(libs.varselKotlinBuilder)
}