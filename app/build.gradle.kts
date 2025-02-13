import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.ByteArrayOutputStream

val komponenterVersjon = "1.0.135"

plugins {
    id("behandlingsflyt.conventions")
    id("io.ktor.plugin") version "3.1.0"
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
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = command.split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

fun getCheckedOutGitCommitHash(): String {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        return System.getenv("GITHUB_SHA")
    }
    return runCommand("git rev-parse --verify HEAD")
}

dependencies {
    implementation(project(":meldekortdomene"))
    implementation(project(":http-flate"))
    implementation(project(":postgres-repositories"))
    implementation(project(":arena-integrasjon"))

    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")

    implementation("io.micrometer:micrometer-registry-prometheus:1.14.4")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")


    testImplementation(testFixtures(project(":postgres-repositories")))
    testImplementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
}
