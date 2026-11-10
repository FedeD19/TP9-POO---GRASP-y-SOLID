# SmartTicket — TP9 GRASP y SOLID — Etapa 3.2

**HU 1: Selección y cotización**, sobre la Etapa 3.1 (precios específicos por evento/sector y generación automática de entradas).

Proyecto Java 21, Spring Boot 3.5.16, Maven, Spring Data JPA, H2, Bean Validation y Swagger/OpenAPI.

## 1. Qué funciona

- CRUD de **Lugar, Sector, Evento y Cliente** (Etapa 2).
- Registro de **precios por evento y sector**, y generación de entradas `DISPONIBLE` al crear un evento (Etapa 3.1).
- Consulta de inventario: `GET /api/v1/eventos/{id}/inventario`.
- **Cotización:** `POST /api/v1/cotizaciones/cotizar` (Etapa 3.2).
- **Pendiente:** iniciar pago, webhook de pagos y escaneo de acceso; conservan la respuesta 501.

En esta versión, **cotizar NO reserva, vende ni emite entradas**, y tampoco cambia su estado. El resultado es orientativo hasta intentar comprar; HU 2 verificará nuevamente el inventario.

## 2. Ejecutar en Windows con VSCode

Abrir la carpeta del proyecto donde está `pom.xml` y ejecutar en PowerShell:

```powershell
mvn clean test
mvn spring-boot:run
```

Si Maven finaliza las pruebas correctamente, mostrará `BUILD SUCCESS`.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Estado: http://localhost:8080/api/v1/estado
- Consola H2: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:smartticket;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Usuario H2: `sa`; contraseña: vacía.

La base H2 está **en memoria**: sus registros desaparecen al detener Spring Boot.

## 3. Preparación de los datos en Swagger

### A. Crear Lugar

`POST /api/v1/lugares` (esperado **201**):

```json
{
  "nombre": "Estadio Central",
  "direccion": "Av. Principal 123"
}
```

Anotar el `id` real de la respuesta. En los ejemplos siguientes se supone que vale `1`.

### B. Crear dos sectores

`POST /api/v1/sectores` dos veces (esperado **201**):

```json
{
  "nombre": "Platea",
  "capacidadMaxima": 3,
  "lugarId": 1
}
```

```json
{
  "nombre": "VIP",
  "capacidadMaxima": 1,
  "lugarId": 1
}
```

Anotar sus IDs. En los ejemplos siguientes se supone `1` para Platea y `2` para VIP. Usamos capacidades pequeñas para facilitar las pruebas.

### C. Crear Evento con precios

`POST /api/v1/eventos` (esperado **201**):

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

Se debe indicar precio para **todos los sectores del lugar, sin duplicados**, como exige la Etapa 3.1. Anotar el ID real del evento; el ejemplo supone `1`.

### D. Verificar inventario

`GET /api/v1/eventos/1/inventario`: se esperan 3 entradas Platea, 1 VIP, todas `DISPONIBLE`.

## 4. HU 1: ejecutar las tres pruebas de aceptación

Abrir en Swagger: **Cotizaciones** → `POST /api/v1/cotizaciones/cotizar` → **Try it out**.

### Escenario 1. Cotización exitosa (HTTP 200)

```json
{
  "eventoId": 1,
  "sectorId": 1,
  "cantidad": 2
}
```

Respuesta ejemplo:

```json
{
  "eventoId": 1,
  "evento": "Rock Fest",
  "sectorId": 1,
  "sector": "Platea",
  "cantidad": 2,
  "precioUnitario": 12000.00,
  "subtotal": 24000.00,
  "total": 24000.00,
  "entradasDisponibles": 3
}
```

**Cálculo:** `12000.00 × 2 = 24000.00`. Como no hay descuentos, comisiones ni impuestos en la HU 1, `subtotal == total`.

### Escenario 2. Disponibilidad insuficiente (HTTP 400)

```json
{
  "eventoId": 1,
  "sectorId": 2,
  "cantidad": 3
}
```

Hay 1 entrada VIP, pero se solicitan 3:

```json
{
  "error": "VALIDACION",
  "detalle": "Capacidad insuficiente o entradas no disponibles"
}
```

### Escenario 3. Cantidad inválida (HTTP 400)

