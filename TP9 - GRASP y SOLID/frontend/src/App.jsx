import React, { useCallback, useEffect, useState } from 'react';
import Navbar from './components/Navbar.jsx';
import StagePanel from './components/StagePanel.jsx';
import CotizacionPanel from './components/CotizacionPanel.jsx';
import ConnectionCard from './components/ConnectionCard.jsx';
import { api } from './services/api.js';
import './App.css';

export default function App() {
  const [activa, setActiva] = useState('cotizacion');
  const [conexion, setConexion] = useState('verificando');
  const [servicio, setServicio] = useState('SmartTicket');
  const [detalle, setDetalle] = useState('');

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

  return (
    <div className="app-layout">
      <Navbar activa={activa} onChange={setActiva} conexion={conexion} />
      <main className="contenido">
        <div className="introduccion">
          <div>
            <div className="eyebrow">TP 9 · PROGRAMACIÓN ORIENTADA A OBJETOS</div>
            <h1>SmartTicket.<br /><span>Del backend a la experiencia real.</span></h1>
            <p>La cotización ya utiliza datos y precios del backend Spring Boot. Pago, acceso y explorador se integrarán en las siguientes etapas.</p>
          </div>
          <div className="version"><span>FRONTEND</span><strong>Etapa 4.2</strong><small>HU 1 conectada</small></div>
        </div>
        {activa === 'cotizacion' ? <CotizacionPanel /> : <StagePanel seccion={activa} />}
        <ConnectionCard conexion={conexion} servicio={servicio} detalle={detalle} onReintentar={() => verificarConexion()} />
        <div className="tarjetas-ayuda">
          <div className="ayuda-item"><span aria-hidden="true">🔌</span><div><strong>API REST real</strong><p>El formulario consulta los eventos e inventarios de Spring Boot.</p></div></div>
          <div className="ayuda-item"><span aria-hidden="true">🧮</span><div><strong>Reglas en Java</strong><p>La disponibilidad y el total definitivo los valida el backend.</p></div></div>
          <div className="ayuda-item"><span aria-hidden="true">💳</span><div><strong>Etapa 4.3</strong><p>Conectaremos pago, emisión y los códigos de entrada.</p></div></div>
        </div>
      </main>
      <footer className="footer"><span>SmartTicket · GRASP / SOLID</span><span>React 18 · Vite 5 · Spring Boot</span></footer>
    </div>
  );
}
