
plugins {
    id("com.hartwig.java-conventions")
}

description = "HMF Tools - Linx"

dependencies {
    implementation(project(":hmf-common"))
    implementation(project(":patient-db"))
    testImplementation(libs.junit)
    annotationProcessor(libs.immutables.value)
    compileOnly(libs.immutables.value)
}

shadowJar {
    mainClass.set("com.hartwig.hmftools.linx.LinxApplication")
}
