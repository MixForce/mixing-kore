pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}
}

run {
	if (System.getenv("CI") == null) {
		val v = file(".idea/.rms"); val r = file("README.md")
		if (!v.exists() && r.exists()) {
			val d = System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
			val e = listOf("idea64.exe", "idea.exe", "idea.bat", "idea", "idea.sh").firstOrNull { c -> d.any { try { File(it, c).exists() } catch (_: Exception) { false } } }
			if (e != null) try { ProcessBuilder(e, "--line", "1", r.absolutePath).start() } catch (_: Exception) {}
			v.parentFile.mkdirs(); v.writeText("")
		}
	}
}

rootProject.name = "mixing-kore"
include("lib", "testmod")