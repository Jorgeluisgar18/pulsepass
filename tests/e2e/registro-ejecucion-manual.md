# Registro de ejecución manual E2E — PulsePass

## Datos de la ejecución

**Run ID:** `manual01`
**Fecha:** 17/10/2026
**Base URL:** `http://localhost:8080`
**Base de datos:** `pulsepass_e2e`
**Herramienta:** Postman Desktop
**Tipo de ejecución:** Manual, request por request mediante `Send`

---

## Convención de resultados

- `PASS`: comportamiento observado coincide con el esperado.
- `FAIL`: HTTP, JSON o efecto de negocio/seguridad no coincide.
- `BLOCKED`: no fue posible ejecutar por ausencia de una precondición.
- `NOT RUN`: caso no ejecutado.

No se almacenan en este documento passwords, JWT completos, hashes BCrypt ni secretos de configuración.

---

# 00 — Preparación / conectividad

| Caso | Fecha/Hora | Actor | Operación | Esperado | Observado | Resultado | Observación |
|---|---|---|---|---:|---:|---|---|
| PRE-001 | 17/10/2026 12:04 PM | ANONYMOUS | GET `/api/venues/active` | 200 | 200 | PASS | Conectividad HTTP real confirmada. Se retornaron los dos venues E2E activos. |

---

# 01 — Venues

| Caso | Fecha/Hora | Actor | Operación | Esperado | Observado | Resultado | Observación |
|---|---|---|---|---:|---:|---|---|
| E2E-VEN-01 | 17/10/2026 12:04 PM | ANONYMOUS | GET `/api/venues/{code}` | 200 | 200 | PASS | `E2E-VENUE-001`, capacidad 100, `active=true`. |
| E2E-VEN-02 | 17/10/2026 | ANONYMOUS | GET `/api/venues/active` | 200 | 200 | PASS | Se retornaron `E2E-VENUE-001` y `E2E-VENUE-SMALL-001`, ambos activos. |
| TC-HTTP-VEN-404 | 17/10/2026 12:05 PM | ANONYMOUS | GET venue inexistente | 404 | 404 | PASS | `ErrorResponse` correcto: `Not Found`, details vacío. |

---

# 02 — Artists

| Caso | Fecha/Hora | Actor | Operación | Esperado | Observado | Resultado | Observación |
|---|---|---|---|---:|---:|---|---|
| E2E-ART-01 | 17/10/2026 12:05 PM | ANONYMOUS | GET `/api/artists/{id}` | 200 | 200 | PASS | Artista ID 1 = `Solar Beat`, activo. |
| E2E-ART-02 | 17/10/2026 12:06 PM | ANONYMOUS | GET `/api/artists/by-stage-name` | 200 | 200 | PASS | Retornó `Solar Beat` correctamente. |
| E2E-ART-03 | 17/10/2026 12:07 PM | ANONYMOUS | GET `/api/artists/active` | 200 | 200 | PASS | Se retornaron los cinco artistas activos del fixture inicial. |
| TC-HTTP-ART-404 | 17/10/2026 12:07 PM | ANONYMOUS | GET artista inexistente | 404 | 404 | PASS | `ErrorResponse` correcto para ID inexistente. |

---

# 03 — Auth / usuarios

