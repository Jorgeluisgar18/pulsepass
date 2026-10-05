# PulsePass

Proyecto académico desarrollado en Java con Spring Boot para la gestión de eventos, usuarios, artistas, venues y tickets.

Actualmente el proyecto cuenta con una capa de persistencia y una capa de servicios con reglas de negocio, DTOs, mapeo con MapStruct y pruebas unitarias.

## Tecnologías

- Java 21
- Spring Boot 4
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- Maven
- JUnit 5
- Mockito
- AssertJ
- Testcontainers

## Funcionalidades principales

- Gestión de venues.
- Gestión de artistas.
- Creación y publicación de eventos.
- Asociación de artistas a eventos.
- Registro de usuarios y perfiles.
- Compra de tickets.
- Validación de edad mínima.
- Control de capacidad de eventos.
- Cambio automático del evento a `SOLD_OUT`.
- Cancelación de tickets.
- Marcación de tickets como usados.
- Cálculo de precios según el tipo de ticket.

## Tipos de ticket

- GENERAL
- STUDENT
- VIP
- BACKSTAGE

## Estados de eventos

- DRAFT
- PUBLISHED
- SOLD_OUT
- CANCELLED
- FINISHED

## Estados de tickets

- RESERVED
- PAID
- CANCELLED
- USED

## Estructura principal

```text
src/main/java/com/pulsepass
├── domain
├── repository
├── dto
│   ├── request
│   └── response
├── mapper
├── exception
└── service
    ├── impl
    └── pricing
