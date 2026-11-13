# SmartTicket — Guía de integración de la Etapa 4.5

## Objetivo

Implementar **Explorador del sistema** en React + Vite con información REAL de la API Spring Boot: eventos, lugares, sectores, clientes, inventario, ventas y entradas. Las tres Historias de Usuario siguen funcionando como en la Etapa 4.4.

A diferencia de las Etapas 4.1–4.4, **esta etapa también incorpora archivos Java**: agregamos dos endpoints de consulta que antes no existían. No se modifica la lógica de pago, cotización ni acceso.

## 1. Instalación en tu proyecto actual (recomendada)

Utilizá `SmartTicket_Etapa4_5_Solo_Cambios.zip`.

1. Cerrá ambos servidores con **Ctrl + C** en sus terminales de VSCode.
2. Descomprimí el ZIP. Copiá las carpetas **`src/` y `frontend/`** del ZIP **a la carpeta del proyecto donde está `pom.xml`**, permitiendo combinar carpetas y reemplazar los archivos que lo soliciten. Nunca borres tus carpetas originales para hacer esto.
3. Conservá `pom.xml`, `.git`, `src` original, `frontend/node_modules/` y el resto de tu repositorio. El ZIP de cambios sólo contiene lo nuevo o modificado.
4. Si tenés cambios locales propios en alguno de estos archivos, revisá `git diff` antes de reemplazarlos para no perderlos.

**Importante:** no basta con copiar `frontend/` en esta etapa. Necesitás agregar el controlador, servicio y DTO Java para que React pueda consultar las ventas y entradas.

## 2. Iniciar Java (Terminal 1)

Abrí en VSCode la terminal integrada y situate en la carpeta **que contiene `pom.xml`** (no `src/`):

```powershell
mvn clean test
mvn spring-boot:run
```

La primera orden compila y ejecuta también las pruebas anteriores del proyecto. No cierres esta terminal después de iniciar Spring Boot.

## 3. Iniciar React (Terminal 2)

Abrí otra terminal integrada en VSCode:

```powershell
cd frontend
npm.cmd run test
npm.cmd run build
npm.cmd run dev
```

Recordatorio: usá **`npm.cmd`** en PowerShell, porque Windows bloqueaba `npm.ps1` en tu equipo. No es necesario repetir `npm.cmd ci` si conservaste `node_modules/`; no se agregaron dependencias.

Frontend: http://localhost:5173  
Swagger: http://localhost:8080/swagger-ui.html  
H2: http://localhost:8080/h2-console

## 4. Endpoints que utiliza el Explorador

Los que ya existían:

- `GET /api/v1/eventos`
- `GET /api/v1/lugares`
- `GET /api/v1/sectores`
- `GET /api/v1/clientes`
- `GET /api/v1/eventos/{id}/inventario`

**Nuevos endpoints de esta etapa:**

- `GET /api/v1/explorador/ventas?pagina=0&tamano=10`
- `GET /api/v1/explorador/entradas?pagina=0&tamano=10`
- `GET /api/v1/explorador/entradas?pagina=0&tamano=10&eventoId=1`

Los parámetros `pagina` y `tamano` usan índice de página que comienza en **0** y tamaño entre **1 y 50**. Los registros se presentan en **orden descendente por ID**. El filtro `eventoId` es opcional y solamente afecta al listado de entradas.

Ejemplo de respuesta para `/api/v1/explorador/entradas`:

```json
{
  "contenido": [
    {
      "id": 17,
      "eventoId": 1,
      "evento": "Rock Fest",
      "sectorId": 2,
      "sector": "Platea",
      "ventaId": 4,
      "estado": "UTILIZADA",
      "fechaHoraIngreso": "2026-11-12T20:31:15"
    }
  ],
  "pagina": 0,
  "tamano": 10,
  "totalElementos": 1,
  "totalPaginas": 1
}
```

Los ejemplos muestran la *estructura* del JSON, no datos cargados automáticamente. Con H2 vacía, `contenido` será `[]` y el total será `0`.

