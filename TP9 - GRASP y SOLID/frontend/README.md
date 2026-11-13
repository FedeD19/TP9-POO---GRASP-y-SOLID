# SmartTicket — Frontend Etapa 4.2 (HU 1 conectada)

React 18 + Vite 5 sobre la Etapa 4.1. **Se conserva íntegro el backend Java estable de la Etapa 3.4**.

## Instalación y ejecución (VSCode / PowerShell en Windows)

Abrí dos terminales **integradas de VSCode**:

**Terminal 1, donde está `pom.xml`:**

```powershell
mvn spring-boot:run
```

**Terminal 2, donde está `frontend/package.json`:**

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Abrí `http://localhost:5173` y verificá `API conectada`. **Se usa `npm.cmd` porque PowerShell bloquea `npm.ps1` en esta computadora.** Para los próximos pasos, `npm.cmd run test` y `npm.cmd run build`.

> `npm.cmd ci` solo es necesario al instalar o actualizar dependencias. No hace falta repetirlo cada vez que se inicia el frontend.

## Qué funciona en la Etapa 4.2

1. **GET `/api/v1/eventos`**: la lista del selector viene de Spring Boot.
2. **GET `/api/v1/eventos/{id}/inventario`**: muestra los sectores de ese evento, su precio y las entradas disponibles.
3. **POST `/api/v1/cotizaciones/cotizar`**: envía `{ "eventoId": 1, "sectorId": 1, "cantidad": 2 }` y presenta el resultado **real** (precio unitario, subtotal, total y disponibilidad).
4. Muestra errores de validación del backend, estado de carga, inventario agotado, datos inexistentes y pérdida de conexión.
5. El indicador de API de la Etapa 4.1 continúa usando `GET /api/v1/estado`.
6. Pago, control de acceso y explorador conservan paneles provisionales: **todavía no están conectados**.

### Sin datos iniciales

H2 funciona en memoria. Si reiniciaste Java, la lista de eventos aparecerá vacía. Desde Swagger (`http://localhost:8080/swagger-ui.html`) creá:

**POST `/api/v1/lugares`:**

```json
{"nombre":"Estadio Central","direccion":"Av. Principal 123"}
```

**POST `/api/v1/sectores` (reemplazá `lugarId` por el ID obtenido):**

```json
{"nombre":"Platea","capacidadMaxima":3,"lugarId":1}
```

**POST `/api/v1/eventos` (reemplazá los IDs por los reales):**

```json
{"nombre":"Rock Fest","fechaHora":"2026-12-20T21:00:00","lugarId":1,"preciosSectores":[{"sectorId":1,"precio":12000.00}]}
```

Luego pulsá **Actualizar eventos**. El selector mostrará Rock Fest, el sector Platea, el precio unitario y 3 disponibles.

### Pruebas manuales

- **Éxito**: elegí Rock Fest, Platea y cantidad 2. Resultado esperado: HTTP 200 y total $24.000; las entradas siguen disponibles.
- **Insuficiente**: cantidad 4, cuando hay 3 disponibles. Debe mostrar el error de disponibilidad del backend, sin inventar una cotización válida.
- **Cantidad inválida**: vacío, 0, negativo o decimal: se valida localmente y no se envía la solicitud.
- **Sin eventos**: reiniciá Java (H2 vacía), actualizá el selector; debe guiarte a Swagger.
- **Cambio de evento**: cargá otro evento con precios distintos; el frontend debe cambiar sectores y limpiar la cotización anterior.
- **Desconexión**: detené Java y actualizá eventos; debe mostrar un error legible.
- **Consulta**: cotizar NO reserva ni compra; al pulsar **Actualizar disponibilidad**, la cifra no debe bajar.

### Pruebas automáticas del frontend

Desde `frontend/`:

```powershell
npm.cmd run test
npm.cmd run build
```

Las pruebas con `node --test` verifican el cliente REST, errores y validaciones **sin necesidad de un servidor**. `npm.cmd run build` valida React/Vite. Para comprobar los escenarios reales, iniciá el backend y utilizá el formulario. No es necesario volver a instalar dependencias si ya tenés `node_modules`.

## Estructura

```text
frontend/
├── src/
│   ├── App.jsx                  # Navegación y estado de conexión
│   ├── components/
│   │   ├── CotizacionPanel.jsx  # HU 1, formulario y respuesta
│   │   ├── CotizacionPanel.css  # Diseño responsive del formulario
│   │   ├── StagePanel.jsx       # HU 2/HU 3/explorador pendientes
│   │   ├── ConnectionCard.jsx
│   │   └── Navbar.jsx
│   ├── services/api.js          # Cliente HTTP centralizado
│   └── utils/formato.js         # Presentación y validación local
├── tests/api.test.js            # Pruebas con Node.js
├── vite.config.js               # Proxy /api a localhost:8080
└── package.json
```

La API es la autoridad sobre el precio y la disponibilidad. Los importes estimados del formulario son orientativos; el total confirmado procede siempre del `POST` al backend.

## Integrar sin perder el backend ni Git

Copiá la carpeta `frontend/` de esta entrega sobre la carpeta `frontend/` actual. No copies ni elimines `.git`, `src/` de Java ni `pom.xml`. `frontend/node_modules/` y `frontend/dist/` están ignorados por Git.

Después de verificar:

```powershell
git status
git add frontend
git commit -m "Etapa 4.2: integrar HU1 cotizacion con React y Spring Boot"
git push origin main
```

La Etapa 4.3 conectará Pago y Emisión de Entradas.
