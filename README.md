# PulsePass

Proyecto académico desarrollado en Java con Spring Boot para la gestión de eventos, usuarios, artistas, venues y tickets.

PulsePass está construido mediante una arquitectura por capas:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

Actualmente el proyecto cuenta con:

- Capa de persistencia.
- Capa de servicios y reglas de negocio.
- API REST mediante Controllers.
- DTOs de request y response.
- Bean Validation.
- Manejo global de errores.
- Pruebas unitarias.
- Pruebas de persistencia.
- Pruebas aisladas de Controllers con MockMvc.

## Tecnologías

- Java 21
- Spring Boot 4
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- Bean Validation
- Maven
- JUnit 5
- Mockito
- MockMvc
- Testcontainers
- AssertJ

## Funcionalidades principales

- Gestión y consulta de venues.
- Gestión y consulta de artistas.
- Creación de eventos.
- Consulta de eventos.
- Publicación de eventos.
- Asociación de artistas a eventos.
- Consulta de eventos publicados.
- Consulta de eventos por artista.
- Registro de usuarios.
- Consulta de usuarios por email.
- Consulta de usuarios por username.
- Compra de tickets.
- Consulta de tickets por código.
- Consulta de tickets por usuario.
- Consulta de tickets pagados por evento.
- Cancelación de tickets.
- Marcación de tickets como usados.
- Validación de edad mínima.
- Control de capacidad de eventos.
- Cambio automático del evento a `SOLD_OUT`.
- Validación de estados de eventos y tickets.
- Cálculo de precios según el tipo de ticket.
- Validación estructural de requests.
- Manejo uniforme de errores HTTP.

## API REST

La aplicación expone las funcionalidades de la capa Service mediante Controllers REST.

### Venues

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/venues/{code}` | Consultar venue por código |
| GET | `/api/venues/active` | Consultar venues activos |

### Events

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/events` | Crear un evento |
| GET | `/api/events/{eventCode}` | Consultar evento por código |
| GET | `/api/events/published` | Consultar eventos publicados |
| PATCH | `/api/events/{eventCode}/publish` | Publicar un evento |
| POST | `/api/events/{eventCode}/artists/{artistId}` | Asociar un artista a un evento |
| GET | `/api/events/by-artist?stageName=...` | Consultar eventos por artista |
| GET | `/api/events/{eventCode}/tickets/paid` | Consultar tickets pagados de un evento |

### Artists

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/artists/{id}` | Consultar artista por ID |
| GET | `/api/artists/by-stage-name?stageName=...` | Consultar artista por nombre artístico |
| GET | `/api/artists/active` | Consultar artistas activos |

### Users

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/users` | Registrar un usuario |
| GET | `/api/users/by-email?email=...` | Consultar usuario por email |
| GET | `/api/users/by-username?username=...` | Consultar usuario por username |

### Tickets

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/tickets` | Comprar un ticket |
| GET | `/api/tickets/{ticketCode}` | Consultar ticket por código |
| GET | `/api/tickets/by-user?email=...` | Consultar tickets por usuario |
| PATCH | `/api/tickets/{ticketCode}/cancel` | Cancelar un ticket |
| PATCH | `/api/tickets/{ticketCode}/use` | Marcar un ticket como usado |

## Cobertura Service → HTTP

La capa Controller expone todos los métodos públicos definidos en los Services.

```text
VenueService      2 operaciones
EventService      6 operaciones
ArtistService     3 operaciones
UserService       3 operaciones
TicketService     6 operaciones
                 ──────────────