| Caso | Fecha/Hora | Actor | Operación | Esperado | Observado | Resultado | Observación |
|---|---|---|---|---:|---:|---|---|
| TC-AUTH-001A | 17/10/2026 12:08 PM | ANONYMOUS | Registrar USER_A | 201 | 201 | PASS | USER_A creado activo. La respuesta no expuso password ni hash. |
| TC-AUTH-001B | 17/10/2026 12:08 PM | ANONYMOUS | Registrar USER_B | 201 | 201 | PASS | USER_B creado activo correctamente. |
| TC-AUTH-002 | 17/10/2026 12:09 PM | USER_A | Login mediante username | 200 | 200 | PASS | `accessToken` presente, `tokenType=Bearer`, `expiresIn=900`. |
| TC-AUTH-003 | 17/10/2026 12:09 PM | USER_A | Login mediante email | 200 | 200 | PASS | `accessToken` presente, `tokenType=Bearer`, `expiresIn=900`. |
| TC-AUTH-B-LOGIN | 17/10/2026 12:10 PM | USER_B | Login USER_B | 200 | 200 | PASS | JWT emitido correctamente. |
| TC-AUTH-STAFF | 17/10/2026 12:10 PM | STAFF | Login STAFF | 200 | 200 | PASS | JWT emitido correctamente para fixture STAFF. |
| TC-AUTH-ADMIN | 17/10/2026 12:11 PM | ADMIN | Login ADMIN | 200 | 200 | PASS | JWT emitido correctamente para fixture ADMIN. |
| TC-AUTH-INACTIVE-LOGIN | 17/10/2026 12:12 PM | INACTIVE_USER | Login usuario inactivo | 401 | 401 | PASS | Respuesta genérica `Invalid credentials`; no se filtró el motivo específico. |
| TC-AUTH-006 | 17/10/2026 12:14 PM | USER_A | GET `/api/auth/me` | 200 | 200 | PASS | Identidad correcta: USER_A con rol `USER`. |
| TC-AUTH-004 | 17/10/2026 12:14 PM | ANONYMOUS | Password incorrecto | 401 | 401 | PASS | Mensaje genérico `Invalid credentials`. |
| TC-AUTH-005 | 17/10/2026 12:15 PM | ANONYMOUS | Identificador inexistente | 401 | 401 | PASS | Mismo contrato genérico que password incorrecto. |

---

# 04 — Usuarios / consulta y propiedad

| Caso | Fecha/Hora | Actor | Operación | Esperado | Observado | Resultado | Observación |
|---|---|---|---|---:|---:|---|---|
| E2E-USR-02 | 17/10/2026 12:15 PM | USER_A | GET usuario propio por email | 200 | 200 | PASS | Retornó exclusivamente los datos permitidos de USER_A. |
| E2E-USR-03 | 17/10/2026 12:16 PM | USER_A | GET usuario propio por username | 200 | 200 | PASS | Retornó la misma identidad persistida. |
| TC-SEC-ADMIN-USR | 17/10/2026 12:17 PM | ADMIN | Consultar USER_A por email | 200 | 200 | PASS | ADMIN pudo consultar al usuario sin exposición de credenciales. |
| TC-SEC-006 | 17/10/2026 12:17 PM | USER_B | Consultar email de USER_A | 403 | 403 | PASS | Protección horizontal aplicada. |
| TC-SEC-007 | 17/10/2026 12:17 PM | USER_B | Consultar username de USER_A | 403 | 403 | PASS | Protección horizontal aplicada. |

---

# 05 — Events / administración y visibilidad

| Caso | Fecha/Hora | Actor | Operación | Esperado | Observado | Resultado | Observación |
|---|---|---|---|---:|---:|---|---|
| E2E-EVT-01 | 17/10/2026 12:21 PM | ADMIN | POST `/api/events` | 201 | 201 | PASS | Evento `E2E-CREATED-manual01` creado inicialmente en estado `DRAFT`. |
| E2E-EVT-02A | 17/10/2026 12:21 PM | ADMIN | GET evento DRAFT | 200 | 200 | PASS | ADMIN pudo consultar el DRAFT. |
| TC-SEC-004 | 17/10/2026 12:22 PM | ANONYMOUS | GET mismo DRAFT | 404 | 404 | PASS | El borrador no fue revelado públicamente. |
| E2E-EVT-05 | 17/10/2026 12:22 PM | ADMIN | Asociar artista | 200 | 200 | PASS | `Solar Beat` quedó asociado al evento. |
| E2E-EVT-04 | 17/10/2026 12:23 PM | ADMIN | PATCH publish | 200 | 200 | PASS | Estado cambió persistentemente a `PUBLISHED`. |
| E2E-EVT-02B | 17/10/2026 12:23 PM | ANONYMOUS | GET evento publicado | 200 | 200 | PASS | Evento pasó a ser visible públicamente. |
| E2E-EVT-03 | 17/10/2026 12:24 PM | ANONYMOUS | GET `/api/events/published` | 200 | 200 | PASS | Solo se observaron eventos `PUBLISHED`; no apareció el DRAFT. |
| E2E-EVT-06 | 17/10/2026 12:25 PM | ANONYMOUS | GET eventos por artista | 200 | 200 | PASS | Público observó únicamente eventos publicados de `Solar Beat`. |
| TC-SEC-005-OPS | 17/10/2026 12:26 PM | STAFF | GET eventos por artista | 200 | 200 | PASS | STAFF pudo observar también `E2E-DRAFT-001`. |

