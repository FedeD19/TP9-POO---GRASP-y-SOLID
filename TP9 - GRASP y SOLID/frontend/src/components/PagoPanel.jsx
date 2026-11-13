import React, { useEffect, useRef, useState } from 'react';
import { api } from '../services/api.js';
import { dinero } from '../utils/formato.js';
import { comprobanteValido, mensajeErrorPago } from '../utils/pago.js';
import './PagoPanel.css';

/** HU 2: interfaz y feedback. El backend valida inventario, total, pago y emisión. */
export default function PagoPanel({ seleccion, comprobante, onVolverCotizacion, onComprobante, onProcesando }) {
  const [clientes, setClientes] = useState([]);
  const [clienteId, setClienteId] = useState('');
  const [tarjetaPrueba, setTarjetaPrueba] = useState('APROBADA');
  const [cargandoClientes, setCargandoClientes] = useState(false);
  const [errorClientes, setErrorClientes] = useState('');
  const [errorPago, setErrorPago] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [recarga, setRecarga] = useState(0);
  const [copiado, setCopiado] = useState('');
  const enviandoRef = useRef(false);

  useEffect(() => {
    if (!seleccion || comprobante) return;
    const controller = new AbortController();
    setCargandoClientes(true);
    setErrorClientes('');
    api.clientes(controller.signal)
      .then((lista) => {
        if (!Array.isArray(lista)) throw new Error('La API devolvió una lista de clientes inválida.');
        setClientes(lista);
        setClienteId((previo) => lista.some((c) => String(c.id) === previo)
          ? previo : String(lista[0]?.id ?? ''));
      })
      .catch((error) => {
        if (error.name === 'AbortError') return;
        setClientes([]);
        setClienteId('');
        setErrorClientes(error.message);
      })
      .finally(() => { if (!controller.signal.aborted) setCargandoClientes(false); });
    return () => controller.abort();
  }, [seleccion, comprobante, recarga]);

  async function pagar(e) {
    e.preventDefault();
    if (enviandoRef.current || comprobante || !seleccion) return;
    setErrorPago('');
    if (!clientes.some((cliente) => String(cliente.id) === clienteId)) {
      setErrorPago('Seleccioná un cliente registrado en SmartTicket antes de pagar.');
      return;
    }
    enviandoRef.current = true;
    setEnviando(true);
    onProcesando(true);
    try {
      const resultado = await api.iniciarPago({
        clienteId: Number(clienteId),
        eventoId: seleccion.eventoId,
        sectorId: seleccion.sectorId,
        cantidad: seleccion.cantidad,
        tarjetaPrueba,
      });
      if (!comprobanteValido(resultado, seleccion.cantidad)) {
        throw new Error('La API devolvió un comprobante inesperado. Revisá en el backend si la venta se registró antes de volver a intentar el pago.');
      }
      onComprobante(resultado);
    } catch (error) {
      setErrorPago(mensajeErrorPago(error));
    } finally {
      enviandoRef.current = false;
      setEnviando(false);
      onProcesando(false);
    }
  }

  async function copiarQR(codigo) {
    try {
      await navigator.clipboard.writeText(codigo);
      setCopiado(codigo);
    } catch {
      setCopiado('error');
    }
  }

  if (!seleccion && !comprobante) {
    return (
      <section className="pago-escena" aria-labelledby="hu2-titulo">
        <div className="pago-encabezado">
          <div className="etiqueta"><span className="etiqueta-punto"/> HU 2 · Pago y emisión</div>
          <h2 id="hu2-titulo">Tus entradas.<br/><span>Un paso más cerca.</span></h2>
          <p>Primero calculá una cotización válida para elegir el evento, el sector y la cantidad.</p>
        </div>
        <div className="pago-vacio">
          <div aria-hidden="true" className="pago-vacio-icono">🎟️</div>
          <h3>Todavía no hay una cotización para pagar</h3>
          <p>La compra empieza en la HU 1. Podrás regresar aquí con el total confirmado por Spring Boot.</p>
          <button className="boton-cotizar pago-accion" type="button" onClick={onVolverCotizacion}>Ir a Cotización <span aria-hidden="true">→</span></button>
        </div>
      </section>
    );
  }

  const compra = comprobante || null;
  return (
    <section className="pago-escena" aria-labelledby="hu2-titulo">
      <div className="pago-encabezado">
        <div>
          <div className="etiqueta"><span className="etiqueta-punto"/> HU 2 · Pago y emisión real</div>
          <h2 id="hu2-titulo">{compra ? 'Compra confirmada.' : 'Confirmá tu compra.'}<br/><span>{compra ? 'Entradas emitidas.' : 'Recibí tus entradas.'}</span></h2>
          <p>{compra
            ? 'La venta fue registrada por SmartTicket y cada entrada tiene su identificador QR único.'
            : 'La pasarela es ficticia: solo admite APROBADA y RECHAZADA. No ingreses números de tarjetas reales.'}</p>
        </div>
        <div className="paso-estado"><span className="paso-bola">02</span><span>PAGO<br/><strong>Etapa 4.3</strong></span></div>
      </div>

      <div className="pago-columnas">
        <div className="pago-principal">
          {compra ? (
            <div className="compra-exitosa" role="status" aria-live="polite">
              <div className="compra-resultado-icono" aria-hidden="true">✓</div>
              <div>
                <div className="compra-label">VENTA PAGADA</div>
                <h3>¡Pago de prueba aprobado!</h3>
                <p>Venta #{compra.ventaId} · Cliente #{compra.clienteId} · {compra.cantidad} entrada{compra.cantidad === 1 ? '' : 's'} emitida{compra.cantidad === 1 ? '' : 's'}</p>
              </div>
              <div className="compra-total"><small>Total confirmado</small><strong>{dinero(compra.total)}</strong></div>
            </div>
          ) : (
            <form onSubmit={pagar} className="pago-formulario" noValidate>
              <div className="form-titulo"><h3>Datos del comprador</h3><span>02 / 03</span></div>
              <label htmlFor="hu2-cliente" className="campo-etiqueta">Cliente registrado</label>
              <div className="control-con-accion">
                <select id="hu2-cliente" value={clienteId} onChange={(e) => { setClienteId(e.target.value); setErrorPago(''); }}
                  disabled={cargandoClientes || enviando || clientes.length === 0}>
                  {clientes.length === 0 && <option value="">{cargandoClientes ? 'Cargando clientes…' : 'No hay clientes registrados'}</option>}
                  {clientes.map((c) => <option key={c.id} value={String(c.id)}>{c.nombre} ({c.email})</option>)}
                </select>
                <button type="button" className="boton-recargar" title="Actualizar clientes" aria-label="Actualizar clientes"
                  disabled={cargandoClientes || enviando} onClick={() => setRecarga((v) => v + 1)}>↻</button>
              </div>
              {errorClientes && <p className="mensaje-error" role="alert">{errorClientes}</p>}
              {!cargandoClientes && !errorClientes && clientes.length === 0 && (
                <div className="aviso-vacio" role="status">No hay clientes cargados en H2. Creá uno desde{' '}
                  <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">Swagger UI</a> (POST /api/v1/clientes) y actualizá la lista.
                </div>
              )}
              <fieldset className="pago-metodos" disabled={enviando}>
                <legend className="campo-etiqueta">Resultado del pago simulado</legend>
                <label className={`pago-opcion ${tarjetaPrueba === 'APROBADA' ? 'elegida' : ''}`}>
                  <input type="radio" name="tarjetaPrueba" value="APROBADA" checked={tarjetaPrueba === 'APROBADA'}
                    onChange={(e) => { setTarjetaPrueba(e.target.value); setErrorPago(''); }}/>
                  <span><strong>APROBADA</strong><small>Confirmar venta y emitir entradas</small></span>
                  <span className="pago-opcion-tag">Éxito</span>
                </label>
                <label className={`pago-opcion ${tarjetaPrueba === 'RECHAZADA' ? 'elegida' : ''}`}>
                  <input type="radio" name="tarjetaPrueba" value="RECHAZADA" checked={tarjetaPrueba === 'RECHAZADA'}
                    onChange={(e) => { setTarjetaPrueba(e.target.value); setErrorPago(''); }}/>
                  <span><strong>RECHAZADA</strong><small>Probar el error HTTP 402</small></span>
                  <span className="pago-opcion-tag">Rechazo</span>
                </label>
              </fieldset>
              {errorPago && <div className="mensaje-error destacado" role="alert">{errorPago}</div>}
              {errorPago && <button type="button" className="pago-enlace" onClick={onVolverCotizacion}>Volver a cotizar y comprobar disponibilidad</button>}
              <button type="submit" className="boton-cotizar pago-accion" disabled={enviando || cargandoClientes || !clienteId || clientes.length === 0}>
                {enviando ? 'Procesando pago de prueba…' : tarjetaPrueba === 'APROBADA' ? 'Simular pago aprobado' : 'Simular pago rechazado'}
                <span aria-hidden="true">→</span>
              </button>
              <p className="nota-formulario">No se realizan cargos reales. Si otro cliente compró antes, el backend devolverá HTTP 409 sin cobrar.</p>
            </form>
          )}

          {compra && (
            <div className="tickets-contenedor" aria-label="Entradas emitidas">
              <div className="form-titulo"><h3>Tus entradas emitidas</h3><span>{compra.entradas.length} CÓDIGOS</span></div>
              <p className="mensaje-suave">Estos códigos son identificadores de texto válidos para SmartTicket, no imágenes QR escaneables. Podrás copiarlos para probar el control de acceso en la HU 3.</p>
              <div className="tickets-lista">
                {compra.entradas.map((entrada, indice) => (
                  <article className="ticket-emitido" key={entrada.entradaId ?? indice}>
                    <div className="ticket-superior"><span>🎟️ Entrada {indice + 1}</span><span className="ticket-estado">{entrada.estado}</span></div>
                    <p>Identificador #{entrada.entradaId}</p>
                    <code className="ticket-codigo">{entrada.codigoQR}</code>
                    <button type="button" className="boton-copiar" onClick={() => copiarQR(entrada.codigoQR)}>
                      {copiado === entrada.codigoQR ? '✓ Código copiado' : 'Copiar código QR'}
                    </button>
                  </article>
                ))}
              </div>
              {copiado === 'error' && <p className="mensaje-error" role="alert">No se pudo copiar automáticamente. Podés seleccionar el texto y copiarlo manualmente.</p>}
              <p className="nota-formulario">Guardá los códigos antes de actualizar la página: este comprobante se conserva solo mientras la aplicación siga abierta.</p>
            </div>
          )}
        </div>

        <aside className="panel-resumen pago-resumen" aria-label="Resumen del pago">
          <div className="form-titulo"><h3>{compra ? 'Comprobante' : 'Tu selección'}</h3><span>03 / 03</span></div>
          <div className="resumen-linea"><span>Evento</span><strong>{seleccion?.evento || `#${compra?.eventoId}`}</strong></div>
          <div className="resumen-linea"><span>Sector</span><strong>{seleccion?.sector || `#${compra?.sectorId}`}</strong></div>
          <div className="resumen-linea"><span>Cantidad</span><strong>{compra?.cantidad ?? seleccion?.cantidad}</strong></div>
          <div className="resumen-linea"><span>Precio unitario cotizado</span><strong>{seleccion ? dinero(seleccion.precioUnitario) : '—'}</strong></div>
          <div className="resumen-total"><span>{compra ? 'Total cobrado (simulado)' : 'Total cotizado'}</span>
            <strong>{dinero(compra?.total ?? seleccion?.totalCotizado)}</strong>
            <small>{compra ? 'Confirmado por Java al pagar.' : 'El backend volverá a calcular el total al pagar.'}</small>
          </div>
          <div className="siguiente-etapa"><span>REGLA DE NEGOCIO</span><strong>La cotización no reserva entradas</strong><small>El precio y la disponibilidad se comprueban de nuevo antes de emitir.</small></div>
          <button className="pago-volver" type="button" disabled={enviando} onClick={onVolverCotizacion}>
            {compra ? 'Cotizar otra compra' : '← Volver a cotización'}
          </button>
        </aside>
      </div>
    </section>
  );
}
