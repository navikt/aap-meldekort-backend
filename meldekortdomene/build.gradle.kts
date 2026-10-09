plugins {
    id("aap.conventions")
}

dependencies {
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.motor)
    implementation(libs.motorApi)
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.verdityper)
    implementation(kotlin("reflect"))

    testImplementation(project(":repositories"))
    testImplementation(project(":lib-test"))
    testImplementation(libs.dbtest)
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kelvinLibs.mockk)
    testImplementation(kotlin("test"))
}