**Seguridad:** el explorador no entrega `codigoQR` de entradas ni datos de tarjetas. Sigue siendo una aplicación académica local sin autenticación ni autorización: no es adecuada para publicarse con información personal real.

## 5. Prueba manual completa

H2 es una base en memoria: al reiniciar Java suelen desaparecer los datos. Si no aparecen eventos, creá desde Swagger un lugar, un sector, un evento con precio para todos los sectores del lugar y un cliente.

1. Entrá a **Explorador → Resumen** y verificá los conteos.
2. En **Eventos e inventario**, presioná **Ver inventario**; comprobá que se visualicen precios, capacidad y estados de las entradas por sector.
3. Revisá **Sectores** y **Clientes**: los datos deben coincidir con Swagger.
4. En **Entradas**, elegí **Todos los eventos** o filtrá por el creado. Verificá que el listado paginado no muestre códigos QR.
5. En **Ventas**, inicialmente debe aparecer el estado vacío si todavía no compraste.
6. Volvé a **Cotización**, comprá una entrada desde **Pago y emisión** con `APROBADA`, y regresá a **Explorador**. Presioná **Actualizar datos**: verás una venta `PAGADA` y la entrada `EMITIDA`.
7. En un evento correspondiente a la fecha actual, ingresá con el QR desde **Control de acceso**; volvé al Explorador y actualizá: la misma entrada ahora debe mostrar `UTILIZADA` y `fechaHoraIngreso`.
8. Si creás más de diez entradas, utilizá **Anterior** y **Siguiente** para recorrer páginas; el filtro por evento debe reiniciar la página a cero.
9. Probá que `GET /api/v1/explorador/entradas?tamano=51` y `GET /api/v1/explorador/ventas?pagina=-1` respondan HTTP 400.
10. Confirmá que siguen funcionando cotización, pago, acceso y que los tests terminan correctamente.

## 6. ¿Qué se desarrolló?

### Backend nuevo

- `src/main/java/com/smartticket/controller/ExploradorController.java`
- `src/main/java/com/smartticket/service/ExploradorService.java`
- `src/main/java/com/smartticket/dto/{PaginaResponse,EntradaResumenResponse,VentaResumenResponse}.java`
- `src/main/java/com/smartticket/repository/EntradaRepository.java` (dos consultas para el explorador)
- `src/test/java/com/smartticket/ExploradorTests.java` (cinco pruebas de integración)

### Frontend nuevo/modificado

- `frontend/src/components/ExploradorPanel.jsx` y `ExploradorPanel.css`
- `frontend/src/utils/explorador.js`
- `frontend/src/services/api.js`
- `frontend/src/App.jsx`
- `frontend/src/components/{CotizacionPanel,PagoPanel,AccesoPanel}.jsx` (etiqueta de etapa)
- `frontend/tests/explorador.test.js` (ocho pruebas nuevas)

### GRASP / SOLID

- **Controlador GRASP:** `ExploradorController` recibe las solicitudes externas.
- **SRP / Alta cohesión:** `ExploradorService` transforma consultas en DTO de sólo lectura, separado de las reglas de pago y acceso.
- **Bajo acoplamiento:** React consume JSON por `api.js`, sin acceder a JPA ni H2.
- **Experto en información:** la capa Java obtiene los estados de las entidades y sus relaciones.
- **Programación defensiva:** se valida la paginación y los QR no se revelan en los listados.

## 7. GitHub

**Sólo cuando** Maven, los tests del frontend y las pruebas manuales terminen correctamente:

```powershell
git status
git diff
git add src frontend GUIA_ETAPA4_5.md
git commit -m "Etapa 4.5: explorador React y consultas REST de ventas y entradas"
git push origin main
```

No incluyas `node_modules/`, `dist/` ni `target/`.

## 8. Estado de validación

- Frontend: **31 pruebas con Node.js superadas**; Vite construye la aplicación.
- Código Java: **78 archivos analizados sin errores de sintaxis**.
- Pruebas Maven y comunicación real con H2: **pendientes de ejecución local**, ya que este entorno no tiene Maven disponible.
