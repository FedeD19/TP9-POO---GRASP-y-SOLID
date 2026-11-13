/**
 * Cliente REST de SmartTicket. No calcula precios ni modifica inventarios:
 * solo traduce las acciones de React en solicitudes a nuestro backend Java.
 * En desarrollo Vite reenvía /api a http://localhost:8080.
 */
const API_BASE = '/api/v1';

export class ApiError extends Error {
  constructor(message, status = 0, codigo = '') {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.codigo = codigo;
  }
}

async function request(path, { body, headers = {}, ...options } = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      cache: 'no-store',
      ...options,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...headers,
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError('No se pudo conectar con SmartTicket. Verificá que Spring Boot esté funcionando en el puerto 8080.');
  }

  const tipo = response.headers.get('content-type') || '';
  let datos;
  try {
    datos = tipo.includes('application/json') ? await response.json() : await response.text();
  } catch {
    throw new ApiError('La API devolvió una respuesta que no se pudo interpretar.', response.status);
  }

  if (!response.ok) {
    const mensaje = typeof datos === 'object' && datos !== null
      ? (datos.detalle || datos.message || datos.error)
      : undefined;
    throw new ApiError(mensaje || `Error HTTP ${response.status}`, response.status,
      typeof datos === 'object' && datos !== null ? (datos.error || '') : '');
  }
  return datos;
}

export const api = {
  estado: (signal) => request('/estado', { signal }),
  eventos: (signal) => request('/eventos', { signal }),
  inventarioEvento: (eventoId, signal) =>
    request(`/eventos/${encodeURIComponent(eventoId)}/inventario`, { signal }),
  clientes: (signal) => request('/clientes', { signal }),
  iniciarPago: ({ clienteId, eventoId, sectorId, cantidad, tarjetaPrueba }, signal) =>
    request('/pagos/iniciar-pago', {
      method: 'POST',
      body: { clienteId, eventoId, sectorId, cantidad, tarjetaPrueba },
      signal,
    }),
  cotizar: ({ eventoId, sectorId, cantidad }, signal) => request('/cotizaciones/cotizar', {
    method: 'POST',
    body: { eventoId, sectorId, cantidad },
    signal,
  }),
};
