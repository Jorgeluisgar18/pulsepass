# PulsePass

PulsePass es un proyecto académico desarrollado en Java con Spring Boot para la gestión segura de eventos, artistas, venues, usuarios y tickets.

El proyecto se ha construido de manera incremental mediante cuatro fases:

```text
Fase 1
Persistencia
JPA + PostgreSQL + Flyway

        ↓

Fase 2
Servicios
Reglas de negocio + DTOs + transacciones

        ↓

Fase 3
API REST
Controllers + Bean Validation + ErrorResponse

        ↓

Fase 4
Seguridad
Spring Security + JWT + roles + ownership
```

---

## Arquitectura

La aplicación utiliza una arquitectura por capas.

```text
Cliente HTTP
     ↓
Spring Security Filter Chain
     ↓
JwtAuthenticationFilter
     ↓
Controller
     ↓
Service
     ↓
Repository
     ↓
PostgreSQL
```

Las responsabilidades se mantienen separadas:

- Spring Security controla autenticación y autorización HTTP.
- Los Controllers reciben las solicitudes y delegan.
- Los Services contienen reglas de negocio y controles de propiedad.
- Los Repositories administran el acceso a datos.
- PostgreSQL almacena la información persistente.

---

## Tecnologías

- Java 21
- Spring Boot 4
- Spring MVC
- Spring Security 7
- JJWT 0.13
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- Bean Validation
- Maven
- JUnit 5
- Mockito
- MockMvc
- Spring Security Test
- Testcontainers
- AssertJ

---

## Funcionalidades principales

PulsePass permite:

- Consultar venues.
- Consultar artistas.
- Crear eventos.
- Consultar eventos.
- Publicar eventos.
- Asociar artistas a eventos.
- Consultar eventos publicados.
- Consultar eventos por artista.
- Registrar usuarios.
- Consultar usuarios.
- Comprar tickets.
- Consultar tickets.
- Consultar tickets por usuario.
- Consultar tickets pagados de un evento.
- Cancelar tickets.
- Marcar tickets como utilizados.
- Validar edad mínima.
- Controlar capacidad de eventos.
- Cambiar automáticamente un evento a `SOLD_OUT`.
- Validar estados de eventos y tickets.
- Calcular precios de tickets.
- Autenticar usuarios.
- Emitir y validar JWT.
- Autorizar operaciones según roles.
- Proteger recursos mediante ownership.
- Prevenir accesos IDOR.

---

# Seguridad implementada

La Fase 4 incorpora una capa de seguridad utilizando Spring Security y JJWT.

Incluye:

- Registro seguro de usuarios.
- Contraseñas almacenadas utilizando BCrypt.
- Autenticación por username o email.
- JWT firmados utilizando HS256.
- API stateless.
- Autenticación mediante encabezado Bearer.
- Expiración de access tokens.
- Validación del issuer.
- Validación de firma.
- Validación del usuario activo en cada request.
- Recarga de roles actuales desde persistencia.
- Roles `USER`, `STAFF` y `ADMIN`.
- Protección de endpoints por rol.
- Protección de recursos por propiedad.
- Defensa contra IDOR.
- Respuestas uniformes `401` y `403`.
- Protección contra exposición de eventos no publicados.
- Protección contra exposición de contraseñas y hashes.

---

# Modelo de identidad

La entidad `User` continúa siendo la entidad de negocio principal.

Las credenciales y roles se almacenan separadamente.

```text
User
 ├── id
 ├── username
 ├── email
 └── active

UserCredential
 ├── user_id
 ├── password_hash
 └── password_changed_at

UserRole
 ├── user_id
 └── role
```

Los roles disponibles son:

```text
USER
STAFF
ADMIN
```

El registro público asigna exclusivamente:

```text
ROLE_USER
```

Un usuario no puede registrarse públicamente como `STAFF` o `ADMIN`.

Los roles privilegiados deben provisionarse mediante mecanismos administrativos o datos controlados para pruebas.

---

# Registro de usuarios

Endpoint público:

```http
POST /api/users
```

El registro crea atómicamente:

```text
User
+
UserProfile
+
UserCredential con contraseña BCrypt
+
ROLE_USER
```

La contraseña es obligatoria.

Reglas definidas para contraseña:

```text
Mínimo: 12 caracteres
Máximo con BCrypt tradicional: 72 bytes UTF-8
```

Ejemplo:

