plugins {
    val kotlinVersion = "2.3.0"
    val springBootVersion = "4.0.1"
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

val nimbusVersion = "11.31"
val okhttpVersion = "5.3.2"
val shedlockVersion = "7.5.0"
val unleashVersion = "11.2.1"
val navCommonVersion = "3.2025.10.10_08.21-bb7c7830d93c"
val navTokenSupportVersion = "5.0.39"
val logstashEncoderVersion = "9.0"

val kotestVersion = "6.0.7"
val mockkVersion = "1.14.7"
val springmockkVersion = "5.0.1"
val testcontainersVersion = "2.0.3"
val kotestExtensionsSpringVersion = "1.3.0"
val kotestExtensionsTestcontainersVersion = "2.0.2"

val amtLibVersion = "1.2025.12.06_12.56-a9fdb0b96ea0"
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

// fjernes ved neste release av org.apache.kafka:kafka-clients
configurations.configureEach {
    resolutionStrategy {
        capabilitiesResolution {
            withCapability("org.lz4:lz4-java") {
                select(candidates.first { (it.id as ModuleComponentIdentifier).group == "at.yawk.lz4" })
            }
        }
    }
}

dependencies {
    implementation("at.yawk.lz4:lz4-java:1.10.2") // fjernes ved neste release av org.apache.kafka:kafka-clients
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

    implementation("no.nav.amt.lib:models:${amtLibVersion}")

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

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<Test>("test") {
    jvmArgs(
        "-Xshare:off",
        "-XX:+EnableDynamicAgentLoading"
    )
    useJUnitPlatform()
}
