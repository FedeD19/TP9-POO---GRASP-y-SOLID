const formateadorMoneda = new Intl.NumberFormat('es-AR', {
  style: 'currency', currency: 'ARS', minimumFractionDigits: 2,
});
const formateadorFecha = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
});

/** Solo presentación: el backend define el importe verdadero. */
export function dinero(numero) {
  const valor = Number(numero);
  return Number.isFinite(valor) ? formateadorMoneda.format(valor) : '—';
}

/** El backend envía LocalDateTime ISO sin zona horaria; lo mostramos como hora local. */
export function fechaEvento(fechaHora) {
  if (!fechaHora) return 'Fecha pendiente';
  const fecha = new Date(fechaHora);
  return Number.isNaN(fecha.getTime()) ? String(fechaHora) : formateadorFecha.format(fecha);
}

/** Validación de interacción; el backend también valida la cantidad. */
export function cantidadValida(valor) {
  const numero = Number(valor);
  return String(valor).trim() !== '' && Number.isSafeInteger(numero) && numero > 0;
}
