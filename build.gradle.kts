plugins {
    idea
    java
    application
    id("com.github.johnrengelman.shadow") version ("6.1.0")
    id("com.github.ben-manes.versions") version ("0.38.0")
    id("io.freefair.lombok") version ("6.0.0-m2")
    id("com.google.cloud.tools.jib") version ("3.0.0")
}

group = "fr.raksrinana"
description = "FilBleuAttestations"

dependencies {
    implementation(libs.slf4j)
    implementation(libs.logback) {
        exclude(group = "edu.washington.cs.types.checker", module = "checker-framework")
    }

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
            compilerArgs.add("--module-path")
            compilerArgs.add(classpath.asPath)
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

    mainClassName = className
    mainModule.set(moduleName)
    mainClass.set(className)
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
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
