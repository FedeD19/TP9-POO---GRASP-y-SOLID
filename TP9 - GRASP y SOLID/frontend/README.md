# SmartTicket — Frontend Etapa 4.3: HU 2 Pago y emisión

Frontend React 18 + Vite 5 que **se suma al backend estable Java de la Etapa 3.4**. Continúa funcionando la HU 1 de la Etapa 4.2 y se conecta ahora la HU 2 a la pasarela simulada del backend.

## Iniciar en VSCode (PowerShell, Windows)

Abrí **dos terminales integradas**.

**Terminal 1, carpeta que contiene `pom.xml`:**

```powershell
mvn spring-boot:run
```

**Terminal 2, carpeta `frontend/`:**

```powershell
cd frontend
npm.cmd run test
npm.cmd run dev
```

Abrí <http://localhost:5173>. En la computadora de este proyecto se utiliza **`npm.cmd`** porque la política de ejecución de PowerShell bloquea `npm.ps1`. Si faltan `node_modules`, ejecutá una vez `npm.cmd ci` desde `frontend/` (no se agregaron dependencias en la Etapa 4.3).

Para compilar la web: `npm.cmd run build`. El proxy de Vite redirige `/api` a `http://localhost:8080`; la API Java no cambia.

## Qué se implementó

- **HU 1** sigue cargando `GET /api/v1/eventos`, `GET /api/v1/eventos/{id}/inventario` y cotizando con `POST /api/v1/cotizaciones/cotizar`.
- **Nuevo:** después de una cotización exitosa, el botón **Continuar a pago simulado** transporta la selección confirmada a la pestaña HU 2.
- **Nuevo:** HU 2 carga clientes con `GET /api/v1/clientes` y permite seleccionar `APROBADA` o `RECHAZADA` sin campos de tarjeta real.
- **Nuevo:** POST `/api/v1/pagos/iniciar-pago` con `{ clienteId, eventoId, sectorId, cantidad, tarjetaPrueba }`. Solo el backend determina el precio y la disponibilidad al pagar.
- **Nuevo:** respuesta `PAGADA` con entradas `EMITIDA`: se muestra `ventaId`, total realmente confirmado, entradas y sus `codigoQR`. Hay botón para copiar cada código.
- **Nuevo:** mensajes adecuados para 402 (pago rechazado), 409 (se agotaron entradas), errores de red y 404.
- Se evita un segundo clic de pago mientras la petición está en curso y se bloquea temporalmente la navegación entre pestañas hasta obtener respuesta.
- La pestaña de **Acceso (HU 3)** y el **Explorador** siguen pendientes de integración. El código QR se entrega como **identificador de texto**, no como imagen escaneable.

## Preparación: datos de H2

H2 está en memoria. Si reiniciaste Java, hay que cargar los datos desde Swagger en <http://localhost:8080/swagger-ui.html>:

1. `POST /api/v1/lugares`:

```json
{"nombre":"Estadio Central","direccion":"Av. Principal 123"}
```

2. `POST /api/v1/sectores` (usar ID real del lugar):

```json
{"nombre":"Platea","capacidadMaxima":3,"lugarId":1}
```

3. `POST /api/v1/eventos` (usar los IDs reales):

```json
{"nombre":"Rock Fest","fechaHora":"2026-12-20T21:00:00","lugarId":1,"preciosSectores":[{"sectorId":1,"precio":12000.00}]}
```

4. **Nuevo para HU 2:** `POST /api/v1/clientes`:

```json
{"nombre":"Ana Perez","email":"ana@example.com"}
```

## Pruebas desde la interfaz

1. **Cotización correcta:** ingresá en Cotización, elegí Rock Fest / Platea y cantidad 2. Ejecutá **Calcular cotización**. Total esperado $24.000 y 3 entradas disponibles (si usaste los datos anteriores).
2. **Paso al pago:** pulsá **Continuar a pago simulado**. Comprobá que el evento, sector, cantidad y total se conservan. Seleccioná el cliente Ana Perez.
3. **Pago rechazado:** elegí `RECHAZADA` y enviá. Debe aparecer el error HTTP 402, sin comprobante y sin alterar el inventario. Podés cambiar a `APROBADA` sin recotizar si el stock sigue disponible.
4. **Pago aprobado:** elegí `APROBADA` y enviá. Debe mostrar venta `PAGADA`, total $24.000, dos entradas `EMITIDA` y **dos códigos QR distintos**.
5. **Comprobar inventario:** regresá a Cotización y consultá inventario. De las tres entradas iniciales, debe quedar **1 disponible**. La cotización no reduce el stock; el pago sí.
6. **Sin suficiente stock:** cotizá una entrada en Platea, aprobá el pago, y luego intentá comprar otra. Cuando ya esté agotado el sector, la cotización debería impedir la compra. También podés simular 409 cotizando desde dos pestañas de navegador mientras compiten por la última entrada; al pagar, el backend rechazará la que perdió stock.
7. **Sin clientes:** reiniciá Java y creá evento pero no cliente; Pago muestra cómo cargar uno desde Swagger.
8. **Acceso al QR:** copiá un código para usarlo en Swagger con `POST /api/v1/accesos/escanear-acceso` (la entrada deberá corresponder a un evento cuya fecha sea hoy). El frontend de acceso se conectará en la Etapa 4.4.

> El frontend **no persiste** comprobantes ni QR en `localStorage`: se mantienen solo en la sesión React actual. Copiá los códigos antes de recargar la página. No se usan tarjetas ni cargos reales.

## Pruebas automáticas

```powershell
cd frontend
npm.cmd run test
npm.cmd run build
```

Las **13 pruebas** del cliente REST y utilidades cubren las HU 1 y HU 2, casos 402/409 y validación del comprobante. La compilación de Vite verifica la construcción del frontend. La integración con Spring Boot debe comprobarse ejecutando ambos servidores localmente.

## Estructura de los archivos nuevos

```text
frontend/src/
├── App.jsx                       # Estado compartido HU 1 -> HU 2; conserva recibo
├── components/
│   ├── CotizacionPanel.jsx        # HU 1 + botón continuar
│   ├── PagoPanel.jsx              # HU 2 real
│   ├── PagoPanel.css              # Estilos de pago/entradas
│   └── Navbar.jsx                # Navegación protegida durante pago
├── services/api.js               # Añade GET clientes + POST iniciar-pago
└── utils/pago.js                  # Validación de selección y comprobante
frontend/tests/pago.test.js       # 6 pruebas nuevas
```

## ¿Qué principios aplicamos?

- **SRP:** `PagoPanel` gestiona presentación, `api.js` centraliza HTTP, `PagoService` implementa reglas de negocio.
- **Bajo Acoplamiento:** React solo conoce el contrato REST, no entidades JPA ni consultas SQL.
- **Indirección / DIP:** el backend conserva `PasarelaPago` como abstracción, reemplazable por otra implementación.
- **Experto en Información:** Java obtiene el precio de `PrecioSector` y el inventario de `Entrada`, no del total mostrado por React.

## Versión Git

```powershell
git status
git add frontend
git commit -m "Etapa 4.3: integrar HU2 pago y emision con React"
git push origin main
```

No incluir `frontend/node_modules/`, `frontend/dist/` ni registros de prueba. Conservá el `pom.xml` y `src/` del backend original.
