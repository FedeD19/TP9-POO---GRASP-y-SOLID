# SmartTicket — Trabajo Práctico 9 — Etapa 3.1

**Precios por evento y sector + generación automática de entradas.**

Esta versión parte del proyecto **Etapa 2 corregida** que ya contiene los CRUD de `Lugar`, `Sector`, `Evento` y `Cliente` con Spring Boot, Maven, JPA, H2 y Swagger. Las Historias de Usuario HU1 (cotizar), HU2 (pagar) y HU3 (escanear) **siguen sin implementarse (HTTP 501)**.

## 1. Requisitos y ejecución

- JDK 21 o superior y Maven 3.9.x.
- Puerto `8080` libre.
- Ubicarse con PowerShell en la carpeta que contiene `pom.xml`.

```powershell
mvn clean test
mvn spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html
- Estado: http://localhost:8080/api/v1/estado
- H2: http://localhost:8080/h2-console
- Base H2: `jdbc:h2:mem:smartticket;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, usuario `sa`, contraseña en blanco.

**La base es volátil:** al apagar el servidor se pierden los registros. Es normal y útil para repetir las pruebas.

## 2. Qué se agregó

| Componente | Archivo | Función |
|---|---|---|
| Entidad | `model/PrecioSector.java` | Une un `Evento` con un `Sector` y almacena su `precio` específico. La BD prohíbe duplicar `(evento_id,sector_id)`. |
| DTO de creación | `dto/EventoCreacionRequest.java` | Recibe el evento y una lista obligatoria de precios por sector. |
| DTO de precios | `dto/PrecioSectorRequest.java` | Valida `sectorId` y `precio` mayor a cero, con dos decimales como máximo. |
| DTO de lectura | `dto/InventarioEventoResponse.java`, `InventarioSectorResponse.java` | Permiten consultar precios, capacidades y cantidad de entradas por estado. |
| Repositorios | `PrecioSectorRepository.java`, `EntradaRepository.java` | Persistencia y consultas; los controladores no ejecutan SQL. |
| Servicio | `EventoService.java` | Valida precios, registra el evento y genera entradas en una transacción. |
| Servicio | `SectorService.java` | Impide modificar la capacidad o ubicación de sectores que ya tienen entradas de eventos. |
| Controlador | `EventoController.java` | Recibe el nuevo DTO en POST y expone GET de inventario. |
| Excepción | `SolicitudInvalidaException.java` | Convierte reglas inválidas de alta en HTTP 400 mediante `ApiExceptionHandler`. |
| Pruebas | `PreciosYEntradasTests.java` | Pruebas de precios, generación, invalidaciones, relaciones y bajas seguras. |

## 3. Decisiones de dominio

1. **El precio no se guarda en `Sector`.** El mismo sector puede valer $12.000 para Rock Fest y $18.000 para otro evento. Se usa una tabla independiente `PRECIOS_SECTOR`.
2. **Al crear un evento se deben enviar los precios de *todos* los sectores existentes de su lugar, exactamente una vez.** No se permite crear eventos en lugares que todavía no tengan sectores.
3. **Se genera una entrada física por cada plaza del sector.** Capacidad Platea = 3 y VIP = 2 significa crear cinco objetos `Entrada`, cada uno en `DISPONIBLE`, vinculado a evento y sector.
4. **Cada entrada recibe un token QR aleatorio (`UUID`) desde su creación**, pero ese código NO habilita el ingreso mientras la entrada no esté `EMITIDA`. En HU2 se pasará a `EMITIDA` después de un pago aprobado; HU3 deberá comprobar ese estado antes de permitir el ingreso.
5. **Una transacción engloba el alta completa** (`@Transactional`): el evento, los precios y las entradas se guardan todos o ninguno. Esto evita eventos parcialmente configurados.
6. **No se puede cambiar la capacidad ni el lugar de un sector usado por eventos**, ya que las entradas ya fueron generadas. Sí puede cambiarse el nombre del sector.
7. **PUT de Evento mantiene el DTO anterior** y permite editar nombre/fecha, pero NO mover el evento de lugar después de generar las entradas. Los precios fijados al crear el evento no se modifican con ese PUT.
8. **Eliminar un evento** está permitido solo si todas sus entradas siguen `DISPONIBLE` y ninguna está asociada a una venta. Se eliminan primero sus entradas y precios, luego el evento, dentro de una transacción. Si hay entradas `RESERVADA`, `EMITIDA`, `UTILIZADA` o vinculadas a ventas, se responde 409.
9. En esta etapa no se implementaron reservas, concurrencia de pagos, autenticación ni QR gráfico. El token almacenado en `codigoQR` es el dato que más adelante podrá codificarse en una imagen QR.

## 4. Pruebas en Swagger: orden correcto

### Paso A — Crear el lugar

`POST /api/v1/lugares` (esperado **201 Created**):

```json
{
  "nombre": "Estadio Central",
  "direccion": "Av. Principal 123"
}
```

Guardá el `id` de la respuesta (en ejemplos siguientes se supone que es `1`).

### Paso B — Crear dos sectores

`POST /api/v1/sectores` (esperado **201**) para Platea:

```json
{
  "nombre": "Platea",
  "capacidadMaxima": 3,
  "lugarId": 1
}
```

Repetí para VIP:

```json
{
  "nombre": "VIP",
  "capacidadMaxima": 2,
  "lugarId": 1
}
```

Anotá **ambos ID reales**; en el ejemplo supondremos `1` (Platea) y `2` (VIP). Usamos capacidades pequeñas para que sea fácil comprobar el inventario.

### Paso C — Crear el evento indicando todos los precios

`POST /api/v1/eventos` (esperado **201 Created**):

