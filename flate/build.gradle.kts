plugins {
    id("behandlingsflyt.conventions")
}

val ktorVersion = "3.0.1"

dependencies {
    api("no.nav:ktor-openapi-generator:1.0.50")

    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")
}