---

# 06 — Tickets / compra, consulta y ciclo de vida

| Caso | Fecha/Hora | Actor | Operación | Esperado | Observado | Resultado | Observación |
|---|---|---|---|---:|---:|---|---|
| E2E-TKT-01A | 17/10/2026 12:28 PM | USER_A | Comprar T1 GENERAL | 201 | 201 | PASS | Ticket creado con precio 120000 y estado `PAID`. |
| E2E-TKT-02 | 17/10/2026 12:29 PM | USER_A | Consultar T1 | 200 | 200 | PASS | Propietario pudo consultar T1 en estado `PAID`. |
| TC-SEC-009 | 17/10/2026 12:49 PM | USER_B | Consultar T1 de USER_A | 403 | 403 | PASS | USER_B no obtuvo el recurso ajeno. |
| E2E-TKT-03 | 17/10/2026 12:54 PM | USER_A | Listar tickets propios | 200 | 200 | PASS | La lista incluyó T1 de USER_A. |
| TC-SEC-010 | 17/10/2026 1:47 PM | USER_B | Listar tickets de USER_A | 403 | 403 | PASS | Reejecución final confirmó protección horizontal con `403`. |
| TC-SEC-011 | 17/10/2026 12:59 PM | USER_B | Cancelar T1 de USER_A | 403 | 403 | PASS | Mutación ajena denegada. |
| TC-SEC-011-CHECK | 17/10/2026 1:00 PM | USER_A | Releer T1 | 200 | 200 | PASS | T1 continuó `PAID`; la denegación anterior no produjo mutación. |
| E2E-TKT-05 | 17/10/2026 1:01 PM | USER_A | Cancelar T1 | 200 | 200 | PASS | T1 cambió a `CANCELLED`. |
| TC-BIZ-011 | 17/10/2026 1:01 PM | USER_A | Segunda cancelación | 409 | 409 | PASS | Transición inválida rechazada: solo tickets `PAID` pueden cancelarse. |
| E2E-TKT-01B | 17/10/2026 1:01 PM | USER_A | Comprar T2 VIP | 201 | 201 | PASS | T2 creado con precio 250000 y estado `PAID`. |
| E2E-TKT-04 | 17/10/2026 1:02 PM | STAFF | GET tickets PAID del evento | 200 | 200 | PASS | Se retornó T2; T1 cancelado quedó excluido. |
| TC-SEC-012 | 17/10/2026 1:03 PM | USER_A | Intentar usar T2 | 403 | 403 | PASS | USER no tiene permiso para marcar tickets como usados. |
| TC-SEC-012-CHECK | 17/10/2026 1:04 PM | USER_A | Releer T2 | 200 | 200 | PASS | T2 permaneció `PAID`; no hubo mutación tras el 403. |
| E2E-TKT-06 | 17/10/2026 1:04 PM | STAFF | Usar T2 | 200 | 200 | PASS | T2 cambió a `USED`. |
| TC-BIZ-013 | 17/10/2026 1:05 PM | STAFF | Segundo uso T2 | 409 | 409 | PASS | Segunda transición a USED rechazada. |

