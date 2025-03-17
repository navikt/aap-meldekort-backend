plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
rootProject.name = "meldekort-backend"

include(
    "app",
    "meldekortdomene",
    "repositories",
    "http-flate",
    "gateways",
    "lib-test",
    "kontrakt",
)
