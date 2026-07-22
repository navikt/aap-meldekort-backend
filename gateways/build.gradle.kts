plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":meldekortdomene"))
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.logbackClassic)
    implementation(libs.behandlingsflytKontrakt)

    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.junitJupiterEngine)
}