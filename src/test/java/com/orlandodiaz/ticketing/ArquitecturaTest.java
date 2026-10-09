package com.orlandodiaz.ticketing;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Estas reglas hacen que la arquitectura se verifique sola:
 * si alguien rompe el diseno, el build falla.
 */
@AnalyzeClasses(packages = "com.orlandodiaz.ticketing", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_frameworks =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta..", "javax..");

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_aplicacion_ni_infraestructura =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule la_aplicacion_no_depende_de_infraestructura =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    // todavia no hay clases en application; sin esto ArchUnit falla por "regla vacia"
                    .allowEmptyShould(true);
}
