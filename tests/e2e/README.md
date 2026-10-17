# PulsePass — Pruebas E2E manuales

Este directorio contiene los artefactos de la Fase 5 de PulsePass para la validación HTTP end-to-end manual mediante Postman.

La ejecución E2E se realiza contra una instancia real de PulsePass conectada a una base de datos PostgreSQL exclusiva de pruebas.

---

## 1. Objetivo

Validar de extremo a extremo que las capas desarrolladas en las Fases 1 a 4 funcionen correctamente juntas:

- Persistencia con JPA y PostgreSQL.
- Migraciones Flyway.
- Services y reglas de negocio.
- Controllers REST.
- Spring Security.
- JWT.
- Roles.
- Propiedad de recursos.
- Protección anti-IDOR.
- Manejo uniforme de errores.

La Fase 5 cubre:

- 20 operaciones REST heredadas de negocio.
- `POST /api/auth/login`.
- `GET /api/auth/me`.

Total:

```text
22 rutas base
```

---

## 2. Estrategia de ejecución

Las pruebas se ejecutan manualmente desde Postman.

Cada solicitud debe abrirse individualmente y enviarse mediante:

```text
Send
```

Después de cada request se debe inspeccionar manualmente:

- HTTP Status.
- Headers.
- JSON de respuesta.
- Datos retornados.
- Estado de negocio.
- Estado persistido cuando corresponda.
- Permisos y propiedad.

Un HTTP `200` o `201` por sí solo no implica que una prueba sea correcta.

---

## 3. Restricciones

En esta fase NO se utilizan:

- Pre-request Scripts.
- Post-response / Tests.
- `pm.test`.
- `pm.expect`.
- Collection Runner.
- Newman.
- Postman CLI.
- Ejecución automática de colecciones.
- Mocks durante la ejecución E2E.

La colección Postman se utiliza exclusivamente de forma manual.

---

## 4. Arquitectura E2E

```text
Postman
   ↓
HTTP real
   ↓
Spring Security
   ↓
Controllers REST
   ↓
Services
   ↓
Repositories JPA
   ↓
PostgreSQL pulsepass_e2e
```

Durante las pruebas no se reemplaza ninguna capa mediante mocks.

---

## 5. Ambiente E2E

Base URL utilizada:

```text
http://localhost:8080
```

Base PostgreSQL exclusiva:

```text
pulsepass_e2e
```

Puerto PostgreSQL local:

```text
5434
```

Contenedor Docker utilizado:

```text
pulsepass-e2e-postgres
```

La base E2E debe permanecer separada de cualquier base de desarrollo.

---

## 6. Configuración de ejecución

La aplicación debe arrancarse usando variables de entorno equivalentes a:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5434/pulsepass_e2e

SPRING_DATASOURCE_USERNAME=pulsepass_e2e

SPRING_DATASOURCE_PASSWORD=<valor local>

PULSEPASS_JWT_ISSUER=pulsepass-api

