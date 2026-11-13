import React from 'react';

const DETALLES = {
  cotizacion: {
    indice: '01', etiqueta: 'HU 1 · Selección y cotización',
    titulo: 'Elegí tu evento. Conocé tu precio.',
    descripcion: 'Los eventos y la cotización ya funcionan en la sección anterior.',
    pasos: ['Seleccionar un evento', 'Elegir sector y cantidad', 'Verificar disponibilidad y cotizar'],
    endpoint: 'POST /api/v1/cotizaciones/cotizar',
  },
  explorador: {
    indice: '04', etiqueta: 'Consulta del sistema',
    titulo: 'El sistema, a la vista.',
    descripcion: 'Mostraremos información de eventos, sectores y clientes a partir de las consultas que ofrece nuestro backend.',
    pasos: ['Consultar entidades disponibles', 'Mostrar precios e inventarios', 'Revisar las respuestas de la API'],
    endpoint: 'GET /api/v1/eventos',
  },
};

export default function StagePanel({ seccion }) {
  const item = DETALLES[seccion] || DETALLES.cotizacion;
  return (
    <section className="panel-etapa" aria-labelledby="etapa-titulo">
      <div className="etapa-info">
        <div className="etiqueta"><span className="etiqueta-punto" /> {item.etiqueta}</div>
        <h2 id="etapa-titulo">{item.titulo}</h2>
        <p>{item.descripcion}</p>
        <div className="estado-etapa"><span>PRÓXIMA ETAPA</span> Integración REST pendiente</div>
      </div>
      <aside className="flujo" aria-label="Flujo de la funcionalidad">
        <div className="flujo-superior"><span>FLUJO PREVISTO</span><span>{item.indice} / 04</span></div>
        <ol>
          {item.pasos.map((p) => <li key={p}>{p}</li>)}
        </ol>
        <div className="endpoint"><span>ENDPOINT</span><code>{item.endpoint}</code></div>
      </aside>
    </section>
  );
}
