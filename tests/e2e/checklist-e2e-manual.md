# Checklist manual E2E — PulsePass

**Fecha de ejecución:** 17/10/2026
**Run ID:** `manual01`
**Ambiente:** `pulsepass_e2e`
**Base URL:** `http://localhost:8080`
**Herramienta:** Postman Desktop
**Modalidad:** Ejecución manual request por request mediante `Send`

---

# 1. Preparación

- [x] Aplicación PulsePass inició correctamente.
- [x] PostgreSQL `pulsepass_e2e` estuvo disponible.
- [x] Base E2E independiente de desarrollo.
- [x] Flyway V1-V4 aplicado.
- [x] Fixtures E2E cargados.
- [x] Venue activo disponible.
- [x] Venue de capacidad controlada disponible.
- [x] Artistas iniciales disponibles.
- [x] Evento DRAFT disponible.
- [x] Evento PUBLISHED disponible.
- [x] Evento para prueba de edad disponible.
- [x] Evento para prueba de capacidad disponible.
- [x] ADMIN disponible.
- [x] STAFF disponible.
- [x] INACTIVE_USER disponible.
- [x] USER_A creado mediante API pública.
- [x] USER_B creado mediante API pública.
- [x] Colección Postman importada.
- [x] Environment `PulsePass Local E2E` importado.
- [x] Environment seleccionado.
- [x] `baseUrl = http://localhost:8080`.
- [x] Passwords mantenidos únicamente como valores locales.
- [x] JWT no almacenados en la documentación final.
- [x] Ejecución realizada sin Collection Runner.
- [x] Ejecución realizada sin Newman.
- [x] Ejecución realizada sin Postman CLI.
- [x] Ejecución realizada sin `pm.test`.
- [x] Ejecución realizada sin `pm.expect`.

---

# 2. Cobertura nominal de rutas REST — 22/22

## Venues — 2/2

- [x] GET `/api/venues/{code}`
- [x] GET `/api/venues/active`

## Artists — 3/3

- [x] GET `/api/artists/{id}`
- [x] GET `/api/artists/by-stage-name`
- [x] GET `/api/artists/active`

## Users — 3/3

- [x] POST `/api/users`
- [x] GET `/api/users/by-email`
- [x] GET `/api/users/by-username`

## Events — 6/6

- [x] POST `/api/events`
- [x] GET `/api/events/{eventCode}`
- [x] GET `/api/events/published`
- [x] PATCH `/api/events/{eventCode}/publish`
- [x] POST `/api/events/{eventCode}/artists/{artistId}`
- [x] GET `/api/events/by-artist`

## Tickets — 6/6

- [x] POST `/api/tickets`
- [x] GET `/api/tickets/{ticketCode}`
- [x] GET `/api/tickets/by-user`
- [x] GET `/api/events/{eventCode}/tickets/paid`
- [x] PATCH `/api/tickets/{ticketCode}/cancel`
- [x] PATCH `/api/tickets/{ticketCode}/use`

## Auth — 2/2

- [x] POST `/api/auth/login`
- [x] GET `/api/auth/me`

---

# 3. Venues

- [x] Venue existente por código retorna `200`.
- [x] `E2E-VENUE-001` retorna `active=true`.
- [x] Capacidad de `E2E-VENUE-001` es 100.
- [x] Listado de venues activos retorna `200`.
- [x] `E2E-VENUE-SMALL-001` aparece activo.
- [x] Capacidad de `E2E-VENUE-SMALL-001` es 1.
- [x] Venue inexistente retorna `404`.
- [x] Error de venue inexistente respeta `ErrorResponse`.

---

# 4. Artists

- [x] Artista existente por ID retorna `200`.
- [x] Artista `Solar Beat` recuperado correctamente.
- [x] Consulta por `stageName` retorna `200`.
- [x] Listado de artistas activos retorna `200`.
- [x] Los cinco artistas iniciales aparecen activos.
- [x] Artista inexistente retorna `404`.
- [x] Error de artista inexistente respeta `ErrorResponse`.

---

# 5. Registro de usuarios

- [x] Registro USER_A retorna `201`.
- [x] USER_A queda activo.
- [x] Registro USER_B retorna `201`.
- [x] USER_B queda activo.
- [x] Registro público no devuelve password.
- [x] Registro público no devuelve hash BCrypt.
- [x] Registro público no devuelve JWT.
- [x] Registro público no permite obtener rol ADMIN.
- [x] Registro duplicado retorna `409`.
- [x] Email inválido retorna `400`.

---

