/**
 * Mantiene la HU 2 desacoplada de la presentación y del protocolo HTTP.
 * Nunca calcula el total a cobrar: esa responsabilidad sigue en Spring Boot.
 */
export function seleccionarCotizacion(cotizacion) {
  if (!cotizacion || !Number.isSafeInteger(Number(cotizacion.eventoId))
      || Number(cotizacion.eventoId) <= 0
      || !Number.isSafeInteger(Number(cotizacion.sectorId))
      || Number(cotizacion.sectorId) <= 0
      || !Number.isSafeInteger(Number(cotizacion.cantidad))
      || Number(cotizacion.cantidad) <= 0
      || !Number.isFinite(Number(cotizacion.total))) {
    throw new Error('La cotización recibida no tiene los datos necesarios para pagar.');
  }
  return {
    eventoId: Number(cotizacion.eventoId),
    sectorId: Number(cotizacion.sectorId),
    cantidad: Number(cotizacion.cantidad),
    evento: String(cotizacion.evento ?? ''),
    sector: String(cotizacion.sector ?? ''),
    totalCotizado: Number(cotizacion.total),
    precioUnitario: Number(cotizacion.precioUnitario),
  };
}

export function comprobanteValido(respuesta, cantidadEsperada) {
  return Boolean(
    respuesta && respuesta.estado === 'PAGADA'
    && Number.isSafeInteger(Number(respuesta.ventaId))
    && Number(respuesta.ventaId) > 0
    && Number.isFinite(Number(respuesta.total))
    && Number(respuesta.total) > 0
    && Array.isArray(respuesta.entradas)
    && respuesta.entradas.length === Number(cantidadEsperada)
    && respuesta.entradas.every((entrada) => entrada?.estado === 'EMITIDA'
      && typeof entrada.codigoQR === 'string' && entrada.codigoQR.trim().length > 0)
  );
}

export function mensajeErrorPago(error) {
  if (error?.status === 402) {
    return 'Pago rechazado por la pasarela simulada. No se emitieron entradas. Podés probar con APROBADA.';
  }
  if (error?.status === 409) {
    return 'La disponibilidad cambió antes de comprar. Volvé a Cotización, actualizá el inventario y solicitá una nueva cotización.';
  }
  return error?.status ? `HTTP ${error.status}: ${error.message}`
    : (error?.message || 'No se pudo completar la solicitud de pago.');
}
