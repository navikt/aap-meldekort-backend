plugins {
    id("aap.conventions")
}

dependencies {
    implementation(libs.slf4JApi)
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
    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.junitJupiterEngine)
    testImplementation(libs.assertjCore)
    constraints {
        implementation(libs.commonsCompress) {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
}