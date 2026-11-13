import React, { useCallback, useEffect, useState } from 'react';
import Navbar from './components/Navbar.jsx';
import StagePanel from './components/StagePanel.jsx';
import CotizacionPanel from './components/CotizacionPanel.jsx';
import PagoPanel from './components/PagoPanel.jsx';
import AccesoPanel from './components/AccesoPanel.jsx';
import { seleccionarCotizacion } from './utils/pago.js';
import ConnectionCard from './components/ConnectionCard.jsx';
import { api } from './services/api.js';
import './App.css';

export default function App() {
  const [activa, setActiva] = useState('cotizacion');
  const [conexion, setConexion] = useState('verificando');
  const [servicio, setServicio] = useState('SmartTicket');
  const [detalle, setDetalle] = useState('');
  const [seleccionPago, setSeleccionPago] = useState(null);
  const [comprobante, setComprobante] = useState(null);
  const [pagoProcesando, setPagoProcesando] = useState(false);
  const [accesoProcesando, setAccesoProcesando] = useState(false);
  const [codigoSugerido, setCodigoSugerido] = useState(null);
  const navegacionBloqueada = pagoProcesando || accesoProcesando;

  const verificarConexion = useCallback(async (signal) => {
    setConexion('verificando');
    setDetalle('');
    try {
      const datos = await api.estado(signal);
      if (datos?.estado !== 'OK') throw new Error('La API no informó estado OK.');
      setServicio(datos.servicio || 'SmartTicket');
      setConexion('conectado');
    } catch (error) {
      if (error.name === 'AbortError') return;
      setConexion('desconectado');
      setDetalle('Iniciá el backend con mvn spring-boot:run (puerto 8080) y volvé a intentar.');
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    verificarConexion(controller.signal);
    return () => controller.abort();
  }, [verificarConexion]);

  function continuarAlPago(cotizacion) {
    if (navegacionBloqueada) return;
    setSeleccionPago(seleccionarCotizacion(cotizacion));
    setComprobante(null);
    setCodigoSugerido(null);
    setActiva('pago');
  }

  function probarEntrada(entrada) {
    if (navegacionBloqueada || !entrada?.codigoQR) return;
    setCodigoSugerido({ codigoQR: entrada.codigoQR, entradaId: entrada.entradaId });
    setActiva('acceso');
  }

  return (
    <div className="app-layout">
      <Navbar activa={activa} onChange={(seccion) => { if (!navegacionBloqueada) setActiva(seccion); }}
        conexion={conexion} bloqueado={navegacionBloqueada} />
      <main className="contenido">
        <div className="introduccion">
          <div>
            <div className="eyebrow">TP 9 · PROGRAMACIÓN ORIENTADA A OBJETOS</div>
            <h1>SmartTicket.<br /><span>Del backend a la experiencia real.</span></h1>
            <p>Cotizá, pagá y validá el ingreso con nuestro backend Spring Boot. El explorador se integrará en la próxima etapa.</p>
          </div>
          <div className="version"><span>FRONTEND</span><strong>Etapa 4.4</strong><small>HU 1, HU 2 y HU 3 conectadas</small></div>
        </div>
        {activa === 'cotizacion' ? <CotizacionPanel onContinuarPago={continuarAlPago} />
          : activa === 'pago' ? <PagoPanel seleccion={seleccionPago} comprobante={comprobante}
            onComprobante={setComprobante} onProcesando={setPagoProcesando} onProbarEntrada={probarEntrada}
            onVolverCotizacion={() => { if (!navegacionBloqueada) setActiva('cotizacion'); }} />
          : activa === 'acceso' ? <AccesoPanel comprobante={comprobante} codigoSugerido={codigoSugerido}
              onProcesando={setAccesoProcesando} />
          : <StagePanel seccion={activa} />}
        <ConnectionCard conexion={conexion} servicio={servicio} detalle={detalle} onReintentar={() => verificarConexion()} />
        <div className="tarjetas-ayuda">
          <div className="ayuda-item"><span aria-hidden="true">🔌</span><div><strong>API REST real</strong><p>Cotización, pago y acceso se comunican con Spring Boot.</p></div></div>
          <div className="ayuda-item"><span aria-hidden="true">🧮</span><div><strong>Reglas en Java</strong><p>El backend valida stock, pago, emisión y un único ingreso por QR.</p></div></div>
          <div className="ayuda-item"><span aria-hidden="true">🔎</span><div><strong>Próxima etapa</strong><p>Integraremos el explorador de datos del sistema.</p></div></div>
        </div>
      </main>
      <footer className="footer"><span>SmartTicket · GRASP / SOLID</span><span>React 18 · Vite 5 · Spring Boot</span></footer>
    </div>
  );
}
