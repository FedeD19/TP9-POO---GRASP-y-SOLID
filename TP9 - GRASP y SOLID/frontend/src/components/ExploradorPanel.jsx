import React, { useEffect, useMemo, useState } from 'react';
import { api } from '../services/api.js';
import { dinero, fechaEvento } from '../utils/formato.js';
import { etiquetaEstado, metricasInventario, paginaValida } from '../utils/explorador.js';
import './ExploradorPanel.css';

const VISTAS = [
  { id: 'resumen', nombre: 'Resumen' },
  { id: 'eventos', nombre: 'Eventos e inventario' },
  { id: 'sectores', nombre: 'Sectores' },
  { id: 'clientes', nombre: 'Clientes' },
  { id: 'ventas', nombre: 'Ventas' },
  { id: 'entradas', nombre: 'Entradas' },
];
const TAMANO = 10;

function MensajeVacio({ children }) {
  return <div className="explorador-vacio" role="status">{children}</div>;
}
function Dato({ nombre, valor }) {
  return <div className="explorador-metrica"><span>{nombre}</span><strong>{valor}</strong></div>;
}
function Estado({ valor }) {
  return <span className={`explorador-estado ${String(valor || '').toLowerCase()}`}>{etiquetaEstado(valor)}</span>;
}
function Tabla({ columnas, filas, render }) {
  if (!filas.length) return <MensajeVacio>No hay registros para mostrar con estos filtros.</MensajeVacio>;
  return (
    <div className="explorador-tabla-scroll">
      <table className="explorador-tabla">
        <thead><tr>{columnas.map((col) => <th key={col} scope="col">{col}</th>)}</tr></thead>
        <tbody>{filas.map((fila) => <tr key={fila.id}>{render(fila)}</tr>)}</tbody>
      </table>
    </div>
  );
}
function Paginas({ resultado, irA, bloqueado }) {
  if (!resultado) return null;
  const desde = resultado.totalElementos ? resultado.pagina * resultado.tamano + 1 : 0;
  const hasta = Math.min((resultado.pagina + 1) * resultado.tamano, resultado.totalElementos);
  return (
    <div className="explorador-paginas">
      <span>Mostrando {desde}–{hasta} de {resultado.totalElementos} registros · Página {resultado.pagina + 1} de {Math.max(1, resultado.totalPaginas)}</span>
      <div>
        <button type="button" disabled={bloqueado || resultado.pagina === 0} onClick={() => irA(resultado.pagina - 1)}>← Anterior</button>
        <button type="button" disabled={bloqueado || resultado.pagina + 1 >= resultado.totalPaginas} onClick={() => irA(resultado.pagina + 1)}>Siguiente →</button>
      </div>
    </div>
  );
}

