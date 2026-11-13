import React, { useCallback, useEffect, useState } from 'react';
import Navbar from './components/Navbar.jsx';
import StagePanel from './components/StagePanel.jsx';
import CotizacionPanel from './components/CotizacionPanel.jsx';
import PagoPanel from './components/PagoPanel.jsx';
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
    if (pagoProcesando) return;
    setSeleccionPago(seleccionarCotizacion(cotizacion));
    setComprobante(null);
    setActiva('pago');
  }

  return (
    <div className="app-layout">
      <Navbar activa={activa} onChange={(seccion) => { if (!pagoProcesando) setActiva(seccion); }}
        conexion={conexion} bloqueado={pagoProcesando} />
      <main className="contenido">
        <div className="introduccion">
          <div>
            <div className="eyebrow">TP 9 · PROGRAMACIÓN ORIENTADA A OBJETOS</div>
            <h1>SmartTicket.<br /><span>Del backend a la experiencia real.</span></h1>
            <p>Cotizá y simulá la compra de entradas con nuestro backend Spring Boot. El control de acceso y el explorador se integrarán después.</p>
          </div>
          <div className="version"><span>FRONTEND</span><strong>Etapa 4.3</strong><small>HU 1 y HU 2 conectadas</small></div>
        </div>
        {activa === 'cotizacion' ? <CotizacionPanel onContinuarPago={continuarAlPago} />
          : activa === 'pago' ? <PagoPanel seleccion={seleccionPago} comprobante={comprobante}
            onComprobante={setComprobante} onProcesando={setPagoProcesando}
            onVolverCotizacion={() => { if (!pagoProcesando) setActiva('cotizacion'); }} />
          : <StagePanel seccion={activa} />}
        <ConnectionCard conexion={conexion} servicio={servicio} detalle={detalle} onReintentar={() => verificarConexion()} />
        <div className="tarjetas-ayuda">
          <div className="ayuda-item"><span aria-hidden="true">🔌</span><div><strong>API REST real</strong><p>Cotización, clientes y pago se comunican con Spring Boot.</p></div></div>
          <div className="ayuda-item"><span aria-hidden="true">🧮</span><div><strong>Reglas en Java</strong><p>El backend comprueba stock, total, cobro ficticio y emisión.</p></div></div>
          <div className="ayuda-item"><span aria-hidden="true">💳</span><div><strong>Etapa 4.4</strong><p>Conectaremos el escaneo de códigos en el control de acceso.</p></div></div>
        </div>
      </main>
      <footer className="footer"><span>SmartTicket · GRASP / SOLID</span><span>React 18 · Vite 5 · Spring Boot</span></footer>
    </div>
  );
}
