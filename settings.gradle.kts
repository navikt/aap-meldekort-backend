plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "meldekort-backend"

include(
    "app",
    "meldekortdomene",
    "postgres-repositories",
    "http-flate",
    "arena-integrasjon",
    "lib-test"
)
