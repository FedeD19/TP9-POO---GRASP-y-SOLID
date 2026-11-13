import React from 'react';

export default function ConnectionCard({ conexion, servicio, detalle, onReintentar }) {
  const ok = conexion === 'conectado';
  return (
    <section className="conexion-panel" aria-labelledby="conexion-titulo">
      <div className="conexion-texto">
        <div className="encabezado-pequeno">ESTADO DEL BACKEND</div>
        <h3 id="conexion-titulo">Conexión con Spring Boot</h3>
        <p>React consulta <code>GET /api/v1/estado</code> y las HU 1, HU 2 y HU 3 usan los endpoints de cotización, pagos y acceso.</p>
      </div>
      <div className="conexion-datos">
        <div className={`estado-api ${ok ? 'on' : conexion === 'verificando' ? 'pending' : 'off'}`} role="status" aria-live="polite">
          <span className="indicador" />
          {ok ? 'Servidor conectado' : conexion === 'verificando' ? 'Comprobando conexión…' : 'Servidor no disponible'}
        </div>
        {ok && <p className="respuesta-api">Respuesta: <code>{servicio} · OK</code></p>}
        {conexion === 'desconectado' && <p className="error-api">{detalle}</p>}
        <button className="boton-reintentar" type="button" onClick={onReintentar} disabled={conexion === 'verificando'}>
          {conexion === 'verificando' ? 'Comprobando…' : 'Verificar conexión'}
        </button>
      </div>
    </section>
  );
}
