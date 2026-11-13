# SmartTicket — Frontend Etapa 4.4: Control de acceso (HU 3)

React 18 + Vite 5 conectado al backend Java estable (Etapa 3.4). Las tres historias de usuario funcionan desde la interfaz: cotización, pago simulado y escaneo por texto del QR. **Explorador** sigue pendiente.

## Arrancar en VSCode (PowerShell Windows)

En la primera terminal, en la carpeta con `pom.xml`:

```powershell
mvn spring-boot:run
```

En una segunda terminal integrada, desde la carpeta `frontend`:

```powershell
cd frontend
npm.cmd run test
npm.cmd run build
npm.cmd run dev
```

Abrí http://localhost:5173. Utilizamos `npm.cmd` porque PowerShell bloquea `npm.ps1`. `npm.cmd ci` es necesario solamente si falta `node_modules` (no agregamos dependencias nuevas). El proxy Vite conecta las rutas `/api` con Spring Boot en localhost:8080; el backend no se modificó.

## Funcionalidades

- **HU 1 Cotización:** eventos, sectores, precios, inventario y total real desde Spring Boot.
- **HU 2 Pago:** selecciona cliente, simula `APROBADA`/`RECHAZADA`, muestra comprobante y cada identificador `codigoQR`.
- **HU 3 Acceso:** desde cualquier entrada emitida, pulsá **Probar acceso con esta entrada**. También podés entrar en **Control de acceso**, elegir uno de los códigos de la última compra o pegar uno manualmente. La solicitud se hace con `POST /api/v1/accesos/escanear-acceso` y JSON `{ "codigoQR": "..." }`.
- La respuesta correcta trae `mensaje: "Acceso Permitido"`, `estado: "UTILIZADA"` y `fechaHoraIngreso`. La interfaz muestra el evento, número de entrada, sector y hora de ingreso.
- Maneja **403** (no emitida / sin venta pagada), **404** (QR inexistente), **409** (entrada ya utilizada o evento de otra fecha) y errores de red. El frontend rechaza códigos vacíos antes de enviarlos; el backend también retorna HTTP 400 si se le envía directamente una solicitud inválida.
- Durante el escaneo se bloquea el botón y la navegación entre pestañas para evitar solicitudes accidentales duplicadas. El backend también protege la entrada con bloqueo de escritura en H2.

**Importante:** los QR son *códigos de texto*, no imágenes QR escaneables. No se usa cámara ni se almacenan entradas permanentemente en el navegador. La última compra desaparece al recargar la página, pero su venta y las entradas permanecen en H2 mientras Spring Boot esté abierto.

## Preparación de datos de prueba: evento de hoy

El backend valida la fecha del evento usando `smartticket.zona-horaria=America/Argentina/Buenos_Aires`. El ejemplo de Rock Fest del **20/12/2026** usado en etapas anteriores generará **HTTP 409** si se intenta entrar otro día, aunque la venta esté pagada.

Consultá hoy desde PowerShell:

```powershell
Get-Date -Format 'yyyy-MM-dd'
```

Reemplazá `AAAA-MM-DD` por ese resultado **dentro del campo `fechaHora`** del JSON del evento.

