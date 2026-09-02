import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("info.solidsoft.pitest")
}

val service = "api"
val ciBuild = providers.gradleProperty("ci").isPresent

tasks.named<BootJar>("bootJar") {
    enabled = true
    doLast {
        copy {
            from(archiveFile)
            into(rootProject.layout.projectDirectory.dir(".deploy"))
        }

        if (!ciBuild) {
            providers.exec {
                workingDir(rootProject.layout.projectDirectory.dir(".deploy"))
                commandLine("sh", "deploy.sh", service)
            }.result.get()
        }
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure:redis"))

    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-mysql")

    testImplementation(project(":tests:api-docs"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
    implementation("io.sentry:sentry-logback:${providers.gradleProperty("sentryVersion").get()}")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    compileOnly("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    implementation("org.springframework.security:spring-security-crypto")

    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.2")

    implementation("org.bouncycastle:bcprov-jdk18on:1.76")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.76")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("net.bramp.ffmpeg:ffmpeg:0.7.0")

    implementation(platform("software.amazon.awssdk:bom:2.49.6"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")
}

configure<PitestPluginExtension> {
    targetClasses.set(setOf("kr.co.victoryfairy.diary.application.DiaryQueryService"))
    targetTests.set(setOf("kr.co.victoryfairy.diary.application.DiaryListServiceTest"))
    excludedMethods.set(
        setOf(
            "writeDiary", "lambda\$writeDiary*",
            "updateDiary", "lambda\$updateDiary*",
            "deleteDiary", "lambda\$deleteDiary*",
            "findDailyList", "lambda\$findDailyList*",
            "findById", "lambda\$findById*",
            "toPartnerSaveRequests", "lambda\$toPartnerSaveRequests*"
        )
    )
    junit5PluginVersion.set("1.2.3")
    mutationThreshold.set(100)
    testStrengthThreshold.set(100)
    outputFormats.set(setOf("HTML", "XML"))
    timestampedReports.set(false)
    threads.set(4)
}
