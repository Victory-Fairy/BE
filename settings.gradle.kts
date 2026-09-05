pluginManagement {
    plugins {
        id("org.springframework.boot") version providers.gradleProperty("springBootVersion").get()
        id("io.spring.dependency-management") version providers.gradleProperty("springDependencyManagementVersion").get()
        id("org.asciidoctor.jvm.convert") version providers.gradleProperty("asciidoctorConvertVersion").get()
        id("io.spring.javaformat") version providers.gradleProperty("springJavaFormatVersion").get()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "BE"

include("modules:business")
include("applications:api")
include("applications:crawler")
include("tests:api-docs")
include("infrastructure:redis")
