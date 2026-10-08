plugins {
    alias(libs.plugins.jmh)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.vanniktech.maven.publish)
    `java-library`
}

base {
    archivesName = "MixingKore"
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

tasks.processResources {
    val projectVersion = project.version
    inputs.property("version", projectVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to projectVersion)
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.compilerArgs.addAll(listOf("--add-modules", "jdk.incubator.vector"))
}
tasks.withType<Test>().configureEach {
    jvmArgs("--add-modules", "jdk.incubator.vector")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--add-modules", "jdk.incubator.vector")
}
tasks.register<JavaExec>("MathA") {
    group = "Test"
    description = "Jmh"
    dependsOn("jmhCompileGeneratedClasses")
    mainClass.set("org.openjdk.jmh.Main")
    classpath = sourceSets["jmh"].runtimeClasspath + sourceSets["main"].runtimeClasspath + files(layout.buildDirectory.dir("jmh-generated-classes")) + files(layout.buildDirectory.dir("jmh-generated-resources"))
    if (project.hasProperty("jmhArgs")) {
        args(project.property("jmhArgs").toString().split(" "))
    }
}
tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)
    from(files(rootProject.file("LICENSE"), rootProject.file("NOTICE")))
}
tasks.withType<Javadoc> {
    val v = options as StandardJavadocDocletOptions
    v.addStringOption("Xdoclint:none", "-quiet")
    exclude("io/github/mixforce/mixingkore/internal/**")
}
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.mixforce", "mixing-kore", version.toString())
    pom {
        name.set("MixingKore")
        description.set("A performance algorithm library for Minecraft mods.")
        url.set("https://github.com/MixForce/mixing-kore")
        licenses {
            license {
                name.set("GNU Lesser General Public License v3.0 or later")
                url.set("https://www.gnu.org/licenses/lgpl-3.0.html")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("MixForce")
                name.set("MixForce")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/MixForce/mixing-kore.git")
            developerConnection.set("scm:git:ssh://git@github.com/MixForce/mixing-kore.git")
            url.set("https://github.com/MixForce/mixing-kore")
        }
    }
}
