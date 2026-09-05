import org.gradle.api.tasks.bundling.Jar
import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") {
    enabled = true
}
tasks.named<Jar>("jar") {
    enabled = false
}

dependencies {
    implementation(project(":modules:business"))
    implementation(project(":infrastructure:redis"))

    testImplementation(project(":tests:api-docs"))
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("io.sentry:sentry-logback:${providers.gradleProperty("sentryVersion").get()}")
    implementation("com.microsoft.playwright:playwright:1.48.0")
}