# 6. Autenticación y JWT

- [x] Login USER_A mediante username retorna `200`.
- [x] Login USER_A mediante email retorna `200`.
- [x] Login USER_B retorna `200`.
- [x] Login STAFF retorna `200`.
- [x] Login ADMIN retorna `200`.
- [x] JWT emitido con `tokenType = Bearer`.
- [x] JWT emitido con `expiresIn = 900`.
- [x] `/api/auth/me` retorna identidad correcta de USER_A.
- [x] `/api/auth/me` confirma rol `USER`.
- [x] Password incorrecto retorna `401`.
- [x] Identificador inexistente retorna `401`.
- [x] Usuario inactivo no obtiene login válido.
- [x] Ruta protegida sin Authorization retorna `401`.
- [x] Bearer malformado retorna `401`.
- [x] JWT alterado retorna `401`.
- [x] JWT expirado retorna `401`.
- [x] JWT de otra clave o issuer retorna `401`.
- [x] JWT enviado por query string no autentica.
- [x] Bearer inválido en endpoint público sigue la política implementada y retorna `401`.
- [x] Usuario inactivo con token previamente emitido pierde acceso.
- [x] Revocación de rol se refleja utilizando JWT previamente emitido.

---

# 7. Roles y autorización

## USER

- [x] USER no puede crear eventos.
- [x] USER no puede publicar eventos.
- [x] USER no puede asociar artistas.
- [x] USER no puede consultar tickets PAID del evento.
- [x] USER no puede marcar tickets como usados.

## STAFF

- [x] STAFF puede consultar tickets PAID.
- [x] STAFF puede marcar tickets como usados.
- [x] STAFF puede consultar eventos DRAFT según política heredada.
- [x] STAFF no puede publicar eventos.

## ADMIN

- [x] ADMIN puede crear eventos.
- [x] ADMIN puede consultar eventos DRAFT.
- [x] ADMIN puede asociar artistas.
- [x] ADMIN puede publicar eventos.
- [x] ADMIN puede consultar datos de usuario permitidos.
- [x] Si se revoca ADMIN en persistencia, el JWT antiguo deja de otorgar privilegios.

---

# 8. Propiedad y protección anti-IDOR

- [x] USER_A consulta sus propios datos por email.
- [x] USER_A consulta sus propios datos por username.
- [x] USER_B no puede consultar el email de USER_A.
- [x] USER_B no puede consultar el username de USER_A.
- [x] Acceso a recurso privado ajeno retorna `403`.
- [x] USER_A compra únicamente para su propio email.
- [x] USER_A no puede comprar ticket utilizando email de USER_B.
- [x] USER_A consulta su propio ticket.
- [x] USER_B no puede consultar ticket de USER_A.
- [x] USER_B no puede listar tickets de USER_A.
- [x] USER_B no puede cancelar ticket de USER_A.
- [x] Denegaciones de propiedad no exponen el recurso ajeno.

Política validada:

```text
Recurso privado perteneciente a otro USER
→ 403 Forbidden
```

---

# 9. Visibilidad de eventos

- [x] Evento PUBLISHED es visible públicamente.
- [x] Evento DRAFT no es visible para ANONYMOUS por código.
- [x] DRAFT público retorna `404`.
- [x] `/api/events/published` no muestra eventos DRAFT.
- [x] `/api/events/by-artist` público no muestra DRAFT.
- [x] STAFF puede observar DRAFT cuando está autorizado.
- [x] ADMIN puede observar DRAFT cuando está autorizado.
- [x] Evento creado como DRAFT no fue visible públicamente antes de publicarlo.
- [x] Evento publicado mediante PATCH pasó a ser visible públicamente.

Política validada:

```text
ANONYMOUS / USER + DRAFT
→ 404

STAFF / ADMIN + DRAFT
→ permitido
```

---

# 10. Administración de eventos

- [x] Creación válida retorna `201`.
- [x] Evento nuevo inicia en estado `DRAFT`.
- [x] Evento conserva venue correcto.
- [x] Asociación con `Solar Beat` retorna `200`.
- [x] Asociación artista-evento persiste.
- [x] Publicación retorna `200`.
- [x] Estado cambia de `DRAFT` a `PUBLISHED`.
- [x] Estado publicado persiste en consultas posteriores.
- [x] Visitante puede consultar el evento después de publicarlo.
- [x] Evento inexistente retorna `404`.

---

# 11. Tickets — compra

