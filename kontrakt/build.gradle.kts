import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("meldekort.conventions")
    `maven-publish`
    `java-library`
}

val tilgangVersjon = "1.0.108"
val junitVersion = "5.12.0"
val komponenterVersjon = "1.0.318"

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.19.2")
    api("no.nav:ktor-openapi-generator:1.0.121")
    compileOnly("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.27.4")
    testImplementation("no.nav.aap.kelvin:json:$komponenterVersjon")
}

group = "no.nav.aap.meldekort"

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
            artifactId = project.name
            version = project.findProperty("version")?.toString() ?: "0.0.0"
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