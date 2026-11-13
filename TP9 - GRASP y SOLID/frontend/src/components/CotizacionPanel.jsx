import React, { useEffect, useMemo, useState } from 'react';
import { api } from '../services/api.js';
import { cantidadValida, dinero, fechaEvento } from '../utils/formato.js';
import './CotizacionPanel.css';

export default function CotizacionPanel({ onContinuarPago }) {
  const [eventos, setEventos] = useState([]);
  const [eventoId, setEventoId] = useState('');
  const [inventario, setInventario] = useState(null);
  const [sectorId, setSectorId] = useState('');
  const [cantidad, setCantidad] = useState('2');
  const [cotizacion, setCotizacion] = useState(null);
  const [errorCotizacion, setErrorCotizacion] = useState('');
  const [errorEventos, setErrorEventos] = useState('');
  const [errorInventario, setErrorInventario] = useState('');
  const [cargandoEventos, setCargandoEventos] = useState(true);
  const [cargandoInventario, setCargandoInventario] = useState(false);
  const [cotizando, setCotizando] = useState(false);
  const [versionEventos, setVersionEventos] = useState(0);
  const [versionInventario, setVersionInventario] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setCargandoEventos(true);
    setErrorEventos('');
    api.eventos(controller.signal)
      .then((datos) => {
        if (!Array.isArray(datos)) throw new Error('La lista de eventos no tiene el formato esperado.');
        setEventos(datos);
        setEventoId((anterior) => datos.some((item) => String(item.id) === anterior)
          ? anterior : String(datos[0]?.id ?? ''));
      })
      .catch((error) => {
        if (error.name === 'AbortError') return;
        setErrorEventos(error.message);
        setEventos([]);
        setEventoId('');
      })
      .finally(() => { if (!controller.signal.aborted) setCargandoEventos(false); });
    return () => controller.abort();
  }, [versionEventos]);

  useEffect(() => {
    const controller = new AbortController();
    setInventario(null);
    setSectorId('');
    setCotizacion(null);
    setErrorCotizacion('');
    setErrorInventario('');
    if (!eventoId) {
      setCargandoInventario(false);
      return () => controller.abort();
    }
    setCargandoInventario(true);
    api.inventarioEvento(eventoId, controller.signal)
      .then((datos) => {
        if (!Array.isArray(datos?.sectores)) {
          throw new Error('El inventario del evento no tiene el formato esperado.');
        }
        setInventario(datos);
        const disponible = datos.sectores.find((item) => Number(item.disponibles) > 0);
        setSectorId(disponible ? String(disponible.sectorId) : '');
      })
      .catch((error) => {
        if (error.name === 'AbortError') return;
        setErrorInventario(error.message);
      })
      .finally(() => { if (!controller.signal.aborted) setCargandoInventario(false); });
    return () => controller.abort();
  }, [eventoId, versionInventario]);

  const evento = useMemo(
    () => eventos.find((item) => String(item.id) === eventoId),
    [eventos, eventoId]
  );
  const sector = useMemo(
    () => inventario?.sectores.find((item) => String(item.sectorId) === sectorId),
    [inventario, sectorId]
  );
  const disponibles = Number(sector?.disponibles ?? 0);
  const totalAproximado = sector && cantidadValida(cantidad)
    ? Number(sector.precio) * Number(cantidad) : null;

  async function cotizar(evento) {
    evento.preventDefault();
    setErrorCotizacion('');
    setCotizacion(null);
    if (!eventoId || !sectorId) {
      setErrorCotizacion('Seleccioná un evento y un sector que tenga entradas disponibles.');
      return;
    }
    if (!cantidadValida(cantidad)) {
      setErrorCotizacion('La cantidad debe ser un número entero mayor a cero.');
      return;
    }
    setCotizando(true);
    try {
      const resultado = await api.cotizar({
        eventoId: Number(eventoId),
        sectorId: Number(sectorId),
        cantidad: Number(cantidad),
      });
      setCotizacion(resultado);
    } catch (error) {
      setErrorCotizacion(error.status ? `HTTP ${error.status}: ${error.message}` : error.message);
    } finally {
      setCotizando(false);
    }
  }

  function seleccionarEvento(e) {
    setEventoId(e.target.value);
    setCotizacion(null);
    setErrorCotizacion('');
  }

  function seleccionarSector(id) {
    setSectorId(String(id));
    setCotizacion(null);
    setErrorCotizacion('');
  }

  return (
    <section className="cotizacion-escena" aria-labelledby="hu1-titulo">
      <div className="cotizacion-encabezado">
        <div>
          <div className="etiqueta"><span className="etiqueta-punto"/> HU 1 · Integración REST real</div>
          <h2 id="hu1-titulo">Elegí tu evento.<br/><span>Conocé tu precio.</span></h2>
          <p>Seleccioná un evento y un sector; SmartTicket consultará las entradas disponibles y calculará la cotización en Spring Boot.</p>
        </div>
        <div className="paso-estado"><span className="paso-bola">01</span><span>COTIZACIÓN<br/><strong>Etapa 4.3</strong></span></div>
      </div>

      <div className="cotizacion-columnas">
        <form className="panel-formulario" onSubmit={cotizar} noValidate>
          <div className="form-titulo"><h3>Configurá tu selección</h3><span>01 / 03</span></div>
          <label className="campo-etiqueta" htmlFor="hu1-evento">Evento</label>
          <div className="control-con-accion">
            <select id="hu1-evento" value={eventoId} onChange={seleccionarEvento}
              disabled={cargandoEventos || cotizando || eventos.length === 0}>
              {eventos.length === 0 && <option value="">{cargandoEventos ? 'Cargando eventos…' : 'No hay eventos disponibles'}</option>}
              {eventos.map((item) => <option key={item.id} value={String(item.id)}>{item.nombre}</option>)}
            </select>
            <button className="boton-recargar" type="button" title="Volver a consultar los eventos"
              aria-label="Actualizar eventos" disabled={cargandoEventos || cotizando}
              onClick={() => setVersionEventos((v) => v + 1)}>↻</button>
          </div>
          {errorEventos && <p className="mensaje-error" role="alert">{errorEventos}</p>}
          {!cargandoEventos && !errorEventos && eventos.length === 0 && (
            <div className="aviso-vacio" role="status">
              Todavía no hay eventos en H2. Creá un lugar, sus sectores y un evento con precios desde{' '}
              <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">Swagger UI</a>, y luego presioná actualizar.
            </div>
          )}
          {evento && <div className="info-evento"><span>📅 {fechaEvento(evento.fechaHora)}</span><span>Evento #{evento.id}</span></div>}

          <div className="fila-titulo-sectores">
            <label className="campo-etiqueta" id="hu1-sector-titulo">Sector</label>
            <button className="texto-accion" type="button" disabled={!eventoId || cargandoInventario || cotizando}
              onClick={() => setVersionInventario((v) => v + 1)}>Actualizar disponibilidad</button>
          </div>
          {cargandoInventario && <p className="mensaje-suave" role="status">Consultando precios y disponibilidad…</p>}
          {errorInventario && <p className="mensaje-error" role="alert">{errorInventario}</p>}
          {!cargandoInventario && inventario && (
            <div className="sectores-lista" role="radiogroup" aria-labelledby="hu1-sector-titulo">
              {inventario.sectores.map((item) => {
                const agotado = Number(item.disponibles) <= 0;
                return <label key={item.sectorId} className={`sector-tarjeta ${String(item.sectorId) === sectorId ? 'seleccionado' : ''} ${agotado ? 'agotado' : ''}`}>
                  <input type="radio" name="sector-hu1" value={item.sectorId}
                    checked={String(item.sectorId) === sectorId}
                    onChange={() => seleccionarSector(item.sectorId)} disabled={agotado || cotizando}/>
                  <span className="sector-datos"><strong>{item.nombre}</strong><small>{agotado ? 'Agotado' : `${item.disponibles} disponible${Number(item.disponibles) !== 1 ? 's' : ''}`}</small></span>
                  <span className="sector-precio">{dinero(item.precio)}</span>
                </label>;
              })}
              {inventario.sectores.length === 0 && <p className="aviso-vacio">Este evento no tiene sectores con precios configurados.</p>}
              {inventario.sectores.length > 0 && !inventario.sectores.some((item) => Number(item.disponibles) > 0)
                && <p className="aviso-vacio">No quedan entradas disponibles para este evento.</p>}
            </div>
          )}

          <label className="campo-etiqueta cantidad-label" htmlFor="hu1-cantidad">Cantidad de entradas</label>
          <div className="cantidad-wrap">
            <input id="hu1-cantidad" type="number" inputMode="numeric" min="1" step="1"
              value={cantidad} onChange={(e) => { setCantidad(e.target.value); setCotizacion(null); setErrorCotizacion(''); }}
              disabled={cotizando} />
            <span>Máximo disponible: {sector ? disponibles : '—'}</span>
          </div>
          <button type="submit" className="boton-cotizar"
            disabled={cotizando || cargandoInventario || cargandoEventos || !eventoId || !sectorId}>
            {cotizando ? 'Calculando cotización…' : 'Calcular cotización'} <span aria-hidden="true">→</span>
          </button>
          {errorCotizacion && <div className="mensaje-error destacado" role="alert">{errorCotizacion}</div>}
          <p className="nota-formulario">La cotización es informativa: no reserva ni emite entradas.</p>
        </form>

        <aside className="panel-resumen" aria-label="Resumen de cotización">
          <div className="form-titulo"><h3>Resumen de selección</h3><span>02 / 03</span></div>
          <div className="resumen-linea"><span>Evento</span><strong>{evento?.nombre || 'Sin seleccionar'}</strong></div>
          <div className="resumen-linea"><span>Sector</span><strong>{sector?.nombre || 'Sin seleccionar'}</strong></div>
          <div className="resumen-linea"><span>Entradas</span><strong>{cantidadValida(cantidad) ? cantidad : '—'}</strong></div>
          <div className="resumen-linea"><span>Precio por entrada</span><strong>{sector ? dinero(sector.precio) : '—'}</strong></div>
          <div className="resumen-total">
            <span>Total estimado</span>
            <strong>{totalAproximado === null ? '—' : dinero(totalAproximado)}</strong>
            <small>Estimación visual. El importe definitivo lo calcula Java.</small>
          </div>
          {cotizacion ? (
            <div className="resultado-cotizacion" role="status" aria-live="polite">
              <div className="resultado-cabecera">✓ Cotización confirmada por el backend</div>
              <div className="resumen-linea"><span>Precio unitario</span><strong>{dinero(cotizacion.precioUnitario)}</strong></div>
              <div className="resumen-linea"><span>Subtotal</span><strong>{dinero(cotizacion.subtotal)}</strong></div>
              <div className="resumen-linea"><span>Disponibles al cotizar</span><strong>{cotizacion.entradasDisponibles}</strong></div>
              <div className="resultado-final"><span>TOTAL</span><strong>{dinero(cotizacion.total)}</strong></div>
              <button className="boton-cotizar continuar-pago" type="button" onClick={() => onContinuarPago(cotizacion)}>
                Continuar a pago simulado <span aria-hidden="true">→</span>
              </button>
            </div>
          ) : (
            <div className="esperando-cotizacion"><span aria-hidden="true">◎</span><p>Cuando presiones <strong>Calcular cotización</strong>, la respuesta real de Spring Boot aparecerá aquí.</p></div>
          )}
          <div className="siguiente-etapa"><span>SIGUIENTE PASO</span><strong>Pago y emisión · HU 2</strong><small>Después de cotizar, continuá al pago ficticio. El stock se comprobará de nuevo.</small></div>
        </aside>
      </div>
    </section>
  );
}
