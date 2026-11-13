import React from 'react';

export const SECCIONES = [
  { id: 'cotizacion', nombre: 'Cotización', hu: 'HU 1', icono: '🎟️' },
  { id: 'pago', nombre: 'Pago y emisión', hu: 'HU 2', icono: '💳' },
  { id: 'acceso', nombre: 'Control de acceso', hu: 'HU 3', icono: '🛡️' },
  { id: 'explorador', nombre: 'Explorador', hu: 'Datos', icono: '📊' },
];

export default function Navbar({ activa, onChange, conexion, bloqueado = false }) {
  return (
    <header className="cabecera">
      <div className="cabecera-interior">
        <div className="marca" aria-label="SmartTicket">
          <div className="marca-logo" aria-hidden="true">🎫</div>
          <div>
            <strong>Smart<span>Ticket</span></strong>
            <p>Gestión de eventos y accesos</p>
          </div>
        </div>
        <nav className="navegacion" aria-label="Historias de usuario">
          {SECCIONES.map((s) => (
            <button
              key={s.id}
              type="button"
              className={`nav-boton ${activa === s.id ? 'activo' : ''}`}
              aria-current={activa === s.id ? 'page' : undefined}
              disabled={bloqueado}
              onClick={() => onChange(s.id)}
            >
              <span aria-hidden="true">{s.icono}</span>
              <span>{s.nombre}</span>
            </button>
          ))}
        </nav>
        <div className={`conexion-mini ${conexion === 'conectado' ? 'ok' : conexion === 'verificando' ? 'cargando' : 'error'}`} role="status">
          <span className="indicador" />
          {conexion === 'conectado' ? 'API conectada' : conexion === 'verificando' ? 'Verificando API' : 'API desconectada'}
        </div>
      </div>
    </header>
  );
}
