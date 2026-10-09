import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
    `maven-publish`
    `java-library`
}

dependencies {
    api(kelvinLibs.jackson.annotations)
    api(libs.ktorOpenApiGenerator)
    compileOnly(libs.tilgangApiKontrakt)

    testRuntimeOnly(libs.tilgangApiKontrakt)
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(libs.json)
}

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