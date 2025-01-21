plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.114"
val ktorVersion = "3.0.3"

dependencies {
    api("no.nav:ktor-openapi-generator:1.0.72")
    implementation(project(":meldekortdomene"))

    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")

    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
}