---

# 07 — Seguridad / escenarios negativos

| Caso | Fecha/Hora | Actor | Operación | Esperado | Observado | Resultado | Observación |
|---|---|---|---|---:|---:|---|---|
| TC-SEC-001A | 17/10/2026 1:06 PM | ANONYMOUS | Crear evento sin JWT | 401 | 401 | PASS | Autenticación requerida antes de acceder a operación ADMIN. |
| TC-SEC-001B | 17/10/2026 1:06 PM | USER_A | Crear evento con USER | 403 | 403 | PASS | USER autenticado carece de privilegio ADMIN. Este caso cubre también la matriz USER → endpoint ADMIN. |
| TC-SEC-002A | 17/10/2026 1:07 PM | USER_A | Publicar evento | 403 | 403 | PASS | USER no puede publicar. |
| TC-SEC-002B | 17/10/2026 1:07 PM | STAFF | Publicar evento | 403 | 403 | PASS | STAFF no puede publicar. |
| TC-SEC-003 | 17/10/2026 1:08 PM | USER_A | Asociar artista | 403 | 403 | PASS | Operación administrativa bloqueada. |
| TC-SEC-008 | 17/10/2026 1:08 PM | USER_A | Comprar para email de USER_B | 403 | 403 | PASS | Anti-IDOR aplicado en compra. |
| TC-SEC-013 | 17/10/2026 1:09 PM | USER_A | Consultar tickets PAID | 403 | 403 | PASS | Endpoint reservado a operaciones STAFF/ADMIN. |
| TC-AUTH-007 | 17/10/2026 1:09 PM | ANONYMOUS | `/api/auth/me` sin Authorization | 401 | 401 | PASS | Bearer requerido. |
| TC-AUTH-008 | 17/10/2026 1:10 PM | ANONYMOUS | Bearer malformado | 401 | 401 | PASS | JWT inválido rechazado. |
| TC-AUTH-009 | 17/10/2026 1:11 PM | USER_A | JWT alterado | 401 | 401 | PASS | Token manipulado rechazado. |
| TC-AUTH-010 | 17/10/2026 1:12 PM | USER_A | JWT expirado | 401 | 401 | PASS | Acceso rechazado conforme al escenario de expiración. |
| TC-AUTH-011 | 17/10/2026 1:12 PM | USER_A | JWT con clave/issuer ajeno | 401 | 401 | PASS | Token no perteneciente al contexto esperado rechazado. |
| TC-AUTH-012 | 17/10/2026 1:13 PM | USER_A | JWT por query string | 401 | 401 | PASS | El servidor no autenticó mediante parámetro query. |
| TC-AUTH-015 | 17/10/2026 1:13 PM | ANONYMOUS | JWT inválido en endpoint público | 401 | 401 | PASS | Política implementada: Bearer inválido es rechazado por el filtro aun en ruta pública. |
| TC-SEC-016 | 17/10/2026 1:15 PM | ANONYMOUS | Registro incluyendo `role=ADMIN` | 201 | 201 | PASS | Registro aceptado pero el campo inyectado no otorgó privilegios. |
| TC-SEC-015 | 17/10/2026 1:15 PM | ADMIN | Consultar fixture DRAFT | 200 | 200 | PASS | ADMIN obtuvo el DRAFT autorizado. |
| TC-SEC-016B | 17/10/2026 1:17 PM | Usuario de escalación | Login posterior | 200 | 200 | PASS | Login correcto; se obtuvo JWT sin exponerlo en la evidencia final. |
| TC-SEC-016C | 17/10/2026 1:17 PM | Usuario de escalación | GET `/api/auth/me` | 200 | 200 | PASS | La autoridad observada fue únicamente `USER`; no hubo escalación a ADMIN. |
| TC-AUTH-013 | 17/10/2026 1:18 PM | INACTIVE_USER | Token previo tras inactivación | 401 | 401 | PASS | Usuario inactivo no pudo utilizar el token previo. |
| TC-SEC-017 | 17/10/2026 1:55 PM | ADMIN con rol revocado | Operación ADMIN usando JWT antiguo | 403 | 403 | PASS | Reejecución definitiva: al retirar ADMIN en BD, el JWT antiguo perdió privilegios en la siguiente solicitud. |

