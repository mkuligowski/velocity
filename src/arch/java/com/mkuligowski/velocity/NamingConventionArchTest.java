package com.mkuligowski.velocity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Annotation ↔ name suffix consistency.
 *
 * <p>Note: classes that are conceptually adapters but not "repositories" in the DDD sense
 * (idempotency gate, read-only query) use {@code @Component} instead of {@code @Repository}
 * so they don't have to wear an awkward suffix. JdbcTemplate handles SQL-exception
 * translation regardless of stereotype.
 */
class NamingConventionArchTest {

    static final JavaClasses CLASSES = new ClassFileImporter()
            .importPath(Paths.get("build/classes/java/main"));

    @Test
    void services_end_in_service() {
        classes()
                .that().areAnnotatedWith(Service.class)
                .should().haveSimpleNameEndingWith("Service")
                .check(CLASSES);
    }

    @Test
    void repositories_end_in_repository() {
        classes()
                .that().areAnnotatedWith(Repository.class)
                .should().haveSimpleNameEndingWith("Repository")
                .check(CLASSES);
    }

    @Test
    void rest_controllers_end_in_controller() {
        classes()
                .that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .check(CLASSES);
    }
}
