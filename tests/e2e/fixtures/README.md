# Fixtures E2E de PulsePass

Los fixtures de este directorio son exclusivos del ambiente E2E.

No deben ejecutarse contra bases de datos de desarrollo, QA o producción.

Los datos privilegiados de ADMIN, STAFF e INACTIVE_USER se provisionarán fuera del registro público y únicamente para las pruebas end-to-end.

Las contraseñas almacenadas en PostgreSQL deberán utilizar BCrypt.

Los usuarios USER_A y USER_B se crearán mediante `POST /api/users` durante la ejecución manual de Postman.