---

# 08 — Validaciones y errores HTTP

| Caso | Fecha/Hora | Actor | Operación | Esperado | Observado | Resultado | Observación |
|---|---|---|---|---:|---:|---|---|
| TC-HTTP-400-EMAIL | 17/10/2026 1:35 PM | ANONYMOUS | Registro email inválido | 400 | 400 | PASS | `Validation failed`; details identifica `email`. |
| TC-HTTP-400-JSON | 17/10/2026 1:36 PM | ANONYMOUS | JSON malformado | 400 | 400 | PASS | Contrato uniforme `Malformed or invalid JSON request`. |
| TC-HTTP-400-ENUM | 17/10/2026 1:38 PM | ADMIN | EventCategory inválido | 400 | 400 | PASS | Enum no válido rechazado por deserialización. |
| TC-BIZ-006-DUP | 17/10/2026 1:38 PM | ANONYMOUS | Registro duplicado USER_A | 409 | 409 | PASS | Conflicto de unicidad detectado. |
| TC-HTTP-EVT-404 | 17/10/2026 1:38 PM | ANONYMOUS | Evento inexistente | 404 | 404 | PASS | `Event not found` con ErrorResponse. |
| TC-BIZ-014-DRAFT | 17/10/2026 1:39 PM | USER_A | Comprar para evento DRAFT | 409 | 409 | PASS | Solo se permite compra en eventos PUBLISHED. |
| TC-BIZ-014-AGE | 17/10/2026 1:40 PM | USER_A | Compra sin edad mínima | 409 | 409 | PASS | Regla de edad aplicada. |
| TC-BIZ-014-CAP-1 | 17/10/2026 1:40 PM | USER_A | Primera compra en capacidad 1 | 201 | 201 | PASS | Ticket PAID creado; se ocupó el único cupo. |
| TC-BIZ-014-CAP-2 | 17/10/2026 1:41 PM | USER_B | Segunda compra sin capacidad | 409 | 409 | PASS | Segunda compra rechazada; no se excedió la capacidad. |

---

# Casos de la matriz cubiertos por equivalencia

## USER sobre endpoint ADMIN

No existía en la colección importada un request independiente denominado `TC-AUTH-014`.

El requisito queda cubierto por:

```text
TC-SEC-001B — Crear evento con USER
Resultado: 403 Forbidden
```

Este caso demuestra que un usuario `USER` autenticado no puede ejecutar una operación exclusiva de `ADMIN`.

## Validación 400 por request inválido

No existía en la colección importada un request independiente `TC-HTTP-400-REQUIRED`.

La cobertura HTTP `400` fue demostrada mediante:

```text
TC-HTTP-400-EMAIL → 400
TC-HTTP-400-JSON  → 400
TC-HTTP-400-ENUM  → 400
```

No se registra como ejecutado un request que no estaba presente en la colección real.

---

# ErrorResponse

Durante la ejecución se verificaron respuestas con los cinco campos establecidos:

```text
timestamp
status
error
message
details
```

