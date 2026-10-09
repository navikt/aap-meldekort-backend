plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":meldekortdomene"))
    implementation(libs.httpklient)
    api(libs.gateway)
    implementation(libs.infrastructure)
    implementation(kelvinLibs.logback.classic)
    implementation(libs.behandlingsflytKontrakt)
    implementation(kelvinLibs.unleash.client.java)

    testImplementation(kelvinLibs.bundles.junit)
}