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

    /**
     * Los modulos no se conocen entre si por dentro: ventas solo puede tocar precios y catalogo
     * a traves de sus adaptadores (infrastructure). Su dominio y su aplicacion hablan con puertos propios.
     */
    @ArchTest
    static final ArchRule ventas_solo_conoce_otros_modulos_desde_sus_adaptadores =
            noClasses().that().resideInAnyPackage("..ventas.domain..", "..ventas.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("..precios..", "..catalogo..");

    /** Ventas publica eventos pero no sabe que facturacion existe. */
    @ArchTest
    static final ArchRule ventas_no_conoce_facturacion =
            noClasses().that().resideInAPackage("..ventas..")
                    .should().dependOnClassesThat().resideInAPackage("..facturacion..");

    /** Facturacion solo conoce a ventas en su adaptador que escucha eventos (infrastructure). */
    @ArchTest
    static final ArchRule facturacion_solo_conoce_otros_modulos_desde_sus_adaptadores =
            noClasses().that().resideInAnyPackage("..facturacion.domain..", "..facturacion.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("..ventas..", "..precios..", "..catalogo..");

    /** Spring (y cualquier framework) queda fuera de la logica de aplicacion: solo en adaptadores y configuracion. */
    @ArchTest
    static final ArchRule la_aplicacion_no_depende_de_spring =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .allowEmptyShould(true);

    /** Ventas publica eventos pero no sabe que accesos existe. */
    @ArchTest
    static final ArchRule ventas_no_conoce_accesos =
            noClasses().that().resideInAPackage("..ventas..")
                    .should().dependOnClassesThat().resideInAPackage("..accesos..");

    /** Accesos solo conoce a ventas en su adaptador que escucha eventos (infrastructure). */
    @ArchTest
    static final ArchRule accesos_solo_conoce_otros_modulos_desde_sus_adaptadores =
            noClasses().that().resideInAnyPackage("..accesos.domain..", "..accesos.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..ventas..", "..precios..", "..catalogo..", "..facturacion..");

    /** Ni ventas ni accesos saben que reportes existe: solo publican eventos. */
    @ArchTest
    static final ArchRule ventas_y_accesos_no_conocen_reportes =
            noClasses().that().resideInAnyPackage("..ventas..", "..accesos..")
                    .should().dependOnClassesThat().resideInAPackage("..reportes..");

    /** Reportes solo conoce a otros modulos en su adaptador que escucha eventos (infrastructure). */
    @ArchTest
    static final ArchRule reportes_solo_conoce_otros_modulos_desde_sus_adaptadores =
            noClasses().that().resideInAnyPackage("..reportes.domain..", "..reportes.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..ventas..", "..accesos..", "..precios..", "..catalogo..", "..facturacion..");
}
