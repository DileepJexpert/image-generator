plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.katixo"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web + WebSocket
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Persistence
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Redis (job queue)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Health checks / metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ComfyUI workflow graphs live next to the generation feature code
// (src/main/java/.../generation/workflows/*.json per CLAUDE.md). Copy them
// onto the classpath so ComfyUiClient can load them as resources.
tasks.named<ProcessResources>("processResources") {
    from("src/main/java") {
        include("**/workflows/*.json")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