```json
{
  "username": "andrea",
  "email": "andrea@example.com",
  "firstName": "Andrea",
  "lastName": "Garcia",
  "phone": "3001234567",
  "city": "Santa Marta",
  "birthDate": "1998-05-10",
  "password": "UnaFraseSeguraLarga2026!"
}
```

La respuesta nunca expone:

```text
password
passwordHash
```

El registro no genera automáticamente un token.

El flujo es:

```text
Registro
   ↓
Login
   ↓
JWT
```

---

# Login

Endpoint público:

```http
POST /api/auth/login
```

El usuario puede autenticarse utilizando:

- username;
- email.

Ejemplo:

```json
{
  "identifier": "andrea",
  "password": "UnaFraseSeguraLarga2026!"
}
```

Respuesta:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

Una autenticación incorrecta devuelve:

```text
401 Unauthorized
```

La respuesta es genérica y no revela si:

- el usuario no existe;
- la contraseña es incorrecta.

---

# Usuario autenticado

Endpoint:

```http
GET /api/auth/me
```

Requiere autenticación Bearer.

Devuelve información mínima del principal autenticado.

No devuelve:

```text
password
passwordHash
```

---

# JWT

Los access tokens de PulsePass:

- utilizan firma HS256;
- tienen una duración predeterminada de 15 minutos;
- contienen un `sub` basado en el ID persistente del usuario;
- incluyen `iss`;
- incluyen `iat`;
- incluyen `exp`;
- incluyen `jti`.

El identificador técnico utilizado en `sub` es el ID persistente del usuario.

No se utiliza el email como identidad técnica del JWT.

El token tampoco se considera la fuente definitiva de:

- roles;
- estado activo.

En cada request autenticado se vuelve a consultar persistencia para obtener:

```text
usuario actual
estado active actual
roles actuales
```

Esto permite que un usuario desactivado pierda acceso en su siguiente request aunque todavía posea un JWT no expirado.

---

# Configuración JWT

La configuración principal se encuentra en:

```text
src/main/resources/application.properties
```

Configuración:

```properties
pulsepass.security.jwt.issuer=${PULSEPASS_JWT_ISSUER:pulsepass-api}
pulsepass.security.jwt.secret-base64=${PULSEPASS_JWT_SECRET_BASE64}
pulsepass.security.jwt.access-token-ttl=PT15M
```

Variables de entorno:

| Variable | Obligatoria | Descripción |
|---|---|---|
| `PULSEPASS_JWT_SECRET_BASE64` | Sí | Clave Base64 de al menos 32 bytes aleatorios |
| `PULSEPASS_JWT_ISSUER` | No | Issuer esperado. Por defecto `pulsepass-api` |

Nunca debe almacenarse un secreto real dentro del código fuente o del repositorio.

---

# Generar secreto JWT

En PowerShell se puede generar una clave aleatoria de 32 bytes:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Configurarla para la sesión actual:

```powershell
$env:PULSEPASS_JWT_SECRET_BASE64="CLAVE_GENERADA"
```

Opcionalmente se puede configurar el issuer:

```powershell
$env:PULSEPASS_JWT_ISSUER="pulsepass-api"
```

---

# Autenticación Bearer

Las rutas protegidas requieren:

```http
Authorization: Bearer <token>
```

Un token:

- expirado;
- malformado;
- con firma alterada;
- con issuer incorrecto;

no concede acceso a los recursos protegidos.

---

# API REST y matriz de autorización

## Auth

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/auth/login` | Público |
| GET | `/api/auth/me` | Autenticado |

---

## Venues

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/venues/{code}` | Público |
| GET | `/api/venues/active` | Público |

---

## Artists

| Método | Endpoint | Acceso |
|---|---|---|
| GET | `/api/artists/{id}` | Público |
| GET | `/api/artists/by-stage-name?stageName=...` | Público |
| GET | `/api/artists/active` | Público |

---

## Events

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/events` | ADMIN |
| GET | `/api/events/{eventCode}` | PUBLISHED público; otros estados STAFF/ADMIN |
| GET | `/api/events/published` | Público |
| PATCH | `/api/events/{eventCode}/publish` | ADMIN |
| POST | `/api/events/{eventCode}/artists/{artistId}` | ADMIN |
| GET | `/api/events/by-artist?stageName=...` | Público solo para publicados; STAFF/ADMIN pueden consultar otros estados |
| GET | `/api/events/{eventCode}/tickets/paid` | STAFF / ADMIN |

---

## Users

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/users` | Público |
| GET | `/api/users/by-email?email=...` | Propietario / ADMIN |
| GET | `/api/users/by-username?username=...` | Propietario / ADMIN |

