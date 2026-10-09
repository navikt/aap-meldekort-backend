plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "meldekort-backend"

include(
    "app",
    "meldekortdomene",
    "repositories",
    "gateways",
    "lib-test",
    "kontrakt",
)

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    versionCatalogs {
        create("kelvinLibs") {
            from("no.nav.aap.kelvin:version-catalog:2.0.177")
        }
    }
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
    }
}