```json
{
  "eventoId": 1,
  "sectorId": 1,
  "cantidad": 0
}
```

Repetir con `-1`. Respuesta:

```json
{
  "error": "VALIDACION",
  "detalle": "La cantidad de entradas solicitadas debe ser mayor a cero"
}
```

### Verificaciones adicionales

- Evento inexistente o sector no perteneciente al evento → **404 Not Found**.
- Falta de alguno de los campos requeridos → **400 Bad Request**.
- Dos cotizaciones consecutivas no alteran las entradas: consultar nuevamente `GET /api/v1/eventos/1/inventario` → los `disponibles` siguen iguales.
- Una entrada `RESERVADA`, `EMITIDA` o `UTILIZADA` no se cuenta como disponible; esto se verifica en las pruebas automatizadas `CotizacionTests` (todavía no hay endpoints de pago que cambien estados).

Los IDs `1` y `2` son **ilustrativos**. Usar los IDs reales devueltos en cada paso. Si probaste la base antes sin reiniciar, tal vez los IDs sean distintos.

## 5. Archivos nuevos y modificados

| Archivo | Cambio |
|---|---|
| `dto/CotizacionRequest.java` | Recibe `eventoId`, `sectorId`, `cantidad`. Valida IDs positivos y campos obligatorios. |
| `dto/CotizacionResponse.java` | Expone identificación, cantidad, precio unitario, subtotal, total y disponibilidad actual. |
| `service/CotizacionService.java` | Comprueba cantidad positiva **antes de consultar repositorios**, valida el evento/sector, cuenta entradas `DISPONIBLE` y calcula con `BigDecimal`. |
| `controller/CotizacionController.java` | Reemplaza la respuesta 501 de HU 1 por un endpoint POST con `@Valid @RequestBody`, que delega en el servicio. |
| `test/CotizacionTests.java` | Seis pruebas automatizadas que cubren HU 1, estados de entradas, errores y precios por evento. |
| `model/Evento.java` | Actualización de un comentario desactualizado. |
| `pom.xml` | Actualización de la descripción del proyecto; no se cambian dependencias. |

Los endpoints `PagoController`, `WebhookPagoController` y `AccesoController` permanecen sin lógica de negocio.

## 6. GRASP y SOLID aplicados

- **Controlador GRASP / SRP (SOLID):** `CotizacionController` conoce HTTP y delega el caso de uso; no consulta H2 ni multiplica importes.
- **Experto en Información (GRASP):** el precio se obtiene de la entidad `PrecioSector`; los estados de las entradas están en `Entrada`. `CotizacionService` **coordina** esos datos y realiza el cálculo del caso de uso.
- **Bajo acoplamiento / Indirección (GRASP):** el servicio depende de las interfaces `EventoRepository`, `PrecioSectorRepository` y `EntradaRepository`, inyectadas mediante el constructor.
- **Alta cohesión (GRASP):** la lógica de cotizar reside en un servicio enfocado, separado de creación de eventos, persistencia, pagos y accesos.
- **Precisión monetaria:** `BigDecimal.multiply(BigDecimal.valueOf(cantidad))` evita usar `double` para dinero.
- **Solo lectura:** `@Transactional(readOnly = true)` expresa que cotizar no modifica la información. Una consulta es una foto del inventario, NO una garantía de disponibilidad futura.

## 7. Siguiente etapa (HU 2)

Será necesario seleccionar entradas concretas y resolver la concurrencia correctamente al iniciar un pago (por ejemplo, usando bloqueo transaccional o actualizaciones condicionales). No es suficiente confiar en una cotización anterior: otro cliente podría comprar primero.

## 8. Git (una vez que funcionen las pruebas)

Integrar estos archivos en la **misma carpeta Git** de la Etapa 3.1. Conservar la carpeta `.git` existente y no copiar ni subir `target/`:

```powershell
git status
git add .
git commit -m "Etapa 3.2: HU1 seleccion y cotizacion"
git push origin main
```

> Nota de verificación: el ZIP y su contenido pueden validarse en este entorno. Maven no está instalado y el entorno no puede acceder a los servidores de dependencias, por lo que el `BUILD SUCCESS` se debe confirmar ejecutando `mvn clean test` localmente.
