/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Punto de Venta
 * DESCRIPCIÓN: Clase principal que inicia la aplicación Spring Boot del sistema POS.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de PosApplication.

@SpringBootApplication
public class PosApplication {

	public static void main(String[] args) {
		SpringApplication.run(PosApplication.class, args);
	}

}

// Fin de la definición principal de PosApplication.
