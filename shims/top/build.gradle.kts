// 1.12.2 backport: the TOP (The One Probe) compatibility shim. A STANDALONE build
// (`gradle -p shims/top`): its settings includes the Jade root (`includeBuild("../../")`) so
// `snownee.jade:Jade` is substituted with the Jade project -- the shim compiles against Jade's
// output without any mavenLocal publication.
//
// build-logic is claimed by the Jade root (its settings includes "build-logic"), so the typesafe
// "conventions" catalog is registered into the root's settings only. This shim references the
// SAME convention plugins by their bare plugin id (minecraft/jvm/jvmdg/shadow/...) rather than
// alias(conventions.plugins.*); they resolve from the root-claimed build-logic as a nested
// included build. NOTE: "idea" resolves to Gradle's built-in idea plugin here (the core id
// shadows the convention's), so the group/version/archivesName the convention's idea plugin
// would set are applied explicitly below.
plugins {
    id("repositories")
    id("minecraft")
    id("shadow")
    id("jvmdg")
    id("idea")
    id("test")
    id("jvm")
}

// Mirrors the convention's idea plugin (which core "idea" shadows): mod identity for the jar.
group = modGroup
version = modVersion
base {
    archivesName = archiveName
}

// Test-mod classes live in src/testmod/java but are compiled as part of the test source set so
// they get the patched Minecraft classpath for free (same pattern as the Jade root).
sourceSets {
    named("test") {
        java.srcDir("src/testmod/java")
    }
}

dependencies {
    implementation("snownee.jade:Jade:1.0.0")
    testImplementation(deps.assertj.core)
    // Upgrades fastutil to 8.5.x on the test runtime: Jade's CallbackContainer needs
    // IntReferencePair, absent from the MC-bundled fastutil 7.1.0. The root build gets the
    // same upgrade transitively via its own compileClasspath.
    testImplementation(deps.dataFixerUpper)
}

// The jvmdg convention pins the test worker to Java 8, but the substituted Jade root
// (`snownee.jade:Jade` -> project(':Jade')) resolves to RAW Java-25 bytecode -- JvmDowngrader
// does not apply to a composite project's output. The shim's OWN test classes are downgraded
// by jvmdg (downgradeTest/downgradeMain) and load fine on a modern JVM, so run the tests on
// Java 25 (the same modern Java the runClientModernJava dev tasks use) rather than Java 8.
tasks.test {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.AZUL)
    }
}