- [x] USER_A compra ticket GENERAL válido.
- [x] Compra válida retorna `201`.
- [x] Ticket GENERAL tiene precio `120000`.
- [x] Ticket nuevo inicia en estado `PAID`.
- [x] Ticket corresponde al email de USER_A.
- [x] Ticket corresponde al evento esperado.
- [x] USER_A compra un segundo ticket independiente para prueba de uso.
- [x] Ticket VIP tiene precio `250000`.
- [x] T1 y T2 utilizan códigos independientes.

---

# 12. Tickets — cancelación

- [x] USER_B no puede cancelar T1 de USER_A.
- [x] Intento ajeno retorna `403`.
- [x] Después del `403`, T1 continúa `PAID`.
- [x] USER_A puede cancelar su propio T1.
- [x] Cancelación válida retorna `200`.
- [x] T1 pasa a `CANCELLED`.
- [x] Segunda cancelación retorna `409`.
- [x] Ticket permanece `CANCELLED` después del conflicto.

---

# 13. Tickets — uso

- [x] USER no puede marcar T2 como usado.
- [x] Intento USER retorna `403`.
- [x] Después del `403`, T2 continúa `PAID`.
- [x] STAFF puede marcar T2 como usado.
- [x] Uso válido retorna `200`.
- [x] T2 pasa a `USED`.
- [x] Segundo intento de uso retorna `409`.
- [x] Ticket permanece `USED`.

---

# 14. Tickets pagados

- [x] STAFF puede consultar tickets PAID de un evento.
- [x] T2 aparece mientras está `PAID`.
- [x] T1 cancelado no aparece como PAID.
- [x] USER no puede utilizar el endpoint operativo.
- [x] USER recibe `403`.

---

# 15. Reglas de negocio

- [x] Usuario duplicado retorna `409`.
- [x] Compra para evento DRAFT retorna `409`.
- [x] Compra para evento no publicado no persiste ticket.
- [x] Restricción de edad insuficiente retorna `409`.
- [x] Capacidad controlada permite la primera compra.
- [x] Evento de capacidad 1 no permite una segunda compra efectiva.
- [x] No se excede la capacidad configurada.
- [x] Cancelación repetida retorna `409`.
- [x] Uso repetido retorna `409`.
- [x] Reglas de estado se aplican antes de mutaciones inválidas.

---

# 16. Escalación de privilegios

- [x] Payload de registro incluyó intento de `role=ADMIN`.
- [x] Registro no otorgó ADMIN.
- [x] Usuario creado pudo autenticarse.
- [x] `/api/auth/me` confirmó únicamente rol `USER`.
- [x] No fue posible obtener rol privilegiado mediante registro público.

Resultado:

```text
Intento role=ADMIN
→ usuario final con rol USER
```

---

# 17. Revocación de rol

- [x] ADMIN obtuvo JWT válido inicialmente.
- [x] Rol ADMIN fue retirado en la base E2E.
- [x] Se reutilizó JWT emitido previamente.
- [x] Operación exclusiva de ADMIN fue rechazada.
- [x] Resultado definitivo `403 Forbidden`.
- [x] Las autoridades actuales se validan contra la persistencia.
- [x] El JWT antiguo no conserva privilegios revocados.

---

# 18. Usuario inactivo

- [x] Fixture INACTIVE_USER disponible.
- [x] Login de usuario inactivo retorna `401`.
- [x] Respuesta no revela excesivamente el motivo del rechazo.
- [x] Token previo del usuario deja de permitir acceso después de inactivación.
- [x] Acceso posterior retorna `401`.

---

# 19. Validaciones HTTP 400

- [x] Email inválido retorna `400`.
- [x] `details` identifica el campo `email`.
- [x] JSON malformado retorna `400`.
- [x] Enum inválido retorna `400`.
- [x] Error de JSON/enum no produce HTTP 500.
- [x] Contrato `ErrorResponse` se mantiene.

Nota:

No existía en la colección importada un request independiente denominado `TC-HTTP-400-REQUIRED`. La cobertura de HTTP `400` quedó comprobada mediante los escenarios reales ejecutados de email inválido, JSON malformado y enum inválido.

---

# 20. ErrorResponse

Para los errores observados se comprobó:

- [x] Campo `timestamp`.
- [x] Campo `status`.
- [x] Campo `error`.
- [x] Campo `message`.
- [x] Campo `details`.
- [x] `status` coincide con el HTTP observado.
- [x] 400 utiliza `Bad Request`.
- [x] 401 utiliza `Unauthorized`.
- [x] 403 utiliza `Forbidden`.
- [x] 404 utiliza `Not Found`.
- [x] 409 utiliza `Conflict`.
- [x] Sin stack trace.
- [x] Sin password.
- [x] Sin hash BCrypt.
- [x] Sin secretos de configuración.
- [x] Sin exposición innecesaria de entidades JPA.

