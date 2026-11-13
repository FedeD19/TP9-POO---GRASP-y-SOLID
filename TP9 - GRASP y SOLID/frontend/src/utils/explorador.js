/** Formato de las respuestas paginadas; protege la interfaz contra un contrato inesperado. */
export function paginaValida(respuesta) {
  return respuesta !== null && typeof respuesta === 'object'
    && Array.isArray(respuesta.contenido)
    && Number.isInteger(respuesta.pagina) && respuesta.pagina >= 0
    && Number.isInteger(respuesta.tamano) && respuesta.tamano >= 1
    && Number.isInteger(respuesta.totalElementos) && respuesta.totalElementos >= 0
    && Number.isInteger(respuesta.totalPaginas) && respuesta.totalPaginas >= 0;
}

/** Los conteos son los que informa el backend, no se infieren del tamaño de la página. */
export function metricasInventario(sectores = []) {
  return sectores.reduce((total, sector) => ({
    entradas: total.entradas + Number(sector.totalEntradas ?? 0),
    disponibles: total.disponibles + Number(sector.disponibles ?? 0),
    reservadas: total.reservadas + Number(sector.reservadas ?? 0),
    emitidas: total.emitidas + Number(sector.emitidas ?? 0),
    utilizadas: total.utilizadas + Number(sector.utilizadas ?? 0),
  }), { entradas: 0, disponibles: 0, reservadas: 0, emitidas: 0, utilizadas: 0 });
}

export function etiquetaEstado(estado) {
  return ({
    DISPONIBLE: 'Disponible', RESERVADA: 'Reservada', EMITIDA: 'Emitida',
    UTILIZADA: 'Utilizada', PENDIENTE: 'Pendiente', PAGADA: 'Pagada', CANCELADA: 'Cancelada',
  })[estado] ?? estado ?? '—';
}
