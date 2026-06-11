import org.gradle.api.JavaVersion.VERSION_25
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    java
    jacoco
    id("com.adarshr.test-logger") version "4.0.+"
    id("io.freefair.lombok") version "9.+"
    id("io.spring.dependency-management") version "1.1.+"
    id("org.jetbrains.kotlin.jvm") version "2.4.+"
    id("org.springframework.boot") version "4.1.+"
}

repositories {
    mavenCentral()
}

tasks.bootRun {
    val defaultJvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "-Xms2g",
        "-Xmx2g",
        "-XX:+ExitOnOutOfMemoryError",
        "-Djdk.tracePinnedThreads=full",
    )
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

extra["httpclient5.version"] = "5.6.+"
extra["httpcore5.version"] = "5.4.+"

dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.+")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.+")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql:42.+")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.+"))
    testImplementation("io.github.hakky54:logcaptor:2.12.+")
    testImplementation("org.apache.commons:commons-compress:1.28.+")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit-pioneer:junit-pioneer:2.3.+")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
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
    jvmArgs("--enable-native-access=ALL-UNNAMED", "-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
