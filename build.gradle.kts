plugins {
    val kotlinVersion = "2.2.10"
    val springBootVersion = "3.5.5"
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

val nimbusVersion = "11.28"
val okhttpVersion = "5.1.0"
val shedlockVersion = "6.10.0"
val unleashVersion = "11.1.0"
val navCommonVersion = "3.2025.09.03_08.33-728ff4acbfdb"
val navTokenSupportVersion = "5.0.36"
val logstashEncoderVersion = "8.1"

val kotestVersion = "6.0.3"
val mockkVersion = "1.14.5"
val springmockkVersion = "4.0.2"
val testcontainersVersion = "1.21.3"
val kotestExtensionsSpringVersion = "1.3.0"
val kotestExtensionsTestcontainersVersion = "2.0.2"

val navCommonModules = setOf("log", "job", "rest", "token-client")

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }

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

    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-spring:${kotestVersion}")
    testImplementation("io.kotest:kotest-extensions-testcontainers:${kotestVersion}")
    testImplementation("io.mockk:mockk-jvm:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$navTokenSupportVersion")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
            "-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled",
            "-Xmulti-dollar-interpolation",
        )
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
        "-Dkotest.framework.config.fqn=no.nav.amt.arena.acl.KotestConfig",
    )
    useJUnitPlatform()
}
