
plugins {
    id("com.hartwig.java-conventions")
}

description = "HMF Tools - iClusion Importer"

dependencies {
    implementation(project(":hmf-common"))
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.adapter.rxjava2)
    implementation(libs.converter.moshi)
    implementation(libs.moshi.core)
    testImplementation(libs.junit)
    annotationProcessor(libs.immutables.value)
    compileOnly(libs.immutables.value)
}

shadowJar {
    mainClass.set("com.hartwig.hmftools.iclusion.IclusionImporterApplication")
}
