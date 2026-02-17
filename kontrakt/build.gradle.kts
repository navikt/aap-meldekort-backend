import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
    `maven-publish`
    `java-library`
}

val tilgangVersjon = "1.0.179"
val junitVersion = "5.12.0"
val komponenterVersjon = "2.0.1"

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.21")
    api("no.nav.aap.kelvin:ktor-openapi-generator:$komponenterVersjon")
    compileOnly("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("no.nav.aap.kelvin:json:$komponenterVersjon")
}

apply(plugin = "maven-publish")
apply(plugin = "java-library")

kotlin {
    explicitApi = ExplicitApiMode.Warning
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-meldekort-backend")
            credentials {
                username = "x-access-token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}