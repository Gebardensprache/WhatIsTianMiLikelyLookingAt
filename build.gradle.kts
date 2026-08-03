@file:Suppress("AvoidDuplicateDependencies")

plugins {
    alias(conventions.plugins.repositories)
    alias(conventions.plugins.minecraft)
    alias(conventions.plugins.publish)
    alias(conventions.plugins.shadow)
    alias(conventions.plugins.jvmdg)
    alias(conventions.plugins.idea)
    alias(conventions.plugins.test)
    alias(conventions.plugins.jvm)
}

group = modGroup
version = modVersion
base {
    archivesName = archiveName
}

sourceSets {
    named("test") {
        java.srcDir("src/testmod/java")
    }
}

dependencies {
    compileOnlyApi(deps.jspecify)
    compileOnlyApi(deps.annotations)
    testImplementation(deps.assertj.core)

    shadowDowngrade(deps.dataFixerUpper)

    compileOnlyApi(deps.hei)

    // Test mod for proving that TOP shim works
    runtimeOnly(deps.hei)
    implementation(deps.codeChickenLib) { isTransitive = false }
    implementation(deps.gregtech) { isTransitive = false }
    implementation(deps.ae2uel) { isTransitive = false }
}

configurations {
    compileOnly {
        // exclude GNU trove, FastUtil is superior and still updated
        exclude(group = "net.sf.trove4j", module = "trove4j")
        // exclude scala as we don't use it for anything and causes import confusion
        exclude(group = "org.scala-lang")
        exclude(group = "org.scala-lang.modules")
        exclude(group = "org.scala-lang.plugins")
    }
}