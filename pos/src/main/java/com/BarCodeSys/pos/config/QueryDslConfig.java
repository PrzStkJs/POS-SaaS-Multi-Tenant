/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Punto de Venta
 * DESCRIPCIÓN: Clase de configuración encargada de registrar componentes técnicos necesarios
 *              para el sistema POS.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de QueryDslConfig.

@Configuration
public class QueryDslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}

// Fin de la definición principal de QueryDslConfig.