```json
{
  "nombre": "Rock Fest",
  "fechaHora": "2026-12-20T21:00:00",
  "lugarId": 1,
  "preciosSectores": [
    { "sectorId": 1, "precio": 12000.00 },
    { "sectorId": 2, "precio": 25000.00 }
  ]
}
```

IMPORTANTE: este JSON **cambia respecto de la Etapa 2**. Ahora es obligatorio agregar `preciosSectores`; el POST antiguo sin precios responde HTTP 400.

### Paso D — Comprobar los precios y la cantidad de entradas

`GET /api/v1/eventos/{id}/inventario` (esperado **200 OK**), utilizando el ID real retornado al crear Rock Fest.

Respuesta representativa:

```json
{
  "eventoId": 1,
  "evento": "Rock Fest",
  "sectores": [
    {
      "sectorId": 1,
      "nombre": "Platea",
      "capacidadMaxima": 3,
      "precio": 12000.00,
      "totalEntradas": 3,
      "disponibles": 3,
      "reservadas": 0,
      "emitidas": 0,
      "utilizadas": 0
    },
    {
      "sectorId": 2,
      "nombre": "VIP",
      "capacidadMaxima": 2,
      "precio": 25000.00,
      "totalEntradas": 2,
      "disponibles": 2,
      "reservadas": 0,
      "emitidas": 0,
      "utilizadas": 0
    }
  ]
}
```

Así se comprueba la generación de **cinco entradas** sin exponer los tokens QR a través de un endpoint público.

### Paso E — Probar errores intencionales

| Acción | Resultado esperado |
|---|---|
| Intentar crear un evento sin `preciosSectores` | **400** VALIDACION |
| Omitir el precio de VIP | **400** VALIDACION |
| Repetir dos veces Platea | **400** VALIDACION |
| Incluir un sector de otro lugar | **400** VALIDACION |
| Precio `0` o negativo | **400** VALIDACION |
| Crear evento en un lugar sin sectores | **400** VALIDACION |
| Cambiar capacidad o lugar de un sector usado en un evento | **409** CONFLICTO |
| Eliminar un sector usado en un evento | **409** CONFLICTO |
| Cambiar el lugar de un evento con entradas | **409** CONFLICTO |
| Consultar inventario de un evento inexistente | **404** NO_ENCONTRADO |

### Paso F — Probar eliminación segura (opcional)

`DELETE /api/v1/eventos/{id}` debe devolver **204 No Content** si todavía no hay ventas ni estados distintos de Disponible. Luego, `GET /api/v1/eventos/{id}/inventario` debe devolver 404, y los sectores quedan liberados para modificarse o borrarse.

## 5. Verificar la base de datos H2

En `http://localhost:8080/h2-console`, ejecutá:

```sql
SELECT * FROM LUGARES;
SELECT * FROM SECTORES;
SELECT * FROM EVENTOS;
SELECT * FROM PRECIOS_SECTOR;
SELECT EVENTO_ID, SECTOR_ID, ESTADO, COUNT(*) AS CANTIDAD
FROM ENTRADAS
GROUP BY EVENTO_ID, SECTOR_ID, ESTADO;
```

Deberías ver dos filas en `PRECIOS_SECTOR` y un total de **5** entradas para el ejemplo; el campo `CODIGOQR` de cada entrada tendrá un identificador diferente. No se muestran esos códigos en el inventario público.

## 6. Cómo integrar con tu repositorio GitHub existente

1. Hacé una copia de seguridad de tu carpeta actual (Etapa 2).
2. Descomprimí este ZIP.
3. Copiá el contenido de `src/`, `pom.xml` y `README.md` sobre **la carpeta local que ya tiene `.git`** (reemplazando los archivos correspondientes). No copies ninguna carpeta `target/` y **no borres `.git`**.
4. En VSCode, con la terminal ubicada en la raíz del repositorio:

```powershell
mvn clean test
mvn spring-boot:run
```

5. Probá los endpoints de la sección 4; después detené el servidor con `Ctrl+C`.
6. Versioná los cambios:

```powershell
git status
git add .
git commit -m "Etapa 3.1: precios por sector y generacion de entradas"
git push origin main
```

Cambiá `main` por el nombre de tu rama si es distinto. No hagas el commit si las pruebas fallan.

## 7. Por qué este diseño respeta GRASP y SOLID

- **SRP:** `EventoController` solo recibe/envía HTTP; `EventoService` coordina creación y validación; `PrecioSector` representa la relación de negocio; cada repositorio persiste entidades.
- **Experto en Información (GRASP):** el evento conoce el lugar; cada sector conoce su capacidad; `PrecioSector` conoce el precio específico del evento y sector.
- **Alta cohesión:** las reglas de creación e inventario se concentran en el servicio de eventos; no se distribuyen en los controladores.
- **Bajo acoplamiento / Indirección:** el servicio usa interfaces de repositorios inyectadas por constructor, sin SQL directo ni dependencias de una base H2 específica.
- **Transacciones:** garantizan coherencia de las entidades relacionadas y facilitan la posterior HU1.

## 8. Alcance posterior

- **Etapa 3.2 — HU1:** cotizar evento + sector + cantidad con stock disponible y subtotal según `PrecioSector`.
- **Etapa 3.3 — HU2:** validar nuevamente el stock, simular pago, registrar Venta, emitir entradas y habilitar acceso por QR.
- **Etapa 3.4 — HU3:** verificar código, estado y evento; marcar `UTILIZADA` y registrar fecha y hora.

**Importante:** `mvn clean test` incluye los tests de las Etapas 1 y 2, modificados para usar el nuevo POST de creación de eventos, más los tests de `PreciosYEntradasTests`. Debe terminar en `BUILD SUCCESS` antes de considerar completada esta etapa.
