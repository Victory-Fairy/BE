dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springframework.boot:spring-boot-starter-test")
    compileOnly("tools.jackson.core:jackson-databind")
    api("org.springframework.restdocs:spring-restdocs-mockmvc")
    api("io.rest-assured:spring-mock-mvc:6.0.1")
}
