import React, { useEffect, useRef, useState } from 'react';
import { api } from '../services/api.js';
import { accesoConfirmado, codigoQRValido, fechaIngreso, mensajeErrorAcceso } from '../utils/acceso.js';
import './AccesoPanel.css';

/** HU 3: la interfaz solo envía QR y presenta el resultado. Java decide si permite ingresar. */
export default function AccesoPanel({ comprobante, codigoSugerido, onProcesando }) {
  const [codigoQR, setCodigoQR] = useState(codigoSugerido?.codigoQR || '');
  const [procesando, setProcesando] = useState(false);
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState('');
  const enCurso = useRef(false);
  const entradasRecientes = Array.isArray(comprobante?.entradas) ? comprobante.entradas : [];

  // Cuando llegamos desde «Probar acceso» de una entrada concreta, precargar ese QR.
  useEffect(() => {
    if (codigoSugerido?.codigoQR) {
      setCodigoQR(codigoSugerido.codigoQR);
      setResultado(null);
      setError('');
    }
  }, [codigoSugerido]);

  function elegirEntrada(entrada) {
    if (procesando) return;
    setCodigoQR(entrada.codigoQR);
    setError('');
    setResultado(null);
  }

  async function escanear(evento) {
    evento.preventDefault();
    if (enCurso.current) return;
    setError('');
    setResultado(null);

    if (!codigoQRValido(codigoQR)) {
      setError('Ingresá o pegá un código QR antes de escanear.');
      return;
    }

    enCurso.current = true;
    setProcesando(true);
    onProcesando(true);
    try {
      const respuesta = await api.escanearAcceso(codigoQR.trim());
      if (!accesoConfirmado(respuesta)) {
        throw new Error('La API devolvió un resultado de ingreso inesperado. Consultá el estado de la entrada antes de volver a escanearla.');
      }
      setResultado(respuesta);
    } catch (err) {
      setError(mensajeErrorAcceso(err));
    } finally {
      enCurso.current = false;
      setProcesando(false);
      onProcesando(false);
    }
  }

  return (
    <section className="acceso-escena" aria-labelledby="hu3-titulo">
      <div className="acceso-encabezado">
        <div>
          <div className="etiqueta"><span className="etiqueta-punto" /> HU 3 · Control de acceso</div>
          <h2 id="hu3-titulo">Un código.<br /><span>Un único ingreso.</span></h2>
          <p>Ingresá el identificador QR de una entrada emitida. El backend verifica el pago,
            el estado y que el evento se celebre hoy antes de registrar el ingreso.</p>
        </div>
        <div className="paso-estado"><span className="paso-bola">03</span><span>ACCESO<br /><strong>Etapa 4.5</strong></span></div>
      </div>
      <div className="acceso-columnas">
        <div className="acceso-principal">
          <form onSubmit={escanear} noValidate className="acceso-formulario">
            <div className="form-titulo"><h3>Escanear entrada</h3><span>HU 3 / 03</span></div>
            <label htmlFor="hu3-codigo" className="campo-etiqueta">Código QR de la entrada</label>
            <textarea id="hu3-codigo" rows="3" spellCheck="false" autoComplete="off"
              placeholder="Pegá aquí el identificador QR (texto)"
              className="acceso-codigo" value={codigoQR} disabled={procesando}
              onChange={(e) => { setCodigoQR(e.target.value); setError(''); setResultado(null); }} />
            <p className="nota-formulario">En esta simulación se introduce el texto del QR; no se utiliza cámara ni se genera una imagen escaneable.</p>
            {entradasRecientes.length > 0 && (
              <div className="acceso-recientes" aria-label="Entradas de la última compra">
                <h4>Entradas de tu última compra</h4>
                <p>Elegí una para completar el código automáticamente, o pegá cualquier otro QR registrado.</p>
                <div className="acceso-chips">
                  {entradasRecientes.filter((entrada) => codigoQRValido(entrada?.codigoQR)).map((entrada, i) => (
                    <button type="button" key={entrada.entradaId ?? i} disabled={procesando}
                      className={`acceso-chip ${codigoQR === entrada.codigoQR ? 'seleccionado' : ''}`}
                      onClick={() => elegirEntrada(entrada)}>
                      Entrada #{entrada.entradaId ?? i + 1}
                    </button>
                  ))}
                </div>
              </div>
            )}
            <button className="boton-cotizar acceso-boton" type="submit" disabled={procesando}>
              {procesando ? 'Validando con Spring Boot…' : 'Verificar y registrar ingreso'}
              {!procesando && <span aria-hidden="true">→</span>}
            </button>
          </form>
          {error && (
            <div className="acceso-resultado acceso-rechazado" role="alert" aria-live="assertive">
              <span className="acceso-icono" aria-hidden="true">✕</span>
              <div><strong>Acceso rechazado</strong><p>{error}</p></div>
            </div>
          )}
          {resultado && (
            <div className="acceso-resultado acceso-permitido" role="status" aria-live="polite">
              <span className="acceso-icono" aria-hidden="true">✓</span>
              <div className="acceso-confirmacion">
                <div className="acceso-kicker">VALIDACIÓN CONFIRMADA POR JAVA</div>
                <h3>{resultado.mensaje}</h3>
                <dl>
                  <div><dt>Entrada</dt><dd>#{resultado.entradaId}</dd></div>
                  <div><dt>Evento</dt><dd>{resultado.evento}</dd></div>
                  <div><dt>Sector</dt><dd>#{resultado.sectorId}</dd></div>
                  <div><dt>Estado</dt><dd>{resultado.estado}</dd></div>
                  <div><dt>Hora de ingreso</dt><dd>{fechaIngreso(resultado.fechaHoraIngreso)}</dd></div>
                </dl>
                <p>El mismo código ya no podrá autorizar otro ingreso. Podés volver a enviarlo para comprobar el rechazo HTTP 409.</p>
              </div>
            </div>
          )}
        </div>
        <aside className="acceso-aside" aria-label="Cómo se valida una entrada">
          <div className="form-titulo"><h3>Reglas de acceso</h3><span>01 / 01</span></div>
          <ol className="acceso-reglas">
            <li><span>01</span>Buscar el QR en H2.</li>
            <li><span>02</span>Comprobar que está EMITIDA y la venta está PAGADA.</li>
            <li><span>03</span>Validar que la fecha del evento es hoy.</li>
            <li><span>04</span>Cambiar a UTILIZADA y registrar fecha y hora.</li>
          </ol>
          <div className="acceso-tipos">
            <span>200 · Permitido</span>
            <span>400 · Código vacío</span>
            <span>403 · Sin emisión/pago</span>
            <span>404 · QR inexistente</span>
            <span>409 · Ya usado / otra fecha</span>
          </div>
          <div className="siguiente-etapa"><span>RECORDATORIO</span><strong>El evento debe ser de hoy</strong>
            <small>Si compraste para otra fecha, la entrada será válida para ese día, pero no permitirá ingresar hoy.</small></div>
          <p className="acceso-endpoint"><strong>ENDPOINT</strong><code>POST /api/v1/accesos/escanear-acceso</code></p>
        </aside>
      </div>
    </section>
  );
}
