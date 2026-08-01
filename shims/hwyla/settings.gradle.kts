rootProject.name = "jade-hwyla-shim"

dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            from(files("./gradle/deps.versions.toml"))
        }
    }
}

// 1.12.2 backport: the shim is a STANDALONE build (`gradle -p shims/hwyla`) that includes the
// Jade root as a composite producer. Gradle substitutes `snownee.jade:Jade` -> the Jade root
// project here, so the shim compiles against Jade's own output -- no mavenLocal publication of
// Jade is ever needed.
includeBuild("../../") {
    dependencySubstitution {
        substitute(module("snownee.jade:Jade")).using(project(":"))
    }
}

pluginManagement {
    repositories {
        maven {
            // RetroFuturaGradle
            name = "GTNH Maven"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
            mavenContent {
                includeGroupByRegex("com\.gtnewhorizons\..+")
                includeGroup("com.gtnewhorizons")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
