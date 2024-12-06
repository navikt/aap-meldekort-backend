plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.79"

dependencies {
    implementation(project(":meldekortdomene"))
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
}