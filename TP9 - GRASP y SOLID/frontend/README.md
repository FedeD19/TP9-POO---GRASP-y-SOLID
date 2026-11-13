# SmartTicket — Frontend React + Vite

Interfaz web del TP 9 de Programación Orientada a Objetos. Se comunica con el backend **Java / Spring Boot** que se encuentra en la raíz del repositorio. **Versión funcional hasta la Etapa 4.5:** cotización, pago simulado, acceso y Explorador.

> El frontend **no contiene lógica de cobro real** ni accede directamente a la base H2. Los precios, ventas, estados e ingresos se validan en el backend.

## Ejecutar desde VSCode (PowerShell de Windows)

**Terminal 1: backend.** Ubicarse en la carpeta raíz, donde se encuentra `pom.xml`:

```powershell
mvn spring-boot:run
```

**Terminal 2: frontend.** Abrir otra terminal integrada y ejecutar desde la raíz:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Abrir **http://localhost:5173**. Durante el desarrollo, `vite.config.js` redirige `/api` a **http://localhost:8080**, donde corre Spring Boot.

**Importante:** en esta instalación de PowerShell utilizamos **`npm.cmd`** para evitar el error por el bloqueo de ejecución de `npm.ps1`. `npm.cmd ci` solo es necesario en la primera instalación o si cambian las dependencias; para iniciar cada día alcanza con `npm.cmd run dev`.

### Verificar el frontend

```powershell
npm.cmd run test
npm.cmd run build
```

Se ejecutan las pruebas con `node --test` y se compila con Vite. Esto no sustituye las pruebas Maven del backend.

## Secciones

| Vista | Funcionalidad |
|---|---|
| **Cotización — HU 1** | Consultar eventos, sectores, precios y disponibilidad; cotizar una cantidad sin reservar inventario. |
| **Pago — HU 2** | Recuperar la selección, elegir cliente y simular `APROBADA` o `RECHAZADA`; mostrar el comprobante y los códigos de entradas emitidas. |
| **Acceso — HU 3** | Utilizar un código de la última compra o introducirlo manualmente, validar ingreso y mostrar el resultado. |
| **Explorador** | Ver resumen, eventos e inventario, sectores, clientes, ventas y entradas; paginar los listados de ventas y entradas. |

La última compra se conserva en memoria de React mientras la página permanece abierta; al recargarla se pierde ese estado visual, pero las ventas y entradas permanecen en H2 **hasta que se detiene Spring Boot**.

## Organización

```text
frontend/
├── src/
│   ├── components/
│   │   ├── CotizacionPanel.jsx
│   │   ├── PagoPanel.jsx
│   │   ├── AccesoPanel.jsx
│   │   ├── ExploradorPanel.jsx
│   │   └── ...
│   ├── services/api.js      # Cliente HTTP centralizado
│   ├── utils/               # Funciones auxiliares de presentación
│   ├── App.jsx              # Navegación y estado compartido
│   └── main.jsx
├── tests/                   # Tests con Node.js
├── vite.config.js           # Servidor Vite y proxy de desarrollo
├── package.json
└── package-lock.json
```

### Contratos REST consumidos

`services/api.js` usa rutas relativas a `/api/v1`:

- `GET /estado`, `GET /eventos`, `GET /eventos/{id}/inventario`, `GET /clientes`.
- `POST /cotizaciones/cotizar`, `POST /pagos/iniciar-pago`, `POST /accesos/escanear-acceso`.
- `GET /lugares`, `GET /sectores`, `GET /explorador/ventas`, `GET /explorador/entradas`.

**Los endpoint del Explorador requieren el backend actualizado a la Etapa 4.5.**

## Datos y limitaciones

Al iniciar H2 por primera vez no hay datos precargados. Creá un Lugar, Sector, Evento y Cliente desde **http://localhost:8080/swagger-ui.html**. Para comprobar el acceso, la fecha del evento debe ser la del día de la prueba.

Los códigos `codigoQR` son **identificadores de texto, no imágenes escaneables**. No envíes tarjetas bancarias reales: la pasarela es ficticia y solo admite `APROBADA` o `RECHAZADA`. La aplicación carece de autenticación y está destinada a demostraciones locales.

Documentación principal: **[README del proyecto](../README.md)**. Explicación de diseño: **[GRASP y SOLID](../docs/GRASP_SOLID.md)**.