---

## Tickets

| Método | Endpoint | Acceso |
|---|---|---|
| POST | `/api/tickets` | USER, exclusivamente para sí mismo |
| GET | `/api/tickets/{ticketCode}` | Propietario / STAFF / ADMIN |
| GET | `/api/tickets/by-user?email=...` | Propietario / ADMIN |
| PATCH | `/api/tickets/{ticketCode}/cancel` | Propietario USER / ADMIN |
| PATCH | `/api/tickets/{ticketCode}/use` | STAFF / ADMIN |

---

# Protección anti-IDOR

PulsePass no considera suficientes los identificadores suministrados por el cliente para demostrar propiedad.

Por ejemplo:

```text
ticketCode
email
userEmail
```

no representan por sí solos una identidad confiable.

Las validaciones de propiedad utilizan:

```text
principal.userId
```

y lo comparan con el propietario persistido del recurso.

Ejemplo:

```text
JWT Andrea
+
request userEmail = Carlos
=
403 Forbidden
```

Otro ejemplo:

```text
JWT Andrea
+
ticket pertenece a Carlos
=
403 Forbidden
```

La política adoptada para recursos privados ajenos es:

```text
403 Forbidden
```

Para eventos no publicados consultados públicamente se puede ocultar su existencia mediante:

```text
404 Not Found
```

---

# Visibilidad de eventos

Los eventos `PUBLISHED` pueden consultarse públicamente.

Los eventos no publicados no deben filtrarse accidentalmente mediante:

```http
GET /api/events/{eventCode}
```

ni:

```http
GET /api/events/by-artist
```

Para visitantes anónimos y usuarios `USER`, estas consultas únicamente exponen eventos `PUBLISHED`.

Los roles:

```text
STAFF
ADMIN
```

pueden consultar otros estados según las reglas de autorización.

---

# Reglas de negocio

La incorporación de seguridad no reemplaza las reglas de negocio existentes.

Se conservan reglas como:

- Un usuario inactivo no puede comprar tickets.
- Solo pueden comprarse tickets para eventos `PUBLISHED`.
- No pueden comprarse tickets para eventos pasados.
- El usuario debe cumplir la edad mínima del evento.
- No puede superarse la capacidad del venue.
- El último ticket disponible puede cambiar el evento a `SOLD_OUT`.
- Solo un ticket `PAID` puede cancelarse.
- Un ticket `USED` no puede cancelarse.
- Un ticket `CANCELLED` no puede marcarse como usado.
- Un artista no puede asociarse dos veces al mismo evento.
- No pueden agregarse artistas a eventos `CANCELLED` o `FINISHED`.

Los Controllers no implementan directamente estas reglas.

---

# Tipos de ticket

```text
GENERAL
STUDENT
VIP
BACKSTAGE
```

---

# Estados de eventos

```text
DRAFT
PUBLISHED
SOLD_OUT
CANCELLED
FINISHED
```

---

# Estados de tickets

```text
RESERVED
PAID
CANCELLED
USED
```

---

# Manejo de errores

PulsePass mantiene un único contrato de errores:

```java
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
}
```

Códigos utilizados:

| Código | Significado |
|---|---|
| `200 OK` | Operación exitosa |
| `201 Created` | Creación exitosa |
| `400 Bad Request` | Validación o JSON inválido |
| `401 Unauthorized` | No existe identidad válida |
| `403 Forbidden` | Identidad válida sin permiso |
| `404 Not Found` | Recurso inexistente u oculto |
| `409 Conflict` | Regla de negocio o duplicado |
| `500 Internal Server Error` | Error inesperado |

---

# Errores de seguridad

Los errores producidos antes de alcanzar el Controller utilizan:

```text
RestAuthenticationEntryPoint
RestAccessDeniedHandler
```

Los errores producidos dentro del flujo MVC utilizan:

```text
GlobalExceptionHandler
```

Todos mantienen el mismo DTO:

```text
ErrorResponse
```

---

# Migraciones Flyway

La seguridad se incorporó mediante una migración incremental:

```text
V4__security_credentials_roles.sql
```

La migración crea:

```text
user_credentials
user_roles
```

Las migraciones anteriores no se modifican.