TOTAL            20 operaciones
```

## Validación de requests

La API utiliza Bean Validation para validar los datos estructurales antes de invocar la capa Service.

### CreateEventRequest

Validaciones principales:

- `eventCode`: obligatorio.
- `name`: obligatorio.
- `description`: máximo 1000 caracteres.
- `category`: obligatorio.
- `eventDate`: obligatorio.
- `minimumAge`: obligatorio y mayor o igual a 0.
- `venueCode`: obligatorio.

### RegisterUserRequest

Validaciones principales:

- `username`: obligatorio.
- `email`: obligatorio y formato de email válido.
- `firstName`: obligatorio.
- `lastName`: obligatorio.
- `birthDate`: obligatorio.

### PurchaseTicketRequest

Validaciones principales:

- `userEmail`: obligatorio y formato de email válido.
- `eventCode`: obligatorio.
- `type`: obligatorio.

Las reglas de negocio permanecen en la capa Service.

## Códigos HTTP

La API utiliza los siguientes códigos HTTP:

| Código | Descripción |
|---|---|
| `200 OK` | Consulta o actualización exitosa |
| `201 Created` | Creación exitosa de evento, usuario o ticket |
| `400 Bad Request` | Request inválido o JSON mal formado |
| `404 Not Found` | Recurso inexistente |
| `409 Conflict` | Duplicado o regla de negocio incumplida |
| `500 Internal Server Error` | Error inesperado |

## Manejo global de errores

El manejo de errores está centralizado mediante:

```java
@RestControllerAdvice
```

La clase responsable es:

```text
GlobalExceptionHandler
```

Todos los errores HTTP utilizan el DTO:

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

### Ejemplo de error 404

```json
{
  "timestamp": "2026-10-07T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Event not found: CMF-2026",
  "details": {}
}
```

### Ejemplo de error de validación

```json
{
  "timestamp": "2026-10-07T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "eventCode": "Event code is required",
    "venueCode": "Venue code is required"
  }
}
```

## Mapeo de excepciones

| Excepción o condición | Código HTTP |
|---|---|
| `MethodArgumentNotValidException` | `400 Bad Request` |
| JSON mal formado | `400 Bad Request` |
| `ResourceNotFoundException` | `404 Not Found` |
| `DuplicateResourceException` | `409 Conflict` |
| `BusinessRuleException` | `409 Conflict` |
| Error inesperado | `500 Internal Server Error` |

## Reglas principales de negocio

La capa Service contiene las reglas de negocio del sistema.

Entre ellas:

- Un usuario inactivo no puede comprar tickets.
- Solo se pueden comprar tickets para eventos `PUBLISHED`.
- No se pueden comprar tickets para eventos pasados.
- El usuario debe cumplir la edad mínima del evento.
- No puede superarse la capacidad del venue.
- El último ticket disponible puede cambiar el evento a `SOLD_OUT`.
- Solo un ticket `PAID` puede cancelarse.
- Un ticket `USED` no puede cancelarse.
- Un ticket `CANCELLED` no puede marcarse como usado.
- Un artista no puede asociarse dos veces al mismo evento.
- No pueden agregarse artistas a eventos `CANCELLED` o `FINISHED`.

Los Controllers no implementan estas reglas directamente.

## Tipos de ticket

- `GENERAL`
- `STUDENT`
- `VIP`
- `BACKSTAGE`

## Estados de eventos

- `DRAFT`
- `PUBLISHED`
- `SOLD_OUT`
- `CANCELLED`
- `FINISHED`

## Estados de tickets

- `RESERVED`
- `PAID`
- `CANCELLED`
- `USED`

## Estructura principal

```text
src/main/java/com/pulsepass
├── controller
│   ├── VenueController.java
│   ├── EventController.java
│   ├── ArtistController.java
│   ├── UserController.java
│   └── TicketController.java
│
├── domain
│
├── repository
│
├── dto
│   ├── request
│   │   ├── CreateEventRequest.java
│   │   ├── RegisterUserRequest.java
│   │   └── PurchaseTicketRequest.java
│   │
│   └── response
│       ├── VenueResponse.java
│       ├── EventResponse.java
│       ├── EventSummaryResponse.java
│       ├── ArtistResponse.java
│       ├── UserResponse.java
│       ├── TicketResponse.java
│       └── ErrorResponse.java
│
├── mapper
│
├── exception
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── BusinessRuleException.java
│   └── GlobalExceptionHandler.java
│
└── service
    ├── impl
    └── pricing
```

## Pruebas

El proyecto utiliza diferentes estrategias de pruebas según la capa.

### Pruebas de persistencia

Las pruebas de persistencia utilizan:

- PostgreSQL real.
- Testcontainers.
- Docker.

Docker debe estar activo para ejecutar estas pruebas.

### Pruebas de Service

Las pruebas de Service utilizan principalmente:

- JUnit 5.
- Mockito.
- AssertJ.

### Pruebas de Controller

Las pruebas de Controller utilizan:

- JUnit 5.
- Mockito.
- MockMvc.
- `@WebMvcTest`.
- `@MockitoBean`.
- `jsonPath`.
- `verify(...)`.
- `verify(..., never())`.

Cada Controller posee su propia clase de pruebas:

```text
src/test/java/com/pulsepass/controller
├── VenueControllerTest.java
├── EventControllerTest.java
├── ArtistControllerTest.java
├── UserControllerTest.java
└── TicketControllerTest.java
```

La capa Controller cuenta con más de 32 casos de prueba que validan los contratos HTTP requeridos.

Se prueban respuestas:

```text
200 OK
201 Created
400 Bad Request
404 Not Found
409 Conflict
500 Internal Server Error
```

También se valida:

- Content-Type.
- JSON retornado.
- Bean Validation.
- ErrorResponse.
- Interacción con Services.
- Que requests inválidos no invoquen la capa Service.

## Ejecutar pruebas de Controller

Para ejecutar únicamente las pruebas de Controller:

```bash
mvn "-Dtest=*ControllerTest" test
```

## Ejecutar todas las pruebas

Para ejecutar toda la suite del proyecto:

```bash
mvn clean test
```

Para que las pruebas de persistencia funcionen correctamente, Docker debe estar activo.

El resultado esperado es:

```text
BUILD SUCCESS
```

## Compilación

Para compilar el proyecto:

```bash
mvn clean compile
```

El resultado esperado es:

```text
BUILD SUCCESS
```

## Ejecución de la aplicación

Para ejecutar la aplicación Spring Boot:

```bash
mvn spring-boot:run
```

## Arquitectura

La comunicación entre capas sigue la siguiente dirección:

```text
HTTP Request
     ↓
Controller
     ↓
DTO
     ↓
Service
     ↓
Repository
     ↓
PostgreSQL
```

Los Controllers:

- No acceden directamente a Repository.
- No implementan reglas de negocio.
- No retornan entidades JPA.
- Utilizan DTOs.
- Retornan `ResponseEntity`.
- Delegan las operaciones a los Services.

## Alcance actual

El proyecto se encuentra actualmente en la fase de implementación de la API REST mediante Spring MVC.

Incluye:

- Persistencia.
- Services.
- Controllers REST.
- Bean Validation.
- Manejo global de errores.
- Pruebas con MockMvc.

## Fuera de alcance

Para esta fase no se implementan:

- Spring Security.
- Autenticación.
- Autorización.
- JWT.
- OAuth2.
- Frontend.
- Pasarela de pagos reales.
- Mensajería.
- Notificaciones.
- OpenAPI / Swagger.
- Pruebas E2E.
- Despliegue productivo.
- Paginación.
- Rate limiting.

## Autor

**Jorge Luis Garcia Valderrama**

Ingeniería de Sistemas  
Universidad del Magdalena