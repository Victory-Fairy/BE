dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.redisson:redisson:3.27.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
