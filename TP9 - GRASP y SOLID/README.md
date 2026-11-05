# SmartTicket - TP 9: GRASP y SOLID (Etapa 1)

Proyecto independiente de ejemplo basado en la estructura de la captura compartida.
No modifica automaticamente el proyecto original del alumno.

## Alcance de esta entrega

- Spring Boot + Maven + Java 21.
- H2 en memoria + Spring Data JPA.
- Swagger/OpenAPI.
- Entidades JPA: Lugar, Sector, Evento, Entrada, Cliente y Venta.
- Enumerados para los estados de las entradas y ventas.
- Endpoints de las 3 historias de usuario **sin implementar** (devuelven HTTP 501).
- Endpoint tecnico de estado para comprobar que levanta el servidor.
- Pruebas de contexto y rutas basicas.

**No incluye**: CRUD, inicializacion de datos, precios por evento-sector,
creacion automatica de entradas, pagos, generacion de QR, validacion de acceso,
transacciones de concurrencia o reglas de negocio.

## Requisitos

1. JDK 21 o superior (Maven compila con nivel Java 21).
2. Apache Maven instalado, o Maven disponible desde el IDE.
3. Conexion a Internet solo en el primer build para descargar dependencias.

## Ejecutar

Desde la carpeta con `pom.xml`, en la terminal:

```bash
mvn clean test
mvn spring-boot:run
```

Alternativamente, ejecutar `SmartTicketApplication.main()` desde el IDE,
una vez que el IDE haya importado las dependencias Maven.

## Comprobar funcionamiento

- Estado: http://localhost:8080/api/v1/estado
- Swagger: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Consola H2: http://localhost:8080/h2-console

En la consola H2 usar:

- JDBC URL: `jdbc:h2:mem:smartticket;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- User Name: `sa`
- Password: dejar vacio

Ejemplo de SQL para inspeccionar tablas:

```sql
SELECT * FROM LUGARES;
SELECT * FROM SECTORES;
SELECT * FROM EVENTOS;
SELECT * FROM ENTRADAS;
SELECT * FROM CLIENTES;
SELECT * FROM VENTAS;
```

Las tablas existen, pero **estan vacias** hasta que se implemente CRUD o carga inicial.
Los datos H2 en memoria desaparecen al detener la aplicacion.

## Rutas preparadas

| Metodo | Ruta | Respuesta actual |
| --- | --- | --- |
| GET | `/api/v1/estado` | 200 OK |
| POST | `/api/v1/cotizaciones/cotizar` | 501 No implementado |
| POST | `/api/v1/pagos/iniciar-pago` | 501 No implementado |
| POST | `/api/v1/pagos/webhook-pagos` | 501 No implementado |
| POST | `/api/v1/accesos/escanear-acceso` | 501 No implementado |

Las rutas POST aun no reciben datos: se agregaran DTOs y reglas al implementarlas.

## Decisiones de diseno

- Paquete base `com.smartticket` para que Spring detecte los subpaquetes.
- Entidades en `model`; API en `controller`; `repository` y `service` reservados.
- Las relaciones son principalmente unidireccionales para evitar ciclos al serializar.
- `Sector` pertenece a `Lugar`, `Evento` a `Lugar`, `Entrada` a `Evento` y `Sector`,
  `Venta` a `Cliente`, y `Entrada` se puede asociar con una `Venta`.
- Se usan `EnumType.STRING` para estados legibles en base de datos.
- El precio corresponde a la combinacion `(evento, sector)`, no a `Sector` en general.
  Una futura entidad `TarifaSector` o `PrecioSectorEvento` resolvera esa asociacion.
- El codigo QR y la hora de ingreso son campos opcionales en esta etapa.
- El codigo futuro debe validar que el sector de una entrada corresponda
  al lugar del evento, controlar capacidad y proteger la venta concurrente.
- `501 Not Implemented` evita simular una compra o un pago que todavia no existe.

## Diferencia con la captura

La captura presenta `EntradaController.java` y `DataInitializer.java`. Esos
archivos parecen corresponder a CRUD y datos de prueba, por lo que **no estan
incluidos** en esta etapa inicial. No se deben reemplazar tus archivos originales
sin antes revisar como funcionan.

## Proximos pasos

1. Etapa 2: CRUD para Lugar, Sector, Evento y Cliente; despues precios y entradas.
2. Etapa 3: cotizacion (HU1), pago simulado (HU2) y acceso QR (HU3),
   separando controlador, servicio y repositorio y aplicando GRASP/SOLID.
