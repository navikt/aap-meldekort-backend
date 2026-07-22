import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
    `maven-publish`
    `java-library`
}

dependencies {
    api(libs.jacksonAnnotations)
    api(libs.ktorOpenApiGenerator)
    compileOnly(libs.tilgangApiKontrakt)

    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.tilgangApiKontrakt)
    testRuntimeOnly(libs.junitJupiterEngine)
    testImplementation(libs.assertjCore)
    testImplementation(libs.json)
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