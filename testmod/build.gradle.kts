plugins {
    alias(libs.plugins.jmh)
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = "MixingKore-Testmod"
}

version = libs.versions.mod.version.get()
group = providers.gradleProperty("maven_group").get()

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    jmh(libs.bundles.jmh)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.compilerArgs.addAll(listOf("--add-modules", "jdk.incubator.vector"))
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--add-modules", "jdk.incubator.vector")
}

tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)
    from(files(rootProject.file("LICENSE"), rootProject.file("NOTICE"))) { rename { "${it}_$projectName" } }
}