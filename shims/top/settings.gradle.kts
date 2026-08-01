rootProject.name = "jade-top-shim"

dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            from(files("./gradle/deps.versions.toml"))
        }
        // The root-claimed build-logic convention plugins (minecraft/jvmdg/shadow/test/...)
        // reference the `libs` catalog, which is auto-generated from `gradle/libs.versions.toml`
        // in the Jade root. Point at the root's file so those plugins resolve their plugin and
        // library aliases the same way they do in the root build.
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

// 1.12.2 backport: the shim is a STANDALONE build (`gradle -p shims/top`) that includes the
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
                includeGroupByRegex("com\\.gtnewhorizons\\..+")
                includeGroup("com.gtnewhorizons")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
