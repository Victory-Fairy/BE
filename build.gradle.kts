import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    `java-library`
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    id("io.spring.javaformat") apply false
    id("org.asciidoctor.jvm.convert") apply false
    id("info.solidsoft.pitest") version "1.19.0" apply false
}

allprojects {
    group = providers.gradleProperty("projectGroup").get()

    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(
                JavaLanguageVersion.of(providers.gradleProperty("javaVersion").get().toInt())
            )
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "io.spring.javaformat")
    apply(plugin = "org.asciidoctor.jvm.convert")

    dependencies {
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
    }

    tasks.named<BootJar>("bootJar") {
        enabled = false
    }
    tasks.named<Jar>("jar") {
        enabled = true
    }

    tasks.named<Test>("test") {
        useJUnitPlatform {
            excludeTags("develop", "restdocs")
        }
    }

    tasks.register<Test>("unitTest") {
        group = "verification"
        useJUnitPlatform {
            excludeTags("develop", "context", "restdocs")
        }
    }

    tasks.register<Test>("contextTest") {
        group = "verification"
        useJUnitPlatform {
            includeTags("context")
        }
    }

    tasks.register<Test>("restDocsTest") {
        group = "verification"
        useJUnitPlatform {
            includeTags("restdocs")
        }
    }

    tasks.register<Test>("developTest") {
        group = "verification"
        useJUnitPlatform {
            includeTags("develop")
        }
    }

    tasks.named("asciidoctor") {
        dependsOn("restDocsTest")
    }
}