PULSEPASS_JWT_SECRET_BASE64=<secreto E2E local>
```

`PULSEPASS_JWT_SECRET_BASE64` debe contener una clave aleatoria de al menos 32 bytes.


---

## 7. Flyway

Antes de cargar los fixtures deben estar aplicadas las migraciones existentes:

```text
V1 — create schema
V2 — insert initial artists
V3 — add streaming url to event
V4 — security credentials roles
```

No se debe modificar ni reescribir el historial Flyway para facilitar las pruebas E2E.

---

## 8. Fixtures

Los fixtures se encuentran en:

```text
tests/e2e/fixtures/
```

Archivos versionados:

```text
README.md
seed-e2e.sql
```

Existe además un archivo exclusivamente local:

```text
e2e-secrets.local.sql
```

Ese archivo contiene passwords de fixtures E2E y está ignorado mediante:

```text
*.local.sql
```

Nunca debe subirse a Git.

---

## 9. Actores E2E

### ANONYMOUS

No utiliza encabezado `Authorization`.

Solo puede acceder a endpoints públicos.

### USER_A

Se registra mediante:

```text
POST /api/users
```

Debe obtener únicamente:

```text
ROLE_USER
```

Se utiliza para:

- consultar sus propios datos;
- comprar tickets;
- consultar sus propios tickets;
- cancelar sus propios tickets;
- comprobar restricciones de rol.

### USER_B

Segundo usuario independiente.

Se utiliza principalmente para comprobar:

- propiedad;
- aislamiento entre usuarios;
- protección anti-IDOR;
- intentos de acceso a recursos de USER_A.

### STAFF

Fixture preprovisionado fuera del registro público.

Tiene:

```text
ROLE_STAFF
```

Puede:

- consultar tickets pagados;
- marcar tickets como usados;
- consultar eventos no publicados según la política heredada de Fase 4.

### ADMIN

Fixture preprovisionado fuera del registro público.

Tiene:

```text
ROLE_ADMIN
```

Puede:

- crear eventos;
- publicar eventos;
- asociar artistas;
- realizar operaciones administrativas autorizadas.

### INACTIVE_USER

Fixture con:

```text
active = false
```

Se utiliza para comprobar que el servidor vuelve a consultar el estado actual del usuario y no confía únicamente en el JWT.

---

## 10. Fixtures principales

### Venues

```text
venueActiveCode = E2E-VENUE-001
venueSmallCode  = E2E-VENUE-SMALL-001
```

### Artista

```text
artistStageName = Solar Beat
```

El `artistId` debe verificarse en el ambiente antes de ejecutar las pruebas.

No se deben asumir IDs numéricos como regla general.

### Eventos

```text
draftEventCode          = E2E-DRAFT-001
publishedEventCode      = E2E-PUBLISHED-001
eventToPublishCode      = E2E-PUBLISH-001
ageRestrictedEventCode  = E2E-AGE-001
capacityEventCode       = E2E-CAPACITY-001
```

---

## 11. Archivos Postman

Los archivos se encuentran en:

```text
tests/e2e/postman/
```

Importar en Postman:

```text
PulsePass-Manual-E2E.postman_collection.json
PulsePass-local.postman_environment.json
```

Seleccionar el environment:

```text
PulsePass Local E2E
```

---

## 12. Variables sensibles

Las siguientes variables deben configurarse únicamente de forma local en Postman:

```text
userAPassword
userBPassword

adminPassword
staffPassword
inactivePassword

userAToken
userBToken
adminToken
staffToken
inactiveToken

expiredToken
foreignIssuerToken
escalationToken
```

Sus valores reales no deben almacenarse en los archivos exportables del repositorio.

---

## 13. Captura manual de JWT

Después de un login exitoso:

1. Enviar `POST /api/auth/login`.
2. Verificar HTTP `200`.
3. Confirmar `tokenType = Bearer`.
4. Confirmar que `accessToken` no esté vacío.
5. Confirmar `expiresIn = 900`, salvo que la configuración haya cambiado.
6. Copiar manualmente el valor de `accessToken`.
7. Guardarlo en la variable local correspondiente.
8. Ejecutar `GET /api/auth/me`.
9. Confirmar identidad y roles.

No existe captura automática de tokens mediante scripts.

---

## 14. Política 403 / 404

La política adoptada en Fase 4 para recursos privados ajenos es:

```text
403 Forbidden
```

Ejemplos:

```text
USER_B consulta usuario de USER_A
→ 403

USER_B consulta ticket de USER_A
→ 403

USER_B cancela ticket de USER_A
→ 403
```

Para eventos no publicados:

```text
ANONYMOUS consulta DRAFT
→ 404

USER consulta DRAFT
→ 404

STAFF consulta DRAFT
→ permitido

ADMIN consulta DRAFT
→ permitido
```

El `404` utilizado para eventos DRAFT evita revelar públicamente su existencia.

---

## 15. Secuencia de ejecución

### Flujo A — Descubrimiento público

Ejecutar:

```text
GET /api/venues/active
GET /api/venues/{code}

GET /api/artists/active
GET /api/artists/{id}
GET /api/artists/by-stage-name