/** Etapa 4.5: panel de lectura; las únicas mutaciones del negocio continúan en HU 2 y HU 3. */
export default function ExploradorPanel() {
  const [vista, setVista] = useState('resumen');
  const [recarga, setRecarga] = useState(0);
  const [catalogos, setCatalogos] = useState(null);
  const [estadisticas, setEstadisticas] = useState({ entradas: null, ventas: null });
  const [cargandoCatalogos, setCargandoCatalogos] = useState(true);
  const [errorCatalogos, setErrorCatalogos] = useState('');
  const [eventoInventarioId, setEventoInventarioId] = useState('');
  const [inventario, setInventario] = useState(null);
  const [cargandoInventario, setCargandoInventario] = useState(false);
  const [errorInventario, setErrorInventario] = useState('');
  const [eventoFiltroId, setEventoFiltroId] = useState('');
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState(null);
  const [cargandoPagina, setCargandoPagina] = useState(false);
  const [errorPagina, setErrorPagina] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setCargandoCatalogos(true);
    setErrorCatalogos('');
    Promise.all([
      api.eventos(controller.signal), api.lugares(controller.signal),
      api.sectores(controller.signal), api.clientes(controller.signal),
      api.entradasExplorador(0, 1, '', controller.signal),
      api.ventasExplorador(0, 1, controller.signal),
    ]).then(([eventos, lugares, sectores, clientes, entradas, ventas]) => {
      if (![eventos, lugares, sectores, clientes].every(Array.isArray)
          || !paginaValida(entradas) || !paginaValida(ventas)) {
        throw new Error('El backend devolvió datos del explorador con un formato inesperado.');
      }
      setCatalogos({ eventos, lugares, sectores, clientes });
      setEstadisticas({ entradas: entradas.totalElementos, ventas: ventas.totalElementos });
    }).catch((error) => {
      if (error.name !== 'AbortError') {
        setErrorCatalogos(error.message);
        setCatalogos(null);
        setEstadisticas({ entradas: null, ventas: null });
      }
    }).finally(() => { if (!controller.signal.aborted) setCargandoCatalogos(false); });
    return () => controller.abort();
  }, [recarga]);

  useEffect(() => {
    const controller = new AbortController();
    setInventario(null);
    setErrorInventario('');
    if (vista !== 'eventos' || !eventoInventarioId) {
      setCargandoInventario(false);
      return () => controller.abort();
    }
    setCargandoInventario(true);
    api.inventarioEvento(eventoInventarioId, controller.signal)
      .then((datos) => {
        if (!Array.isArray(datos?.sectores)) throw new Error('Formato de inventario inesperado.');
        setInventario(datos);
      })
      .catch((error) => { if (error.name !== 'AbortError') setErrorInventario(error.message); })
      .finally(() => { if (!controller.signal.aborted) setCargandoInventario(false); });
    return () => controller.abort();
  }, [vista, eventoInventarioId, recarga]);

  useEffect(() => {
    const controller = new AbortController();
    setResultado(null);
    setErrorPagina('');
    if (vista !== 'entradas' && vista !== 'ventas') {
      setCargandoPagina(false);
      return () => controller.abort();
    }
    setCargandoPagina(true);
    const llamada = vista === 'entradas'
      ? api.entradasExplorador(pagina, TAMANO, eventoFiltroId, controller.signal)
      : api.ventasExplorador(pagina, TAMANO, controller.signal);
    llamada.then((datos) => {
      if (!paginaValida(datos)) throw new Error('Formato de página inesperado.');
      setResultado(datos);
    }).catch((error) => {
      if (error.name !== 'AbortError') setErrorPagina(error.message);
    }).finally(() => { if (!controller.signal.aborted) setCargandoPagina(false); });
    return () => controller.abort();
  }, [vista, pagina, eventoFiltroId, recarga]);

  const lugaresPorId = useMemo(() => new Map((catalogos?.lugares || []).map((l) => [l.id, l.nombre])), [catalogos]);
  const metricas = useMemo(() => metricasInventario(inventario?.sectores), [inventario]);
  const hayError = errorCatalogos || errorPagina || errorInventario;

  function cambiarVista(id) {
    setVista(id);
    setPagina(0);
  }
  function actualizar() {
    setPagina(0);
    setRecarga((anterior) => anterior + 1);
  }
  function seleccionarInventario(id) {
    setEventoInventarioId(id ? String(id) : '');
    setInventario(null);
  }

  return (
    <section className="explorador-escena" aria-labelledby="explorador-titulo">
      <div className="explorador-encabezado">
        <div>
          <div className="etiqueta"><span className="etiqueta-punto"/> Etapa 4.5 · Explorador REST</div>
          <h2 id="explorador-titulo">Todos los datos.<br/><span>Una sola vista.</span></h2>
          <p>Consultá eventos, sectores, clientes, inventario, ventas y estados de entradas. Todo proviene de Spring Boot y H2.</p>
        </div>
        <button className="explorador-actualizar" type="button" disabled={cargandoCatalogos} onClick={actualizar}>
          ↻ Actualizar datos
        </button>
      </div>

      <nav className="explorador-vistas" aria-label="Secciones del explorador">
        {VISTAS.map((item) => <button key={item.id} type="button" className={vista === item.id ? 'seleccionado' : ''}
          aria-current={vista === item.id ? 'page' : undefined} onClick={() => cambiarVista(item.id)}>{item.nombre}</button>)}
      </nav>

      <div className="explorador-contenido">
        {cargandoCatalogos && <p role="status" className="explorador-cargando">Consultando registros en Spring Boot…</p>}
        {errorCatalogos && <p role="alert" className="explorador-error">{errorCatalogos}. Verificá la API y volvé a actualizar.</p>}
        {catalogos && <>
          {vista === 'resumen' && <>
            <div className="explorador-metricas">
              <Dato nombre="Eventos" valor={catalogos.eventos.length}/>
              <Dato nombre="Sectores" valor={catalogos.sectores.length}/>
              <Dato nombre="Clientes" valor={catalogos.clientes.length}/>
              <Dato nombre="Ventas" valor={estadisticas.ventas ?? '—'}/>
              <Dato nombre="Entradas" valor={estadisticas.entradas ?? '—'}/>
            </div>
            <div className="explorador-ayuda">
              <h3>¿Qué podés explorar?</h3>
              <p>En <strong>Eventos e inventario</strong> se muestran precios, disponibilidad y estados por sector. En <strong>Ventas</strong> y <strong>Entradas</strong> podés seguir los resultados de las Historias de Usuario 2 y 3.</p>
              {catalogos.eventos.length === 0 && <p>La base H2 está vacía. Creá lugares, sectores, eventos y clientes desde <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">Swagger UI</a> para comenzar.</p>}
              <p className="explorador-privacidad">Por seguridad, el explorador no revela códigos QR ni datos bancarios. Los códigos de ingreso se entregan exclusivamente en el comprobante de compra.</p>
            </div>
          </>}

          {vista === 'eventos' && <>
            <h3>Eventos registrados <small>({catalogos.eventos.length})</small></h3>
            <Tabla columnas={['ID', 'Evento', 'Fecha y hora', 'Lugar', 'Inventario']} filas={catalogos.eventos} render={(e) => <>
              <td>#{e.id}</td><td><strong>{e.nombre}</strong></td><td>{fechaEvento(e.fechaHora)}</td><td>{lugaresPorId.get(e.lugarId) || `Lugar #${e.lugarId}`}</td>
              <td><button className="explorador-link" type="button" onClick={() => seleccionarInventario(e.id)}>Ver inventario</button></td>
            </>}/>
            {eventoInventarioId && <section className="explorador-inventario" aria-label="Detalle de inventario">
              <div className="explorador-inventario-cabecera">
                <h4>Inventario · Evento #{eventoInventarioId}</h4>
                <button type="button" onClick={() => seleccionarInventario('')}>Cerrar ×</button>
              </div>
              {cargandoInventario && <p role="status">Consultando inventario…</p>}
              {errorInventario && <p role="alert" className="explorador-error">{errorInventario}</p>}
              {inventario && <>
                <div className="explorador-metricas compactas">
                  <Dato nombre="Total" valor={metricas.entradas}/>
                  <Dato nombre="Disponibles" valor={metricas.disponibles}/>
                  <Dato nombre="Reservadas" valor={metricas.reservadas}/>
                  <Dato nombre="Emitidas" valor={metricas.emitidas}/>
                  <Dato nombre="Utilizadas" valor={metricas.utilizadas}/>
                </div>
                <Tabla columnas={['Sector', 'Precio', 'Capacidad', 'Disponibles', 'Reservadas', 'Emitidas', 'Utilizadas']} filas={inventario.sectores.map((s) => ({ ...s, id: s.sectorId }))} render={(s) => <>
                  <td><strong>{s.nombre}</strong></td><td>{dinero(s.precio)}</td><td>{s.capacidadMaxima}</td><td>{s.disponibles}</td><td>{s.reservadas}</td><td>{s.emitidas}</td><td>{s.utilizadas}</td>
                </>}/>
              </>}
            </section>}
          </>}

          {vista === 'sectores' && <>
            <h3>Sectores registrados <small>({catalogos.sectores.length})</small></h3>
            <Tabla columnas={['ID', 'Nombre', 'Lugar', 'Capacidad máxima']} filas={catalogos.sectores} render={(s) => <>
              <td>#{s.id}</td><td><strong>{s.nombre}</strong></td><td>{lugaresPorId.get(s.lugarId) || `Lugar #${s.lugarId}`}</td><td>{s.capacidadMaxima}</td>
            </>}/>
            <p className="explorador-nota">El precio depende del evento, no del sector. Consultalo en Eventos e inventario.</p>
          </>}

          {vista === 'clientes' && <>
            <h3>Clientes registrados <small>({catalogos.clientes.length})</small></h3>
            <Tabla columnas={['ID', 'Nombre', 'Correo electrónico']} filas={catalogos.clientes} render={(c) => <>
              <td>#{c.id}</td><td><strong>{c.nombre}</strong></td><td>{c.email}</td>
            </>}/>
            <p className="explorador-nota">Datos de demostración: este TP no incluye autenticación ni autorización de usuarios. No registres datos personales reales.</p>
          </>}

          {vista === 'ventas' && <>
            <h3>Ventas registradas</h3>
            {cargandoPagina && <p role="status">Cargando ventas…</p>}
            {errorPagina && <p role="alert" className="explorador-error">{errorPagina}</p>}
            {resultado && <>
              <Tabla columnas={['Venta', 'Cliente', 'Estado', 'Entradas', 'Total']} filas={resultado.contenido} render={(v) => <>
                <td>#{v.id}</td><td>{v.cliente} <small>#{v.clienteId}</small></td><td><Estado valor={v.estado}/></td><td>{v.cantidadEntradas}</td><td><strong>{dinero(v.total)}</strong></td>
              </>}/>
              <Paginas resultado={resultado} bloqueado={cargandoPagina} irA={setPagina}/>
            </>}
          </>}

          {vista === 'entradas' && <>
            <div className="explorador-filtros">
              <h3>Entradas registradas</h3>
              <label htmlFor="explorador-evento-filtro">Filtrar por evento
                <select id="explorador-evento-filtro" value={eventoFiltroId} onChange={(e) => { setEventoFiltroId(e.target.value); setPagina(0); }}>
                  <option value="">Todos los eventos</option>
                  {catalogos.eventos.map((e) => <option key={e.id} value={String(e.id)}>{e.nombre} (#{e.id})</option>)}
                </select>
              </label>
            </div>
            {cargandoPagina && <p role="status">Cargando entradas…</p>}
            {errorPagina && <p role="alert" className="explorador-error">{errorPagina}</p>}
            {resultado && <>
              <Tabla columnas={['Entrada', 'Evento', 'Sector', 'Estado', 'Venta', 'Fecha de ingreso']} filas={resultado.contenido} render={(e) => <>
                <td>#{e.id}</td><td>{e.evento} <small>#{e.eventoId}</small></td><td>{e.sector}</td><td><Estado valor={e.estado}/></td><td>{e.ventaId ? `#${e.ventaId}` : '—'}</td><td>{e.fechaHoraIngreso ? fechaEvento(e.fechaHoraIngreso) : '—'}</td>
              </>}/>
              <Paginas resultado={resultado} bloqueado={cargandoPagina} irA={setPagina}/>
            </>}
            <p className="explorador-nota">Las entradas se muestran de 10 en 10 y sin su código QR. El escaneo solo se realiza en Control de acceso.</p>
          </>}
        </>}
        {!hayError && !cargandoCatalogos && !catalogos && <MensajeVacio>Todavía no hay datos para mostrar.</MensajeVacio>}
      </div>
    </section>
  );
}
