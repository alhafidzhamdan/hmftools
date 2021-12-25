
plugins {
    id("com.hartwig.java-conventions")
}

description = "HMF Tools - Pave"

dependencies {
    implementation(project(":hmf-common"))
    implementation(project(":patient-db"))
    implementation(libs.jooq)
    implementation(libs.commons.lang3)
    testImplementation(libs.junit)
    annotationProcessor(libs.immutables.value)
    compileOnly(libs.immutables.value)
}

shadowJar {
    mainClass.set("com.hartwig.hmftools.pave.PaveApplication")
}
