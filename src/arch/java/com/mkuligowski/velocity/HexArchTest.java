package com.mkuligowski.velocity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Onion architecture: {@code domain ← app ← adapters}. Adapter sub-packages cannot
 * reference each other.
 *
 * <p>Classes are imported by explicit path because JDK 25's {@code AppClassLoader} doesn't
 * expose classpath URLs for ArchUnit's {@code @AnalyzeClasses} to enumerate.
 */
class HexArchTest {

    static final JavaClasses CLASSES = new ClassFileImporter()
            .importPath(Paths.get("build/classes/java/main"));

    @Test
    void domain_does_not_depend_on_app() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.mkuligowski.velocity.loads.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.mkuligowski.velocity.loads.app..");
        rule.check(CLASSES);
    }

    @Test
    void domain_does_not_depend_on_adapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.mkuligowski.velocity.loads.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.mkuligowski.velocity.loads.adapters..");
        rule.check(CLASSES);
    }

    @Test
    void app_does_not_depend_on_adapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.mkuligowski.velocity.loads.app..")
                .should().dependOnClassesThat().resideInAPackage("com.mkuligowski.velocity.loads.adapters..");
        rule.check(CLASSES);
    }

    @Test
    void db_adapter_does_not_depend_on_rest_adapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.mkuligowski.velocity.loads.adapters.db..")
                .should().dependOnClassesThat().resideInAPackage("com.mkuligowski.velocity.loads.adapters.rest..");
        rule.check(CLASSES);
    }

    @Test
    void rest_adapter_does_not_depend_on_db_adapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.mkuligowski.velocity.loads.adapters.rest..")
                .should().dependOnClassesThat().resideInAPackage("com.mkuligowski.velocity.loads.adapters.db..");
        rule.check(CLASSES);
    }
}
