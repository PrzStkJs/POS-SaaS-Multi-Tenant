<div align="center">
  <img src="https://cdn-icons-png.flaticon.com/512/5809/5809896.png" alt="SaaS POS Logo" width="140" height="140" />
  <h1>🛒 Multi-Tenant SaaS POS System + AI</h1>
  <p><b>Sistema de Punto de Venta Inteligente, Multi-Empresa y Resiliente Offline</b></p>

  [![Java](https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green.svg?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
  [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-blue.svg?style=for-the-badge&logo=postgresql)](https://supabase.com/)
  [![Gemini AI](https://img.shields.io/badge/Google_Gemini-AI_Engine-4285F4.svg?style=for-the-badge&logo=google)](https://ai.google.dev/)
  [![License](https://img.shields.io/badge/License-MIT-brightgreen.svg?style=for-the-badge)](LICENSE)
</div>

---

## 📌 Descripción General

Este proyecto es una plataforma **SaaS Multi-Tenant** de Punto de Venta (POS) e inventarios diseñada para resolver los problemas operativos reales de pequeñas y medianas empresas (bodegas, minimarkets, farmacias). 

Integra en una sola aplicación el control de caja en tiempo real, gestión estricta de inventarios, cobros con pasarelas digitales mediante QR, un motor de **Inteligencia Artificial con Google Gemini** para asistencia ejecutiva/OCR y resiliencia mediante un **modo offline parcial**.

---

## ✨ Características Principales

### 🏢 Arquitectura Multi-Empresa & Control de Acceso (RBAC)
* **Aislamiento Multi-Tenant:** Garantiza que los datos de cada cliente pertenezcan únicamente a su organización mediante filtrado estricto por `empresaId`.
* **Roles Granulares:**
  * `SUPER_ADMIN`: Gestión global de empresas registradas y métricas del SaaS.
  * `ADMIN`: Control total de sucursales, productos, usuarios, categorías y finanzas.
  * `CAJERO`: Interfaz optimizada para agilidad en caja y registro de ventas.

### 💳 Caja, Ventas & Pasarelas de Pago
* **Punto de Venta Dinámico:** Emisión rápida de comprobantes, control de stock en tiempo real y devoluciones con ajuste automático de inventario.
* **Integración de Pagos QR:** Módulos listos para cobros mediante **Mercado Pago** y **Niubiz**.
* **Confirmación Automatizada:** Procesamiento asíncrono de pagos a través de **Webhooks de Mercado Pago**.
* **Reportes & Exportaciones:** Dashboard interactivo con KPIs, gráficos de ventas, productos críticos e informes descargables en formato **Excel / CSV**.

### 🤖 Inteligencia Artificial Integrada (Google Gemini)
* **Escaneo OCR de Facturas:** Carga automática de mercancía y facturas mediante visión por computadora.
* **Analítica & Reportes Ejecutivos:** Resúmenes inteligentes de rendimiento financiero y predicción de agotamiento de inventario.
* **Cross-Selling Recomendado:** Sugerencias automáticas de promociones y productos complementarios para el cajero.

### 📶 Resiliencia & Modo Offline Parcial
* **Sincronización Transaccional:** Si la conexión a internet falla, el sistema almacena localmente las ventas en efectivo utilizando `localStorage`.
* **Auto-Sync:** Al reestablecer el enlace online, el sistema ejecuta background-jobs para sincronizar las transacciones pendientes sin perder integridad de datos.

---

## 🛡️ Seguridad & Buenas Prácticas

El sistema fue diseñado bajo estrictos estándares de ciberseguridad y arquitectura limpia:

| Medida de Seguridad | Descripción |
| :--- | :--- |
| **Protección Multi-Tenant** | Queries interceptadas para garantizar que solo se consulten datos del `empresaId` autenticado. |
| **Gestión de Secretos** | Credenciales aisladas del código mediante variables de entorno (`application.properties`). |
| **Seguridad de Capas** | Autenticación y cifrado de contraseñas con **BCrypt** y **Spring Security**. |
| **Mitigación de Vulnerabilidades** | Protección activada contra ataques **XSS** (sanitización de renderizado JS) y **CSRF**. |

---

## 🛠️ Stack Tecnológico

### Backend
* **Lenguaje & Framework:** Java 17 | Spring Boot 3.3
* **Seguridad:** Spring Security, BCrypt, Webhooks HMAC Verification
* **Persistencia:** Spring Data JPA, Hibernate, QueryDSL
* **Base de Datos:** PostgreSQL en Supabase
* **Documentación & Tools:** OpenAPI / Swagger (springdoc), Maven, ZXing (QR Generator)

### Frontend
* **Core:** Thymeleaf, JavaScript Modular (ES6+), HTML5, CSS3
* **UI Framework:** Bootstrap 5, Bootstrap Icons, SweetAlert2
* **Visualización & Data:** Chart.js, Flatpickr, ExcelJS, XLSX, FileSaver

---

## ✉️ Contáctame

¿Te interesa ver una **demo en vivo** del sistema o discutir oportunidades de desarrollo? ¡Hablemos!

* 📧 **Correo Electrónico:** [jesu123456789pere@gmail.com](mailto:jesu123456789pere@gmail.com)
