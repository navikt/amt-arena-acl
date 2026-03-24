import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    val kotlinVersion = "2.3.20"
    val springBootVersion = "4.0.4"
    val springDependencyManagementVersion = "1.1.7"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version springDependencyManagementVersion
}

group = "no.nav.amt.arena-acl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

val nimbusVersion = "11.34"
val okhttpVersion = "5.3.2"
val shedlockVersion = "7.6.0"
val unleashVersion = "12.2.0"
val navCommonVersion = "3.2026.03.04_12.35-b34c347c6239"
val navTokenSupportVersion = "6.0.4"
val logstashEncoderVersion = "9.0"
val jacksonModuleKotlinVersion = "3.1.0"

val kotestVersion = "6.1.7"
val mockkVersion = "1.14.9"
val springmockkVersion = "5.0.1"
val kotestExtensionsSpringVersion = "1.3.0"
val kotestExtensionsTestcontainersVersion = "2.0.2"

val amtLibVersion = "1.2026.03.15_00.13-2aec41852959"
val navCommonModules = setOf("log", "job", "rest", "token-client")

dependencyManagement {
    dependencies {
        dependency("com.squareup.okhttp3:okhttp:$okhttpVersion")
        dependency("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
    }
}

dependencies {
    implementation("com.nimbusds:oauth2-oidc-sdk:$nimbusVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlockVersion")

    implementation("no.nav.security:token-validation-spring:$navTokenSupportVersion")

    navCommonModules.forEach {
        implementation("no.nav.common:$it:$navCommonVersion")
    }

    implementation("no.nav.common:kafka:$navCommonVersion") {
        exclude(group = "org.xerial.snappy", module = "snappy-java")
        exclude(group = "org.apache.avro", module = "avro")
        exclude(group = "io.confluent", module = "kafka-avro-serializer")
    }

    implementation("io.getunleash:unleash-client-java:$unleashVersion")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-flyway")

    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonModuleKotlinVersion")

    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("no.nav.amt.deltakelser.lib:models:$amtLibVersion")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-spring:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-testcontainers:$kotestVersion")
    testImplementation("io.mockk:mockk-jvm:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$navTokenSupportVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
    testImplementation("org.springframework.boot:spring-boot-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")

    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-kafka")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
            "-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled",
            "-Xmulti-dollar-interpolation",
        )
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<Test>("test") {
    jvmArgs(
        "-Xshare:off",
        "-XX:+EnableDynamicAgentLoading",
    )
    useJUnitPlatform()
}
