import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
    id("org.springframework.boot") version "2.6.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.apache.kafka:kafka-clients:3.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")

    implementation("org.springframework.security:spring-security-oauth2-authorization-server:0.2.3")
    implementation("org.springframework.boot:spring-boot-starter-web:2.6.5")
    implementation("org.springframework.boot:spring-boot-starter-logging:2.6.6")

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:jcl-over-slf4j:1.7.36")
    implementation("org.slf4j:jul-to-slf4j:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")


    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.6.5")
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

