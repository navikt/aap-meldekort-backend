import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val komponenterVersjon = "1.0.280"
val junitVersjon = "5.13.3"
val tilgangVersjon = "1.0.93"
val ktorVersion = "3.2.1"

plugins {
    id("meldekort.conventions")
    id("io.ktor.plugin") version "3.2.1"
}

application {
    mainClass.set("no.nav.aap.meldekort.AppKt")
}

tasks {
    val projectProps by registering(WriteProperties::class) {
        destinationFile = layout.buildDirectory.file("version.properties")
        // Define property.
        property("project.version", getCheckedOutGitCommitHash())
    }

    processResources {
        // Depend on output of the task to create properties,
        // so the properties file will be part of the Java resources.

        from(projectProps)
    }

    withType<ShadowJar> {
        mergeServiceFiles()
    }
}

fun runCommand(command: String): String {
    val execResult = providers.exec {
        this.workingDir = project.projectDir
        commandLine(command.split("\\s".toRegex()))
    }.standardOutput.asText

    return execResult.get()
}

fun getCheckedOutGitCommitHash(): String {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        return System.getenv("GITHUB_SHA")
    }
    return runCommand("git rev-parse --verify HEAD")
}


dependencies {
    implementation(project(":meldekortdomene"))
    implementation(project(":repositories"))
    implementation(project(":gateways"))
    implementation(project(":kontrakt"))

    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")

    implementation("io.micrometer:micrometer-registry-prometheus:1.15.1")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    api("no.nav:ktor-openapi-generator:1.0.117")

    testImplementation(testFixtures(project(":repositories")))
    testImplementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
    testImplementation("org.testcontainers:kafka:1.21.3")
}
