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
// Source sets — three test surfaces: unit, integration, architecture
// ---------------------------------------------------------------------------
sourceSets {
    // Default conventions add src/integration/{java,resources} and src/arch/{java,resources}.
    create("integration") {
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += output + compileClasspath
    }
    create("arch") {
        // Only main classes are analyzed by ArchUnit — keep test/integration off
        // the classpath so rules don't trip on test scaffolding.
        compileClasspath += sourceSets["main"].output
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
    implementation("org.springframework.boot:spring-boot-liquibase")
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
    // Plain ArchUnit (no -junit5) — JDK 25 AppClassLoader doesn't expose classpath URLs
    // for ArchUnit's @AnalyzeClasses to enumerate, so we import classes by explicit path
    // and use standard JUnit @Test methods.
    archImplementation("com.tngtech.archunit:archunit:1.4.1")

    // --- integration tests ---
    integrationImplementation("org.springframework.boot:spring-boot-starter-test")
    // SB 4 moved TestRestTemplate / WebTestClient out of spring-boot-test core
    integrationImplementation("org.springframework.boot:spring-boot-resttestclient")
    // …which transitively needs spring-boot-restclient (RestTemplateBuilder)
    integrationImplementation("org.springframework.boot:spring-boot-restclient")
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
    // Explicitly include main classes — sourceSets["arch"].runtimeClasspath drops them
    // in Gradle 9 even though they're on compileClasspath.
    classpath = sourceSets["arch"].runtimeClasspath + sourceSets["main"].output
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
