package com.smartticket;

import com.smartticket.dto.AccesoRequest;
import com.smartticket.exception.ConflictoException;
import com.smartticket.model.*;
import com.smartticket.repository.*;
import com.smartticket.service.AccesoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/** Dos puestos de acceso intentan utilizar el mismo QR de forma simultanea. */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcurrenciaAccesoTests {
    private static final String BD =
        "jdbc:h2:mem:smartticket_accesos_concurrentes;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE";

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> BD);
    }

    @Autowired private Environment environment;
    @Autowired private Clock reloj;
    @Autowired private LugarRepository lugares;
    @Autowired private SectorRepository sectores;
    @Autowired private EventoRepository eventos;
    @Autowired private EntradaRepository entradas;
    @Autowired private ClienteRepository clientes;
    @Autowired private VentaRepository ventas;
    @Autowired private AccesoService acceso;

    @Test
    void dosEscaneosSimultaneosSoloPermitenUnIngreso() throws Exception {
        assertEquals(BD, environment.getProperty("spring.datasource.url"));
        Lugar lugar = new Lugar();
        lugar.setNombre("Estadio accesos");
        lugar.setDireccion("Calle test");
        lugar = lugares.save(lugar);

        Sector sector = new Sector();
        sector.setNombre("Platea");
        sector.setCapacidadMaxima(1);
        sector.setLugar(lugar);
        sector = sectores.save(sector);

        Evento evento = new Evento();
        evento.setNombre("Evento de hoy");
        evento.setFechaHora(LocalDate.now(reloj).atTime(21, 0));
        evento.setLugar(lugar);
        evento = eventos.save(evento);

        Cliente cliente = new Cliente();
        cliente.setNombre("Comprador");
        cliente.setEmail("comprador-acceso-concurrente@example.com");
        cliente = clientes.save(cliente);

        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setEstado(EstadoVenta.PAGADA);
        venta.setTotal(new BigDecimal("12000.00"));
        venta = ventas.save(venta);

        Entrada entrada = new Entrada();
        entrada.setEvento(evento);
        entrada.setSector(sector);
        entrada.setVenta(venta);
        entrada.setEstado(EstadoEntrada.EMITIDA);
        entrada.setCodigoQR(UUID.randomUUID().toString());
        entrada = entradas.saveAndFlush(entrada);
        final Long id = entrada.getId();
        final String qr = entrada.getCodigoQR();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Callable<String> accion = () -> {
                inicio.await();
                try {
                    return acceso.escanear(new AccesoRequest(qr)).mensaje();
                } catch (ConflictoException ex) {
                    return ex.getMessage();
                }
            };
            Future<String> primero = pool.submit(accion);
            Future<String> segundo = pool.submit(accion);
            inicio.countDown();
            String r1 = primero.get(15, TimeUnit.SECONDS);
            String r2 = segundo.get(15, TimeUnit.SECONDS);
            assertTrue((r1.equals("Acceso Permitido") && r2.equals("Entrada ya utilizada"))
                || (r2.equals("Acceso Permitido") && r1.equals("Entrada ya utilizada")));
            Entrada finalizada = entradas.findById(id).orElseThrow();
            assertEquals(EstadoEntrada.UTILIZADA, finalizada.getEstado());
            assertNotNull(finalizada.getFechaHoraIngreso());
            assertEquals(1, entradas.countByEvento_IdAndSector_IdAndEstado(
                evento.getId(), sector.getId(), EstadoEntrada.UTILIZADA));
        } finally {
            pool.shutdownNow();
        }
    }
}
