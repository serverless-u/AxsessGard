import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.openapi.generator") version "6.6.0"
}

group = "com.javax0"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78")
    implementation("org.postgresql:postgresql")
    implementation("com.google.code.gson:gson:2.10.1")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

// Explicitly set the source and target compatibility for Java compilation
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
}

// This task will copy all runtime dependencies to build/libs/lib
tasks.register<Copy>("copyRuntimeDependencies") {
    from(configurations.runtimeClasspath)
    into("build/libs/lib")
}

// Ensure the copyRuntimeDependencies task runs before the build task
tasks.build {
    dependsOn("copyRuntimeDependencies")
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

kotlin {
    sourceSets.main {
        kotlin.srcDir(layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.absolutePath)
    }
}

// Configure the jar task
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.javax0.axsessgard.AxsessgardApplicationKt"
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(" ") { "lib/${it.name}" }
    }
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/src/main/resources/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)
    apiPackage.set("com.javax0.axsessgard.api")
    modelPackage.set("com.javax0.axsessgard.model")
    configOptions.set(mapOf(
        "interfaceOnly" to "true",
        "useSpringBoot3" to "true",
        "useTags" to "true",
        "serviceInterface" to "true",
        "serializationLibrary" to "jackson",
        "skipDefaultInterface" to "true"
    ))
}