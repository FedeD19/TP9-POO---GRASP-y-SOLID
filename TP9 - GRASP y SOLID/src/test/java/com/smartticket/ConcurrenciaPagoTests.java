package com.smartticket;

import com.smartticket.dto.PagoRequest;
import com.smartticket.exception.ConflictoException;
import com.smartticket.model.*;
import com.smartticket.repository.*;
import com.smartticket.service.PagoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/** Dos transacciones independientes compiten por la ultima entrada. */
@SpringBootTest
// Las compras de los hilos confirman transacciones reales, de modo que esta
// clase usa su propia H2. La propiedad dinámica tiene prioridad sobre
// application.properties y sobre la configuración normal de la aplicación.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcurrenciaPagoTests {
    private static final String BD_CONCURRENCIA =
        "jdbc:h2:mem:smartticket_concurrencia;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE";

    @DynamicPropertySource
    static void configurarBaseDeDatos(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> BD_CONCURRENCIA);
    }

    @Autowired private Environment environment;
    @Autowired private LugarRepository lugares;
    @Autowired private SectorRepository sectores;
    @Autowired private EventoRepository eventos;
    @Autowired private PrecioSectorRepository precios;
    @Autowired private EntradaRepository entradas;
    @Autowired private ClienteRepository clientes;
    @Autowired private VentaRepository ventas;
    @Autowired private PagoService pagos;

    @Test
    void dosCompradoresNoPuedenEmitirLaMismaEntrada() throws Exception {
        // Falla de forma clara si esta clase se ejecuta contra la BD compartida.
        assertEquals(BD_CONCURRENCIA, environment.getProperty("spring.datasource.url"));
        Lugar l = new Lugar();
        l.setNombre("Teatro");
        l.setDireccion("Calle 1");
        l = lugares.save(l);

        Sector s = new Sector();
        s.setNombre("Unico");
        s.setCapacidadMaxima(1);
        s.setLugar(l);
        s = sectores.save(s);

        Evento e = new Evento();
        e.setNombre("Concierto");
        e.setFechaHora(LocalDateTime.of(2026, 12, 20, 21, 0));
        e.setLugar(l);
        e = eventos.save(e);
        precios.save(new PrecioSector(e, s, new BigDecimal("12000.00")));

        Entrada entrada = new Entrada();
        entrada.setEvento(e);
        entrada.setSector(s);
        entrada.setEstado(EstadoEntrada.DISPONIBLE);
        entrada.setCodigoQR(UUID.randomUUID().toString());
        entradas.save(entrada);

        Cliente a = new Cliente();
        a.setNombre("Comprador A");
        a.setEmail("compradora-concurrencia@example.com");
        a = clientes.save(a);
        Cliente b = new Cliente();
        b.setNombre("Comprador B");
        b.setEmail("compradorb-concurrencia@example.com");
        b = clientes.save(b);
        final long eventoId = e.getId(), sectorId = s.getId();
        final long aId = a.getId(), bId = b.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Callable<String> primerPago = () -> comprar(inicio, aId, eventoId, sectorId);
            Callable<String> segundoPago = () -> comprar(inicio, bId, eventoId, sectorId);
            Future<String> f1 = executor.submit(primerPago);
            Future<String> f2 = executor.submit(segundoPago);
            inicio.countDown();
            String r1 = f1.get(15, TimeUnit.SECONDS);
            String r2 = f2.get(15, TimeUnit.SECONDS);
            assertTrue((r1.equals("PAGADA") && r2.equals("SIN_STOCK"))
                    || (r2.equals("PAGADA") && r1.equals("SIN_STOCK")));
            assertEquals(1, ventas.count());
            assertEquals(1, entradas.countByEvento_IdAndSector_IdAndEstado(
                eventoId, sectorId, EstadoEntrada.EMITIDA));
            assertEquals(0, entradas.countByEvento_IdAndSector_IdAndEstado(
                eventoId, sectorId, EstadoEntrada.DISPONIBLE));
        } finally {
            executor.shutdownNow();
        }
    }

    private String comprar(CountDownLatch inicio, long cliente, long evento, long sector) throws Exception {
        inicio.await();
        try {
            return pagos.iniciarPago(new PagoRequest(cliente, evento, sector, 1, "APROBADA"))
                .estado().name();
        } catch (ConflictoException e) {
            return "SIN_STOCK";
        }
    }
}
