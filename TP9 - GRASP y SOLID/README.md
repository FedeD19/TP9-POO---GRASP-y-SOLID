# SmartTicket — TP 9: GRASP y SOLID aplicados

## Etapa 3.4 — HU 3: Control de Acceso en Puerta

Proyecto basado **directamente en la Etapa 3.3 estable**. Java 21, Spring Boot 3.5.16, Maven, JPA/Hibernate, H2 y Swagger/OpenAPI.

### Funcionalidades actuales

| Etapa | Funcionalidad | Estado |
|---|---|---|
| 1 | Infraestructura y controladores | Completada |
| 2 | CRUD de Lugar, Sector, Evento y Cliente | Completada |
| 3.1 | Precio específico por evento/sector; generación de entradas disponibles y QR | Completada |
| 3.2 / HU 1 | Cotización según precio y disponibilidad | Completada |
| 3.3 / HU 2 | Pago ficticio, creación de ventas pagadas, emisión de entradas; concurrencia | Completada |
| **3.4 / HU 3** | **Validación de QR, acceso único y registro de hora de ingreso** | **Implementada** |
| Adicional | `POST /api/v1/pagos/webhook-pagos` | Pendiente (501); la pasarela simulada es síncrona |

**Importante:** el `codigoQR` es un identificador de texto tipo UUID, no una imagen QR. Se entrega al aprobar la compra. No se usan tarjetas reales.

## 1. Ejecutar en VSCode

Desde PowerShell en la carpeta que contiene `pom.xml`:

