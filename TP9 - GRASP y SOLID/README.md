# SmartTicket - TP 9: Etapa 2 (CRUD del dominio)

Ampliacion del proyecto **Etapa 1**, respetando su paquete Java `com.smartticket`,
configuracion Spring Boot + Maven + H2 y rutas reservadas para las Historias de Usuario.
Este ZIP es una **copia independiente**: no sobrescribe el proyecto original.

## Requisitos

- Java JDK 21.
- Maven disponible (`mvn -version`).
- Puerto 8080 libre (no ejecutar la Etapa 1 al mismo tiempo).

## Iniciar

Desde la carpeta que contiene `pom.xml`:

```powershell
mvn clean test
mvn spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html
- Estado: http://localhost:8080/api/v1/estado
- H2: http://localhost:8080/h2-console

### Consola H2

- JDBC URL: `jdbc:h2:mem:smartticket;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- User: `sa`
- Password: vacio

```sql
SELECT * FROM LUGARES;
SELECT * FROM SECTORES;
SELECT * FROM EVENTOS;
SELECT * FROM CLIENTES;
```

La base es **en memoria**: los datos se borran cuando se cierra Spring Boot.

## Endpoints nuevos (Etapa 2)

La siguiente tabla corresponde a cada una de las cuatro colecciones
`lugares`, `sectores`, `eventos` y `clientes`.

| Metodo | Ruta | Efecto | Exito |
|---|---|---|---|
| POST | `/api/v1/{coleccion}` | Crear | 201 Created |
| GET | `/api/v1/{coleccion}` | Listar | 200 OK |
| GET | `/api/v1/{coleccion}/{id}` | Buscar por ID | 200 OK |
| PUT | `/api/v1/{coleccion}/{id}` | Actualizar | 200 OK |
| DELETE | `/api/v1/{coleccion}/{id}` | Eliminar | 204 No Content |

Errores esperables: 400 (datos invalidos), 404 (ID o referencia inexistente),
409 (email duplicado o restriccion por relaciones existentes).

## Recorrido de prueba en Swagger

1. `POST /api/v1/lugares`

   ```json
   { "nombre": "Estadio Central", "direccion": "Av. Principal 123" }
   ```

2. Guardar el `id` retornado (por ejemplo `1`) y enviarlo en
   `POST /api/v1/sectores`:

   ```json
   { "nombre": "Platea", "capacidadMaxima": 300, "lugarId": 1 }
   ```

3. Crear un evento con `POST /api/v1/eventos`:

   ```json
   { "nombre": "Rock Fest", "fechaHora": "2026-12-20T21:00:00", "lugarId": 1 }
   ```

4. Crear un cliente con `POST /api/v1/clientes`:

   ```json
   { "nombre": "Ana Perez", "email": "ana@example.com" }
   ```

5. Probar `GET /api/v1/lugares` y los otros listados para consultar
   los registros guardados.
6. Probar `GET /api/v1/sectores/1` usando el ID real recibido.
7. Modificar el sector con `PUT /api/v1/sectores/1` usando el cuerpo
   completo `{ "nombre": "Platea Alta", "capacidadMaxima": 250, "lugarId": 1 }`.
8. Si queres probar DELETE, elimina primero los objetos relacionados.
   Por ejemplo, para borrar un Lugar con Sector/Evento asociados,
   elimina esos registros antes de borrar el Lugar. No hay borrado en cascada.

**Importante:** los ID de los ejemplos son ilustrativos: deben reemplazarse
por los valores que efectivamente devuelva tu base de datos.

## Carpetas y responsabilidades

```text
src/main/java/com/smartticket/
  config/       Swagger (de etapa 1)
  controller/   4 CRUD nuevos + endpoints HU 1/2/3 sin implementar
  dto/          Request: datos de entrada; Response: datos de salida
  exception/    Errores de negocio, validacion y restricciones de FK
  model/        6 entidades JPA del dominio (se mantienen)
  repository/   4 interfaces JpaRepository para las consultas
  service/      4 servicios con operaciones CRUD y validaciones
```

### Por que separar capas (GRASP/SOLID)

- **Controlador / SRP:** se limita a traducir HTTP a una llamada al servicio.
- **Servicio / Alta cohesion:** coordina alta, baja, consulta y modificaciones
  de una entidad y verifica referencias obligatorias.
- **Repositorio / Indireccion y bajo acoplamiento:** Spring Data JPA
  proporciona el acceso a H2 sin escribir SQL en los controladores.
- **DTO / encapsulamiento:** el cliente de la API nunca necesita construir
  una entidad JPA anidada para vincular relaciones; envia `lugarId`.
- **Inyeccion por constructor:** las dependencias aparecen explicitamente
  y pueden sustituirse en pruebas.
- **@Valid:** valida campos antes de que se realice la operacion.
- **Transacciones:** las operaciones de escritura se agrupan bajo `@Transactional`.

## Alcance y limites deliberados

**En esta etapa SI:**

- CRUD completo de Lugar, Sector, Evento y Cliente.
- Validacion de campos: no vacios, capacidad positiva, email valido,
  IDs de relaciones existentes, email unico (sin distinguir mayusculas).
- Respuestas HTTP explicitas, JSON sin entidades JPA anidadas.
- Los endpoints antiguos de HU1, HU2 y HU3 mantienen la respuesta 501.

**En esta etapa NO:**

- Crear entradas automaticamente al crear un evento.
- Asignar precios por `(evento, sector)`.
- Cotizar, reservar, pagar, generar QR ni registrar ingresos.
- Autenticacion, manejo de tarjetas ni integracion con pasarelas.

Segun las reglas del TP, **antes de habilitar HU1** debera completarse el dominio
con precios particulares por sector para cada evento, y entradas pre-generadas
segun capacidad. Este proyecto todavia no es un sistema de venta funcional.

## Notas de diseño

- `PUT` reemplaza los campos editables completos; no recibe el campo `id`.
- La entidad `Sector` guarda su asociacion con `Lugar`; la entidad `Evento`
  tambien. El JSON usa `lugarId` para evitar ciclos y detalles internos JPA.
- No existe borrado en cascada: se rechaza con HTTP 409 si hay referencias.
- Modificar la capacidad de un sector o mover un evento es una operacion simple
  del CRUD; **antes de generar entradas** habra que endurecer reglas de negocio
  para no invalidar tickets existentes.
- Se mantienen `Entrada` y `Venta` como entidades definidas en la Etapa 1;
  aun no tienen controladores de negocio.
- H2 en memoria facilita las pruebas, pero no persiste entre reinicios.

## Ejecutar las pruebas

```powershell
mvn clean test
```

Incluye tests de infraestructura heredados y pruebas nuevas de los cuatro CRUD,
validaciones y respuestas HTTP, en `DominioCrudTests.java`.


## Correccion de integridad referencial (09/11/2026)

Se corrigio `LugarService.eliminar`: antes de borrar se comprueba con
`SectorRepository.existsByLugar_Id(id)` y `EventoRepository.existsByLugar_Id(id)`
si el lugar tiene sectores o eventos. Si los tiene, se lanza `ConflictoException`
y la API responde HTTP 409, en lugar de intentar borrar y provocar
`TransientObjectException` durante la prueba transaccional.

Se anadio una prueba que verifica el rechazo al eliminar lugares con eventos.
Usar `mvn clean test` para verificar todas las pruebas en tu equipo.
