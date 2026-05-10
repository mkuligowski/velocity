package com.mkuligowski.velocity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Cross-cutting hygiene rules.
 * <ul>
 *   <li>No field injection — constructor injection only.</li>
 *   <li>No static {@code Instant.now()} / {@code OffsetDateTime.now()} etc. — inject {@code Clock}.</li>
 *   <li>No generic exceptions thrown from product code.</li>
 * </ul>
 */
class CommonRulesArchTest {

    static final JavaClasses CLASSES = new ClassFileImporter()
            .importPath(Paths.get("build/classes/java/main"));

    @Test
    void no_field_injection() {
        GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(CLASSES);
    }

    @Test
    void no_generic_exceptions() {
        GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(CLASSES);
    }

    @Test
    void no_java_util_logging() {
        GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(CLASSES);
    }

    @Test
    void no_static_now_calls_outside_clock_configuration() {
        // Clock-injection rule: only adapters/spring (where the Clock @Bean lives) may
        // call Clock.systemUTC(); product code reads time from the injected Clock.
        noClasses()
                .that().resideOutsideOfPackage("com.mkuligowski.velocity.loads.adapters.spring..")
                .should().callMethod(java.time.Instant.class, "now")
                .orShould().callMethod(java.time.LocalDate.class, "now")
                .orShould().callMethod(java.time.LocalDateTime.class, "now")
                .orShould().callMethod(java.time.LocalTime.class, "now")
                .orShould().callMethod(java.time.OffsetDateTime.class, "now")
                .orShould().callMethod(java.time.ZonedDateTime.class, "now")
                .because("inject a java.time.Clock instead — time must be deterministic in tests")
                .check(CLASSES);
    }
}