Los usuarios existentes que no posean credenciales no reciben una contraseña conocida, compartida o predeterminada.

Esos usuarios permanecen sin capacidad de login hasta una activación segura.

---

# Estructura principal de seguridad

```text
src/main/java/com/pulsepass
│
├── controller
│   ├── AuthController.java
│   ├── VenueController.java
│   ├── EventController.java
│   ├── ArtistController.java
│   ├── UserController.java
│   └── TicketController.java
│
├── security
│   ├── SecurityConfig.java
│   ├── SecurityPrincipal.java
│   ├── CustomUserDetailsService.java
│   ├── JwtProperties.java
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── RestAuthenticationEntryPoint.java
│   └── RestAccessDeniedHandler.java
│
├── service
│   ├── AuthService.java
│   ├── AuthorizationService.java
│   ├── impl
│   └── pricing
│
├── domain
├── repository
├── mapper
├── dto
└── exception
```

---

# Pruebas

El proyecto utiliza diferentes estrategias de pruebas:

- JUnit 5.
- Mockito.
- AssertJ.
- MockMvc.
- Spring Security Test.
- Testcontainers.
- PostgreSQL real para pruebas de persistencia.

Existen pruebas para:

- persistencia;
- migraciones Flyway;
- Services;
- reglas de negocio;
- BCrypt;
- `CustomUserDetailsService`;
- `AuthService`;
- `JwtService`;
- `JwtAuthenticationFilter`;
- handlers `401/403`;
- `AuthorizationService`;
- protección anti-IDOR;
- Controllers;
- matriz HTTP con Spring Security activo.

Los filtros de seguridad no se desactivan para hacer pasar las pruebas MVC.

No se utiliza:

```java
@AutoConfigureMockMvc(addFilters = false)
```

---

# Escenarios de seguridad probados

Entre los escenarios automatizados se encuentran:

```text
Anónimo GET eventos publicados             -> 200

Anónimo POST evento                        -> 401

USER POST evento                           -> 403

ADMIN POST evento                          -> 201

Login correcto                             -> 200 + Bearer

Login incorrecto                           -> 401

JWT expirado                               -> 401

JWT manipulado                             -> 401

USER consulta ticket ajeno                 -> 403

USER compra para otro usuario              -> 403

USER cancela ticket propio                 -> 200

USER intenta usar ticket                   -> 403

STAFF utiliza ticket                       -> 200

ADMIN publica evento                       -> 200

USER consulta datos ajenos                 -> 403

Usuario inactivo con JWT vigente           -> 401

Anónimo consulta evento no publicado       -> no expone datos del borrador

Request inválido con identidad autorizada  -> 400
```

---

# Ejecutar pruebas específicas de seguridad

```bash
mvn "-Dtest=JwtServiceTest,JwtAuthenticationFilterTest,SecurityErrorHandlersTest,AuthorizationServiceTest,SecurityHttpTest" test
```

---

# Ejecutar pruebas de Controllers

```bash
mvn "-Dtest=*ControllerTest" test
```

---

# Ejecutar toda la suite

Docker debe estar activo porque las pruebas de persistencia utilizan Testcontainers.

```bash
mvn clean test
```

Resultado esperado:

```text
Failures: 0
Errors: 0
BUILD SUCCESS
```

---

# Compilación

```bash
mvn clean compile
```

Resultado esperado:

```text
BUILD SUCCESS
```

---

# Ejecutar aplicación

Primero debe configurarse el secreto JWT.

PowerShell:

```powershell
$env:PULSEPASS_JWT_SECRET_BASE64="CLAVE_BASE64_SEGURA"
```

Después:

```bash
mvn spring-boot:run
```

---

# Limitaciones conocidas del MVP

El rol `STAFF` posee acceso operativo global sobre las operaciones autorizadas de tickets.

Actualmente el modelo de dominio no contiene una asociación específica entre personal STAFF y eventos.

Una evolución futura podría modelar una asignación como:

```text
EventStaffAssignment
```

pero dicha funcionalidad no pertenece al alcance de esta fase.

Un access token robado puede continuar siendo utilizado hasta su expiración, salvo que el usuario sea desactivado o sus privilegios cambien en persistencia.

La mitigación actual consiste en:

```text
TTL de 15 minutos
+
HTTPS en producción
+
revalidación del estado del usuario
+
revalidación de roles
```

---


# Autor

**Jorge Luis Garcia Valderrama**

Ingeniería de Sistemas  
Universidad del Magdalena