import org.gradle.api.JavaVersion.VERSION_25
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    java
    jacoco
    id("com.adarshr.test-logger") version "4.0.+"
    id("io.freefair.lombok") version "8.+"
    id("io.spring.dependency-management") version "1.1.+"
    id("org.jetbrains.kotlin.jvm") version "2.2.+"
    id("org.springframework.boot") version "3.5.+"
}

repositories {
    mavenCentral()
}

tasks.bootRun {
    val defaultJvmArgs = listOf("-Xms2g", "-Xmx2g", "-XX:+ExitOnOutOfMemoryError", "-Djdk.tracePinnedThreads=full")
    val extendedArgs = listOf("-XX:+UseCompactObjectHeaders")
    jvmArgs = if (JavaVersion.current().isCompatibleWith(VERSION_25)) defaultJvmArgs + extendedArgs else defaultJvmArgs
}

val javaBytecodeVersion = project.findProperty("java.bytecode.version")?.toString() ?: "21"
    .also { println("Java bytecode version: $it") }

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = javaBytecodeVersion
    targetCompatibility = javaBytecodeVersion
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaBytecodeVersion))
    }
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.21.+")
    }
}

extra["httpclient5.version"] = "5.4"
extra["httpcore5.version"] = "5.3"

dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.google.guava:guava:33.4.+")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.+")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.+")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql:42.+")
    testImplementation("io.github.hakky54:logcaptor:2.11.+")
    testImplementation("org.apache.commons:commons-compress:1.27.+")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit-pioneer:junit-pioneer:2.3.+")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.+")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val activeProfiles = System.getenv("SPRING_PROFILES_ACTIVE")
if (activeProfiles?.contains("platform-tomcat") == true || activeProfiles?.contains("loom-tomcat") == true) {
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-web")
    }
    println("Added spring-boot-starter-web dependency")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 2
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
