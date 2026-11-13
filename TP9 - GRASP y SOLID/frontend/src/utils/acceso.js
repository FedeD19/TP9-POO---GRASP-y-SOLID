/** El código QR es un identificador opaco: no debe suponerse un formato UUID. */
export function codigoQRValido(codigo) {
  return typeof codigo === 'string' && codigo.trim().length > 0;
}

/** Comprobación de contrato; no sustituye la validación del backend. */
export function accesoConfirmado(respuesta) {
  return respuesta !== null && typeof respuesta === 'object'
    && respuesta.mensaje === 'Acceso Permitido'
    && respuesta.estado === 'UTILIZADA'
    && Number.isSafeInteger(Number(respuesta.entradaId))
    && Number(respuesta.entradaId) > 0
    && typeof respuesta.evento === 'string' && respuesta.evento.length > 0
    && typeof respuesta.fechaHoraIngreso === 'string'
    && respuesta.fechaHoraIngreso.trim().length > 0;
}

export function mensajeErrorAcceso(error) {
  const detalle = error?.message || '';
  switch (error?.status) {
    case 400:
      return `HTTP 400 · QR inválido: ${detalle || 'Ingresá un código QR antes de escanear.'}`;
    case 403:
      return 'HTTP 403 · Acceso denegado: la entrada no está emitida o su venta no está pagada.';
    case 404:
      return 'HTTP 404 · El código QR no corresponde a ninguna entrada registrada en SmartTicket.';
    case 409:
      if (/utilizada/i.test(detalle)) {
        return 'HTTP 409 · Acceso denegado: esta entrada ya fue utilizada. No puede registrarse un segundo ingreso.';
      }
      if (/hoy|fecha|día|dia/i.test(detalle)) {
        return 'HTTP 409 · Acceso denegado: la entrada corresponde a un evento de otra fecha. Solo se puede ingresar el día del evento.';
      }
      return `Acceso denegado (HTTP 409): ${detalle || 'la entrada no puede utilizarse en este momento.'}`;
    default:
      return error?.status ? `HTTP ${error.status}: ${detalle || 'No se pudo validar la entrada.'}`
        : (detalle || 'No se pudo conectar con SmartTicket.');
  }
}

/** LocalDateTime Java no tiene zona UTC: mostrar el valor como hora local del navegador. */
export function fechaIngreso(fechaHora) {
  if (typeof fechaHora !== 'string' || fechaHora.trim() === '') return '—';
  const fecha = new Date(fechaHora);
  return Number.isNaN(fecha.getTime()) ? fechaHora : fecha.toLocaleString('es-AR', {
    dateStyle: 'short', timeStyle: 'medium',
  });
}
