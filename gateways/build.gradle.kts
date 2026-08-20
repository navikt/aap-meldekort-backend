plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":meldekortdomene"))
    implementation(libs.httpklient)
    api(libs.gateway)
    implementation(libs.infrastructure)
    implementation(libs.logbackClassic)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.unleashClientJava)

    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.junitJupiterEngine)
}