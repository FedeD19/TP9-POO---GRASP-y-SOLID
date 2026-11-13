import test, { afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api, ApiError } from '../src/services/api.js';
import { accesoConfirmado, codigoQRValido, fechaIngreso, mensajeErrorAcceso } from '../src/utils/acceso.js';

const fetchOriginal = globalThis.fetch;
afterEach(() => { globalThis.fetch = fetchOriginal; });
const responder = (body, status = 200) => new Response(JSON.stringify(body), {
  status, headers: { 'content-type': 'application/json' },
});
const accesoOk = {
  mensaje: 'Acceso Permitido', entradaId: 25, eventoId: 6,
  evento: 'Rock Fest', sectorId: 3, estado: 'UTILIZADA',
  fechaHoraIngreso: '2026-11-12T21:20:00',
};

test('POST escanear-acceso envía solo codigoQR al endpoint real', async () => {
  globalThis.fetch = async (url, opciones) => {
    assert.equal(url, '/api/v1/accesos/escanear-acceso');
    assert.equal(opciones.method, 'POST');
    assert.equal(opciones.cache, 'no-store');
    assert.equal(opciones.headers['Content-Type'], 'application/json');
    assert.deepEqual(JSON.parse(opciones.body), { codigoQR: 'qr-valido-abc' });
    return responder(accesoOk);
  };
  const resultado = await api.escanearAcceso('qr-valido-abc');
  assert.ok(accesoConfirmado(resultado));
  assert.equal(resultado.estado, 'UTILIZADA');
});

test('Se rechaza una respuesta que no confirma ingreso y estado UTILIZADA', () => {
  assert.equal(accesoConfirmado(accesoOk), true);
  assert.equal(accesoConfirmado({ ...accesoOk, estado: 'EMITIDA' }), false);
  assert.equal(accesoConfirmado({ ...accesoOk, mensaje: 'Error' }), false);
  assert.equal(accesoConfirmado({ ...accesoOk, fechaHoraIngreso: '' }), false);
  assert.equal(accesoConfirmado(null), false);
});

test('Se admite cualquier identificador opaco no vacío y se rechazan blancos', () => {
  assert.equal(codigoQRValido('  abc-123  '), true);
  assert.equal(codigoQRValido('identificador-no-uuid'), true);
  for (const invalido of ['', '   ', null, undefined, 17]) {
    assert.equal(codigoQRValido(invalido), false);
  }
});

test('HTTP 400 conserva el error de validación del backend', async () => {
  globalThis.fetch = async () => responder({ error: 'VALIDACION', detalle: 'codigoQR: El codigo QR es obligatorio' }, 400);
  await assert.rejects(() => api.escanearAcceso(''), (error) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 400);
    assert.match(mensajeErrorAcceso(error), /QR inválido/);
    return true;
  });
});

test('HTTP 403 representa entrada sin emitir o pago pendiente', async () => {
  globalThis.fetch = async () => responder({ error: 'ACCESO_DENEGADO', detalle: 'Ticket no emitido / Falta de pago' }, 403);
  await assert.rejects(() => api.escanearAcceso('qr-disponible'), (error) => {
    assert.equal(error.status, 403);
    assert.equal(error.codigo, 'ACCESO_DENEGADO');
    assert.match(mensajeErrorAcceso(error), /no está emitida|no está pagada/);
    return true;
  });
});

test('HTTP 404 representa código inexistente', async () => {
  globalThis.fetch = async () => responder({ error: 'NO_ENCONTRADO', detalle: 'Ticket Inválido o Inexistente' }, 404);
  await assert.rejects(() => api.escanearAcceso('no-existe'), (error) => {
    assert.equal(error.status, 404);
    assert.match(mensajeErrorAcceso(error), /no corresponde a ninguna entrada/);
    return true;
  });
});

test('HTTP 409 por QR duplicado presenta el mensaje específico', async () => {
  globalThis.fetch = async () => responder({ error: 'CONFLICTO', detalle: 'Entrada ya utilizada' }, 409);
  await assert.rejects(() => api.escanearAcceso('ya-usada'), (error) => {
    assert.equal(error.status, 409);
    assert.match(mensajeErrorAcceso(error), /ya fue utilizada/);
    return true;
  });
});

test('HTTP 409 por fecha equivocada no se confunde con uso duplicado', async () => {
  globalThis.fetch = async () => responder({ error: 'CONFLICTO', detalle: 'La entrada no corresponde a un evento de hoy' }, 409);
  await assert.rejects(() => api.escanearAcceso('fecha-incorrecta'), (error) => {
    assert.equal(error.status, 409);
    assert.match(mensajeErrorAcceso(error), /otra fecha/);
    assert.doesNotMatch(mensajeErrorAcceso(error), /ya fue utilizada/);
    return true;
  });
});

test('La falla de red no se presenta como QR inexistente', async () => {
  globalThis.fetch = async () => { throw new TypeError('Failed to fetch'); };
  await assert.rejects(() => api.escanearAcceso('qr'), (error) => {
    assert.equal(error.status, 0);
    assert.match(mensajeErrorAcceso(error), /No se pudo conectar/);
    return true;
  });
});

test('La fecha del backend se puede representar y valores inválidos no rompen la UI', () => {
  assert.match(fechaIngreso('2026-11-12T21:20:00'), /2026|26/);
  assert.equal(fechaIngreso(''), '—');
  assert.equal(fechaIngreso('fecha-rara'), 'fecha-rara');
});
