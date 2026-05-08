import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "9.0.0"
    id("net.ltgt.errorprone") version "4.3.0"
}

group = "com.mkuligowski"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

// ---------------------------------------------------------------------------
// Source sets — three test surfaces per BLUEPRINT.md §12 / DESIGN.md §15
// ---------------------------------------------------------------------------
sourceSets {
    create("integration") {
        java.srcDir("src/integration/java")
        resources.srcDir("src/integration/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += output + compileClasspath
    }
    create("arch") {
        java.srcDir("src/arch/java")
        resources.srcDir("src/arch/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += output + compileClasspath
    }
}

val integrationImplementation: Configuration by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}
val integrationRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations["testRuntimeOnly"])
}
val integrationCompileOnly: Configuration by configurations.getting {
    extendsFrom(configurations["testCompileOnly"])
}
val integrationAnnotationProcessor: Configuration by configurations.getting {
    extendsFrom(configurations["testAnnotationProcessor"])
}

val archImplementation: Configuration by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}
val archRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations["testRuntimeOnly"])
}

// ---------------------------------------------------------------------------
// Dependencies
// ---------------------------------------------------------------------------
dependencies {
    // --- core ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.liquibase:liquibase-core")

    // --- JSpecify for @NullMarked / @Nullable ---
    implementation("org.jspecify:jspecify:1.0.0")

    // --- mapping ---
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // --- runtime DB ---
    runtimeOnly("com.h2database:h2")

    // --- ErrorProne / NullAway ---
    errorprone("com.google.errorprone:error_prone_core:2.40.0")
    errorprone("com.uber.nullaway:nullaway:0.12.7")

    // --- unit tests ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // --- arch tests ---
    archImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")

    // --- integration tests ---
    integrationImplementation("org.springframework.boot:spring-boot-starter-test")
}

// ---------------------------------------------------------------------------
// Test tasks — keep `test` fast; integration & arch run on `check`
// ---------------------------------------------------------------------------
tasks.named<Test>("test") {
    useJUnitPlatform()
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests against full Spring context + H2."
    group = "verification"
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter("test")
}

val archTest = tasks.register<Test>("archTest") {
    description = "Runs ArchUnit architectural rule tests."
    group = "verification"
    testClassesDirs = sourceSets["arch"].output.classesDirs
    classpath = sourceSets["arch"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter("test")
}

tasks.named("check") {
    dependsOn(integrationTest, archTest)
}

// ---------------------------------------------------------------------------
// ErrorProne + NullAway — domain purity & null-safety enforcement
// ---------------------------------------------------------------------------
tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        // NullAway only runs against our packages; turning off the rest of
        // ErrorProne's checks keeps signal high during early scaffolding.
        disableAllChecks = true
        check("NullAway", net.ltgt.gradle.errorprone.CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "com.mkuligowski.velocity")
        option("NullAway:JSpecifyMode", "true")
    }
}
// Don't enforce NullAway on test or arch sources.
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.isEnabled = false
}
tasks.named<JavaCompile>("compileIntegrationJava") {
    options.errorprone.isEnabled = false
}
tasks.named<JavaCompile>("compileArchJava") {
    options.errorprone.isEnabled = false
}