```powershell
mvn clean test
mvn spring-boot:run
```

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Estado: <http://localhost:8080/api/v1/estado>
- Consola H2: <http://localhost:8080/h2-console>
- JDBC URL: `jdbc:h2:mem:smartticket;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Usuario H2: `sa`; contraseña vacía.

H2 es **en memoria**: al detener Spring Boot se pierden los registros de esa ejecución. Las pruebas automatizadas usan sus propias bases aisladas.

## 2. Pruebas manuales completas desde Swagger

**El evento debe tener la fecha local de hoy.** En esta HU 3 se comprueba expresamente que el QR pertenezca al evento del día. El ejemplo con Rock Fest el 20 de diciembre que se usaba antes **no permitirá entrar si hoy es otra fecha**.

Para consultar la fecha local desde PowerShell:

```powershell
Get-Date -Format 'yyyy-MM-dd'
```

En el JSON de creación de evento reemplazá `AAAA-MM-DD` por la fecha que te devuelva ese comando, por ejemplo `2026-11-12T21:00:00` si hoy es el 12 de noviembre de 2026. La zona horaria del sistema es configurable mediante `smartticket.zona-horaria` (valor inicial `America/Argentina/Buenos_Aires`).

**Paso A — Lugar:** `POST /api/v1/lugares` (**201**)

```json
{"nombre":"Estadio Central","direccion":"Av. Principal 123"}
```

**Paso B — Sector:** `POST /api/v1/sectores` (**201**)

```json
{"nombre":"Platea","capacidadMaxima":3,"lugarId":1}
```

**Paso C — Evento de HOY:** `POST /api/v1/eventos` (**201**)

```json
{
  "nombre": "Rock Fest",
  "fechaHora": "AAAA-MM-DDT21:00:00",
  "lugarId": 1,
  "preciosSectores": [{"sectorId":1,"precio":12000.00}]
}
```

El campo `fechaHora` debe llevar una **fecha real** (números) al ejecutar. Si el lugar tiene más sectores, indicar precio para cada uno.

**Paso D — Cliente:** `POST /api/v1/clientes` (**201**)

```json
{"nombre":"Ana Perez","email":"ana-acceso@example.com"}
```

**Paso E — Pago y emisión:** `POST /api/v1/pagos/iniciar-pago` (**200**)

```json
{
  "clienteId": 1,
  "eventoId": 1,
  "sectorId": 1,
  "cantidad": 2,
  "tarjetaPrueba": "APROBADA"
}
```

La respuesta contiene `ventaId`, `estado: PAGADA`, `total` y `entradas` con dos pares `{entradaId, estado, codigoQR}`. **Copiá uno de los `codigoQR` reales**. Los IDs 1 de este ejemplo son ilustrativos: usar siempre los IDs recibidos en los pasos anteriores.

### Escenario 1. Ingreso exitoso (HTTP 200)

`POST /api/v1/accesos/escanear-acceso`:

```json
{"codigoQR":"PEGA_AQUI_EL_CODIGO_REAL_DE_LA_COMPRA"}
```

Respuesta de ejemplo (con datos ilustrativos):

```json
{
  "mensaje": "Acceso Permitido",
  "entradaId": 1,
  "eventoId": 1,
  "evento": "Rock Fest",
  "sectorId": 1,
  "estado": "UTILIZADA",
  "fechaHoraIngreso": "2026-11-12T20:45:00.123456"
}
```

El timestamp corresponde al instante real del escaneo en la zona horaria configurada; el ejemplo no es un valor fijo.

### Escenario 2. Reutilización del mismo QR (HTTP 409)

Volvé a enviar exactamente el mismo QR. Respuesta:

```json
{"error":"CONFLICTO","detalle":"Entrada ya utilizada"}
```

El estado sigue `UTILIZADA` y **la fecha y hora originales no cambian**.

### Escenario 3. QR inválido o desconocido (HTTP 404)

```json
{"codigoQR":"qr-inventado-123"}
```

Respuesta:

```json
{"error":"NO_ENCONTRADO","detalle":"Ticket Inválido o Inexistente"}
```

### Escenario 4. Entrada no emitida / sin pago (HTTP 403)

Antes de pagar, las entradas generadas por el evento están en `DISPONIBLE`, aunque ya tengan `codigoQR`. Para simular el escaneo de una de esas entradas, entrá en H2 Console y consultá:

```sql
SELECT ID, CODIGOQR, ESTADO, VENTA_ID FROM ENTRADAS;
```

Copiá un QR de una entrada `DISPONIBLE` (o `RESERVADA` si hiciste una modificación de prueba) y envialo al endpoint. Respuesta:

```json
{"error":"ACCESO_DENEGADO","detalle":"Ticket no emitido / Falta de pago"}
```

Una entrada `EMITIDA` cuyo `Venta.estado` sea distinto de `PAGADA`, o que carezca de venta, también es rechazada (comprobado por los tests).

### Verificaciones adicionales

- **Evento de otra fecha:** una entrada emitida y pagada para un evento que no ocurre hoy produce **409 Conflict** con `La entrada no corresponde a un evento de hoy`.
- **QR vacío, ausente o sin JSON:** produce **400 Bad Request** (validación de la solicitud).
- **Escaneos concurrentes:** dos puestos intentan ingresar la misma entrada; el primero que adquiere el bloqueo la marca como `UTILIZADA`, y el otro es rechazado.
- **Inventario:** `GET /api/v1/eventos/{id}/inventario` debe mostrar el paso de `EMITIDA` a `UTILIZADA` tras el ingreso.

## 3. Archivos de la Etapa 3.4

| Archivo | Cambio |
|---|---|
| `dto/AccesoRequest.java` | Nuevo DTO; valida `codigoQR` no vacío. |
| `dto/AccesoResponse.java` | Nuevo DTO; devuelve mensaje, entrada, evento, sector, estado y hora. |
| `service/AccesoService.java` | Nuevo servicio con todas las reglas de HU 3 y `@Transactional`. |
| `repository/EntradaRepository.java` | Nueva búsqueda por QR con bloqueo pesimista (`PESSIMISTIC_WRITE`). |
| `controller/AccesoController.java` | Reemplaza 501 por POST funcional y delega al servicio. |
| `exception/AccesoDenegadoException.java` | Nuevo error de ticket no emitido/pago pendiente. |
| `exception/ApiExceptionHandler.java` | Asocia ese error con HTTP 403. |
| `config/RelojConfig.java` | Configura el `Clock` para la hora local del evento. |
| `src/main/resources/application.properties` | Agrega `smartticket.zona-horaria`. |
| `src/test/java/com/smartticket/AccesoTests.java` | Pruebas de HU 3 y casos adicionales. |
| `src/test/java/com/smartticket/ConcurrenciaAccesoTests.java` | Simulación de dos escaneos simultáneos en BD independiente. |
| `pom.xml`, `README.md` | Actualización de descripción y documentación. |

No se modificaron el servicio de pagos, el servicio de cotizaciones ni las reglas CRUD anteriores.

## 4. Decisiones de diseño GRASP/SOLID

- **GRASP Controlador / SRP:** `AccesoController` recibe HTTP y delega. No conoce estados ni realiza consultas SQL.
- **Alta cohesión:** `AccesoService` centraliza el caso de uso de acceso, separado de cotización y pagos.
- **Experto en Información:** `Entrada` conserva su estado y hora; `Venta` conserva su estado de pago; `Evento` conserva su fecha.
- **Bajo acoplamiento / Indirección:** el servicio utiliza `EntradaRepository` (interfaz Spring Data), inyectada por constructor.
- **Programación defensiva:** se rechazan códigos inválidos, entradas no emitidas, ventas impagas y repeticiones.
- **Concurrencia:** `@Lock(PESSIMISTIC_WRITE)` sobre la fila de la entrada serializa escaneos del mismo QR. `@Transactional` agrupa validación, cambio de estado y registro de la hora.
- **Tiempo testeable:** un `Clock` inyectado permite definir la zona horaria en un solo lugar y evita llamar a la hora del sistema en varios componentes.

## 5. Pruebas y aislamiento H2

```powershell
mvn clean test
```

La Etapa 3.3 estable traía **31 pruebas**. Se agregaron **9 pruebas de `AccesoTests` y una de `ConcurrenciaAccesoTests`**, para un total esperado de **41 pruebas** si todos los archivos están incluidos.

La prueba de concurrencia utiliza la base aislada `jdbc:h2:mem:smartticket_accesos_concurrentes`, mediante `@DynamicPropertySource`. Las pruebas habituales conservan `src/test/resources/application.properties` y su base `smartticket_pruebas`, evitando la contaminación de datos que habíamos solucionado en la Etapa 3.3.

Si Maven produce un log extenso:

```powershell
cmd /c "mvn clean test > resultado-tests.txt 2>&1"
Get-Content .\resultado-tests.txt -Tail 70
```

Este ZIP **no contiene `target/`**, porque Maven genera esa carpeta. El código fue revisado estáticamente, pero la ejecución completa de Maven debe verificarse en tu computadora.

## 6. GitHub, después de verificar

```powershell
git status
git add .
git commit -m "Etapa 3.4: HU3 control de acceso por QR"
git push origin main
```

Antes de hacer `git add .`, eliminar `resultado-tests.txt` si lo generaste, o excluirlo de Git.