---

# 21. Integridad después de denegaciones

- [x] Cancelación ajena denegada no cambia T1.
- [x] Uso por USER denegado no cambia T2.
- [x] Compra para email ajeno queda bloqueada.
- [x] Creación de evento por USER queda bloqueada.
- [x] Publicación por USER queda bloqueada.
- [x] Publicación por STAFF queda bloqueada.
- [x] Asociación artista por USER queda bloqueada.
- [x] Operación ADMIN con rol revocado queda bloqueada.
- [x] Se realizaron consultas posteriores cuando fue necesario verificar persistencia.

---

# 22. Códigos HTTP requeridos

- [x] HTTP `200 OK`.
- [x] HTTP `201 Created`.
- [x] HTTP `400 Bad Request`.
- [x] HTTP `401 Unauthorized`.
- [x] HTTP `403 Forbidden`.
- [x] HTTP `404 Not Found`.
- [x] HTTP `409 Conflict`.
- [x] No se observó HTTP `500` inesperado en la ejecución final.

---

# 23. Persistencia

- [x] USER_A persistido.
- [x] USER_B persistido.
- [x] Evento creado persistentemente.
- [x] Asociación artista-evento persistida.
- [x] Publicación persistida.
- [x] T1 persistido inicialmente como PAID.
- [x] T1 persistido posteriormente como CANCELLED.
- [x] T2 persistido inicialmente como PAID.
- [x] T2 persistido posteriormente como USED.
- [x] Mutaciones rechazadas no alteraron los estados esperados.

---

# 24. Ejecución manual

- [x] Requests ejecutados individualmente.
- [x] Cada request enviado mediante `Send`.
- [x] HTTP Status inspeccionado manualmente.
- [x] JSON inspeccionado manualmente.
- [x] Estado de negocio inspeccionado manualmente.
- [x] Tokens copiados manualmente cuando fue necesario.
- [x] Códigos de ticket copiados manualmente cuando fue necesario.
- [x] No se utilizó automatización de colección.
- [x] No se utilizó Runner.
- [x] No se utilizó Newman.
- [x] No se utilizó Postman CLI.

---

# 25. Seguridad de artefactos

- [x] Passwords reales no incluidos en documentación versionada.
- [x] JWT completos no incluidos en `registro-ejecucion-manual.md`.
- [x] Secreto JWT no incluido.
- [x] `e2e-secrets.local.sql` ignorado por Git.
- [x] Environment versionado no contiene valores sensibles reales.
- [x] Evidencia final evita credenciales completas.

---

# 26. Incidencias de ejecución

## TC-SEC-010

- [x] Caso reejecutado.
- [x] Resultado definitivo `403 Forbidden`.
- [x] Protección horizontal confirmada.

## TC-SEC-017

- [x] Precondición corregida.
- [x] Rol ADMIN retirado antes del request definitivo.
- [x] JWT previo conservado.
- [x] Resultado definitivo `403 Forbidden`.
- [x] Incidencia preliminar no se considera defecto del producto.

---

# 27. Cierre

- [x] Preparación E2E completada.
- [x] Fixtures verificados.
- [x] Colección importada correctamente.
- [x] Environment importado correctamente.
- [x] 22/22 rutas nominales cubiertas.
- [x] Casos de autenticación ejecutados.
- [x] Casos de autorización ejecutados.
- [x] Casos de propiedad / IDOR ejecutados.
- [x] Casos de visibilidad DRAFT ejecutados.
- [x] Casos de tickets ejecutados.
- [x] Casos de reglas de negocio ejecutados.
- [x] Casos 400 ejecutados.
- [x] Casos 401 ejecutados.
- [x] Casos 403 ejecutados.
- [x] Casos 404 ejecutados.
- [x] Casos 409 ejecutados.
- [x] ErrorResponse verificado.
- [x] Persistencia verificada.
- [x] Integridad posterior a denegaciones verificada.
- [x] Escalación de privilegios rechazada.
- [x] Revocación de rol verificada.
- [x] Usuario inactivo verificado.
- [x] No se identificaron defectos funcionales confirmados.
- [x] Registro de ejecución manual diligenciado.
- [x] Evidencias sin secretos.

---

# Resultado final

```text
Cobertura nominal de rutas: 22 / 22
Cobertura nominal:          100 %

Resultado global: PASS
```