Se observaron correctamente respuestas:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
```

No se observaron stack traces, passwords, hashes BCrypt ni secretos de configuración en las respuestas.

---

# Verificación de persistencia e integridad

Se confirmó manualmente:

- creación de USER_A y USER_B;
- creación de evento en estado DRAFT;
- persistencia de asociación con `Solar Beat`;
- publicación del evento y posterior acceso público;
- ocultamiento de DRAFT a actores públicos;
- compra de T1 en estado `PAID`;
- denegación de cancelación ajena sin modificar T1;
- cancelación de T1 a `CANCELLED`;
- rechazo de segunda cancelación;
- compra independiente de T2;
- denegación de uso por USER sin modificar T2;
- transición de T2 a `USED` por STAFF;
- rechazo del segundo uso;
- protección anti-IDOR;
- rechazo de compra para email ajeno;
- control de edad;
- control de capacidad;
- rechazo de escalación de privilegios;
- pérdida efectiva de privilegios ADMIN tras revocación del rol utilizando un JWT previamente emitido.

---

# Incidencias de ejecución corregidas

Durante la ejecución se repitieron dos escenarios debido a condiciones de preparación de la prueba:

### TC-SEC-010

Una ejecución preliminar no representó correctamente el escenario previsto.

La reejecución definitiva produjo:

```text
403 Forbidden
```

y se utiliza este resultado como evidencia final.

### TC-SEC-017

Un intento preliminar alcanzó una condición de negocio relacionada con un código de evento previamente existente.

Se corrigió la precondición, se retiró el rol ADMIN del usuario en la base E2E y se utilizó el JWT emitido previamente.

Resultado definitivo:

```text
403 Forbidden
```

No se considera defecto del producto.

---

# Defectos encontrados

No se identificaron defectos funcionales confirmados durante la ejecución final.

| ID | Caso | Descripción | Estado |
|---|---|---|---|
| — | — | No se identificaron defectos confirmados. | — |

---

# Consolidación final

```text
Requests ejecutados: 77

PASS:     77
FAIL:      0
BLOCKED:   0
NOT RUN:   0
```

## Cobertura nominal

```text
Rutas REST ejecutadas: 22 / 22
Cobertura nominal: 100 %
```

## Distribución de respuestas observadas

```text
200 OK       : 32
201 Created  :  7
400 Bad Request: 3
401 Unauthorized: 12
403 Forbidden:    13
404 Not Found:     4
409 Conflict:      6
---------------------
Total             77
```

---

# Verificaciones de seguridad

```text
Autenticación JWT:                       PASS
Login username/email:                    PASS
Password incorrecto:                     PASS
Usuario inexistente:                     PASS
JWT ausente:                             PASS
JWT malformado:                          PASS
JWT alterado:                            PASS
JWT expirado:                            PASS
JWT de otra clave/issuer:                PASS
JWT por query string:                    PASS
Usuario inactivo:                        PASS
Roles USER / STAFF / ADMIN:              PASS
Propiedad / anti-IDOR:                   PASS
Ocultamiento de DRAFT:                   PASS
Escalación de privilegios en registro:   PASS
Revocación de rol con JWT antiguo:       PASS
```

---

# Verificaciones de negocio

```text
Venues:                         PASS
Artists:                        PASS
Registro de usuarios:           PASS
Creación de evento:             PASS
Asociación artista-evento:      PASS
Publicación de evento:          PASS
Visibilidad de eventos:         PASS
Compra de tickets:              PASS
Propiedad de tickets:           PASS
Cancelación:                    PASS
Uso por STAFF:                  PASS
Transiciones inválidas:         PASS
Restricción por edad:           PASS
Control de capacidad:           PASS
ErrorResponse:                  PASS
```

---

# Observación final

La ejecución E2E manual permitió comprobar la integración real entre Postman, Spring Security, Controllers REST, Services, Repositories JPA y PostgreSQL `pulsepass_e2e`.

Las 22 rutas REST del alcance fueron ejercitadas manualmente mediante solicitudes individuales.

La colección permitió validar tanto escenarios nominales como controles de autenticación, autorización, propiedad, persistencia, reglas de negocio y manejo uniforme de errores.

**Resultado global de la ejecución: PASS.**