GET /api/events/published
GET /api/events/{eventCode}
GET /api/events/by-artist
```

Comprobar que ningún evento DRAFT sea visible públicamente.

### Flujo B — Registro y JWT

1. Registrar USER_A.
2. Registrar USER_B.
3. Login USER_A.
4. Login USER_B.
5. Login STAFF.
6. Login ADMIN.
7. Copiar manualmente los JWT.
8. Ejecutar `/api/auth/me`.
9. Validar identidad y roles.
10. Ejecutar escenarios negativos de autenticación.

### Flujo C — Administración de eventos

1. Crear un evento como ADMIN.
2. Consultarlo como ADMIN mientras esté DRAFT.
3. Comprobar que ANONYMOUS no puede verlo.
4. Asociar artista.
5. Publicar el evento.
6. Consultarlo nuevamente sin Bearer.
7. Comprobar `/api/events/published`.
8. Comprobar `/api/events/by-artist`.
9. Confirmar que USER y STAFF no pueden crear/publicar/asociar cuando el contrato no lo permite.

### Flujo D — Tickets

Se deben utilizar dos tickets independientes.

#### Ticket T1

```text
comprar
→ consultar como USER_A
→ intentar consultar como USER_B
→ intentar cancelar como USER_B
→ comprobar que sigue PAID
→ cancelar como USER_A
→ intentar cancelar nuevamente
```

#### Ticket T2

```text
comprar
→ consultar tickets PAID como STAFF
→ intentar marcar usado como USER
→ comprobar que sigue PAID
→ marcar usado como STAFF
→ intentar marcar usado nuevamente
```

No utilizar un ticket cancelado para probar el flujo de uso.

### Flujo E — Errores

Comprobar manualmente los siguientes códigos:

```text
400
401
403
404
409
```

Un HTTP `500` inesperado en los escenarios definidos se considera:

```text
FAIL
```

---

## 16. ErrorResponse

Las respuestas de error deben respetar la estructura:

```json
{
  "timestamp": "...",
  "status": 403,
  "error": "Forbidden",
  "message": "...",
  "details": {}
}
```

Se deben verificar los cinco campos:

```text
timestamp
status
error
message
details
```

El valor de `status` debe coincidir con el HTTP real.

La respuesta nunca debe exponer:

```text
stack trace
password
passwordHash
JWT
secretos de configuración
entidades JPA internas
```

---

## 17. Verificación de integridad

Una denegación no se considera completamente comprobada solo por recibir `403`.

Cuando una mutación sea rechazada se debe realizar una consulta posterior con un actor autorizado.

Ejemplo:

```text
USER_B intenta cancelar ticket de USER_A
→ 403

USER_A vuelve a consultar el ticket
→ debe continuar PAID
```

También debe comprobarse que:

```text
compra para email ajeno
→ no crea ticket

use realizado por USER
→ no cambia ticket

publicación rechazada
→ no cambia estado del evento
```

---

## 18. Criterios de resultado

### PASS

La respuesta HTTP, JSON y estado de negocio o seguridad coinciden completamente con lo esperado.

### FAIL

Alguna comprobación no coincide con el contrato esperado o aparece un HTTP `500` inesperado.

### BLOCKED

No puede ejecutarse porque falta una precondición indispensable.

Ejemplos:

```text
fixture inexistente
token no disponible
actor no provisionado
respuesta previa requerida no obtenida
```

### NOT RUN

El caso todavía no ha sido ejecutado.

Los casos `BLOCKED` y `NOT RUN` nunca deben presentarse como aprobados.

---

## 19. Registro de resultados

Los resultados deben escribirse manualmente en:

```text
tests/e2e/registro-ejecucion-manual.md
```

Después de cada `Send` se debe registrar:

- ID del caso.
- Fecha/hora.
- Actor.
- Método y ruta.
- HTTP esperado.
- HTTP observado.
- JSON o estado relevante.
- PASS / FAIL / BLOCKED / NOT RUN.
- Evidencia u observación.

No se deben registrar valores sensibles.

---

## 20. Evidencias

No incluir en capturas, documentos o commits:

```text
JWT completos
passwords
hashes BCrypt
PULSEPASS_JWT_SECRET_BASE64
secretos de PostgreSQL
información privada innecesaria
```

---

## 21. Restauración

Antes de una nueva sesión puede volver a ejecutarse:

```text
seed-e2e.sql
```

para recuperar los fixtures controlados.

Las pruebas que modifican roles, estados de eventos o tickets deben restaurarse antes de repetir una ejecución completa.

Nunca ejecutar procedimientos de limpieza contra bases de:

```text
desarrollo
QA
producción
```

---

## 22. Definition of Done

La Fase 5 se considera terminada cuando:

- La colección Postman v2.1 es válida e importable.
- No existen scripts Pre-request ni Post-response/Tests.
- Las 22/22 rutas base fueron ejecutadas manualmente.
- USER_A, USER_B, STAFF y ADMIN realizaron login correctamente.
- Los JWT fueron manejados únicamente como variables locales.
- Se comprobaron respuestas 400, 401, 403, 404 y 409.
- Se comprobó propiedad e IDOR.
- Se comprobó visibilidad de DRAFT.
- Se comprobaron transiciones de eventos.
- Se comprobaron compra, cancelación y uso de tickets.
- Las mutaciones rechazadas no alteraron el estado persistido.
- ErrorResponse fue inspeccionado manualmente.
- La bitácora está diligenciada.
- Los casos FAIL y BLOCKED están documentados.
- No existen contraseñas ni JWT reales versionados.
- La cobertura final es 22/22.