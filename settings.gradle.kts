plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "meldekort-backend"

include(
    "app",
    "lib-test"
)
include("flate")
