plugins {
    idea
    java
    application
    id("com.github.johnrengelman.shadow") version ("7.0.0")
    id("com.github.ben-manes.versions") version ("0.39.0")
    id("io.freefair.lombok") version ("6.1.0-m3")
    id("com.google.cloud.tools.jib") version ("3.1.2")
}

group = "fr.raksrinana"
description = "FilBleuAttestations"

dependencies {
    implementation(libs.slf4j)
    implementation(libs.bundles.log4j2)

    implementation(libs.jakartaMail)

    implementation(libs.picocli)
    implementation(libs.bundles.jackson)

    implementation(libs.selenide)
}

repositories {
    mavenCentral()
}

tasks {
    processResources {
        expand(project.properties)
    }

    compileJava {
        val moduleName: String by project
        inputs.property("moduleName", moduleName)

        options.encoding = "UTF-8"
        options.isDeprecation = true

        doFirst {
            val compilerArgs = options.compilerArgs

            val path = classpath.asPath.split(";")
                .filter { it.endsWith(".jar") }
                .joinToString(";")
            compilerArgs.add("--module-path")
            compilerArgs.add(path)
            classpath = files()
        }
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveBaseName.set(project.name)
        archiveClassifier.set("shaded")
        archiveVersion.set("")
    }

    wrapper {
        val wrapperVersion: String by project
        gradleVersion = wrapperVersion
    }
}

application {
    val moduleName: String by project
    val className: String by project

    mainModule.set(moduleName)
    mainClass.set(className)
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16

    modularity.inferModulePath.set(false)
}

jib {
    from {
        image = "adoptopenjdk:16-jre"
        platforms {
            platform {
                os = "linux"
                architecture = "arm64"
            }
        }
    }
    to {
        image = "mrcraftcod/filbleu-attestations"
        auth {
            username = project.findProperty("dockerUsername").toString()
            password = project.findProperty("dockerPassword").toString()
        }
    }
}