Desde Swagger (http://localhost:8080/swagger-ui.html) creá:

1. Lugar: `POST /api/v1/lugares`

   ```json
   {"nombre":"Estadio Central","direccion":"Av. Principal 123"}
   ```

2. Sector: `POST /api/v1/sectores` — reemplazá `lugarId` por el ID creado.

   ```json
   {"nombre":"Platea","capacidadMaxima":3,"lugarId":1}
   ```

3. Evento: `POST /api/v1/eventos` — fecha **de hoy**, precio positivo, ID de sector real.

   ```json
   {"nombre":"Rock Fest","fechaHora":"AAAA-MM-DDT21:00:00","lugarId":1,"preciosSectores":[{"sectorId":1,"precio":12000.00}]}
   ```

4. Cliente: `POST /api/v1/clientes`

   ```json
   {"nombre":"Ana Perez","email":"ana.acceso@example.com"}
   ```

Si H2 ya tiene datos, usá los IDs reales de las respuestas, no asumas que serán 1. Si creaste más sectores en el lugar, agregá el precio de cada uno en `preciosSectores`.

## Comprobar los escenarios en React

1. Cotizá dos entradas de Platea a $12.000. Debe devolver $24.000 sin reducir stock.
2. Continuá al Pago, elegí a Ana y `APROBADA`. Debe registrar la venta `PAGADA`, emitir dos entradas `EMITIDA` y devolver dos QR distintos.
3. Presioná **Probar acceso con esta entrada** junto a la primera entrada. Control de acceso precarga ese código. Ejecutá el escaneo. Debe aparecer **Acceso Permitido**, `UTILIZADA` y la hora.
4. Enviá nuevamente el mismo código: debe devolver **HTTP 409**, entrada ya utilizada (sin registrar otro ingreso).
5. Elegí el segundo QR de la última compra: debe autorizarlo por separado (si el evento es hoy).
6. Pegá `qr-inventado-123`: **HTTP 404**, QR inexistente.
7. En H2 Console (`http://localhost:8080/h2-console`) buscá una entrada sin pagar:

   ```sql
   SELECT ID, CODIGOQR, ESTADO, VENTA_ID FROM ENTRADAS WHERE ESTADO = 'DISPONIBLE';
   ```

   Pegá uno de esos QR en Control de acceso; debe devolver **HTTP 403**.
8. Para la condición de fecha incorrecta, comprá una entrada para un evento de otra fecha y escaneala. Debe devolver **HTTP 409** por fecha, distinto del error de ingreso duplicado.
9. Consultá `GET /api/v1/eventos/{id}/inventario`: la entrada aceptada ya figura como `UTILIZADA`. El escaneo no altera la capacidad total.

En H2 Console, la URL JDBC es `jdbc:h2:mem:smartticket;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, usuario `sa`, contraseña vacía.

## Pruebas automatizadas

```powershell
npm.cmd run test
npm.cmd run build
```

Se agregaron diez pruebas de HU 3 al cliente y utilidades; el total es **23 pruebas**. Incluyen el contrato REST, resultado correcto, códigos vacíos, 403/404/409 (dos causas distintas), falla de red y hora de ingreso.

No hay cambios en `pom.xml` ni en `src/main/java`. Mantené también tus 41 pruebas de Maven ejecutando `mvn clean test` si deseás volver a comprobar el backend.

## Archivos de esta etapa

| Archivo | Cambio |
|---|---|
| `src/components/AccesoPanel.jsx` | Formulario de QR, estados de la operación y resultados. |
| `src/components/AccesoPanel.css` | Estilos y diseño responsive del acceso. |
| `src/services/api.js` | Método REST `escanearAcceso`. |
| `src/utils/acceso.js` | Validaciones de presentación y mensajes de errores. |
| `src/App.jsx` | Conexión Pago → Acceso y conservación de QR en memoria. |
| `src/components/PagoPanel.jsx` | Botón para probar cada QR emitido. |
| `tests/acceso.test.js` | Diez pruebas de HU 3. |

## Principios aplicados

- **SRP:** `AccesoPanel` presenta el formulario; `api.js` realiza HTTP; `AccesoService` mantiene las reglas de ingreso.
- **Controlador / Bajo Acoplamiento (GRASP):** React no consulta H2 ni modifica estados de entradas directamente; hace la solicitud a `AccesoController`.
- **Experto en Información:** Java consulta el estado de `Entrada`, su `Venta` y la fecha de `Evento`.
- **Integridad transaccional:** el backend conserva su transacción y bloqueo pesimista para impedir dos ingresos con el mismo QR.

Cuando termines las pruebas:

```powershell
git status
git add frontend
git commit -m "Etapa 4.4: integrar HU3 control de acceso con React"
git push origin main
```

No subas `frontend/node_modules`, `frontend/dist` ni informes temporales. La próxima etapa es el explorador de datos.
