plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.springframework.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.plugin.spring)
}

group = "no.nav.amt.arena-acl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.oauth2.oidc.sdk)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.shedlock.provider.jdbc.template)

    implementation(libs.nav.token.validation.spring)
    implementation(libs.nav.common.log)
    implementation(libs.nav.common.job)
    implementation(libs.nav.common.rest)
    implementation(libs.nav.common.token.client)
    implementation(libs.nav.common.kafka) {
        exclude(group = "org.xerial.snappy", module = "snappy-java")
        exclude(group = "org.apache.avro", module = "avro")
        exclude(group = "io.confluent", module = "kafka-avro-serializer")
    }

    implementation(libs.unleash.client.java)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-logging")

    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("io.micrometer:micrometer-registry-prometheus")

    testImplementation(libs.kotest.runner.junit5.jvm)
    testImplementation(libs.mockk)
    testImplementation(libs.nav.token.validation.spring.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.jar {
    enabled = false
}

tasks.test {
    jvmArgs(
        "-Xshare:off",
        "-XX:+EnableDynamicAgentLoading",
        "-Dkotest.framework.classpath.scanning.autoscan.disable=true",
    )
    useJUnitPlatform()
}
