import kotlin.String

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

val githubPassword: String? by settings


dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/behandlingsflyt")
            credentials {
                username = "x-access-token"
                password = (githubPassword
                    ?: System.getenv("GITHUB_PASSWORD")
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: error("GITHUB_TOKEN not set"))
            }
        }
    }
}

