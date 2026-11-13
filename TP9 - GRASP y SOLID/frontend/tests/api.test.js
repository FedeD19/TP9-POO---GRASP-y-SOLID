import test, { afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api, ApiError } from '../src/services/api.js';
import { cantidadValida, dinero } from '../src/utils/formato.js';

const fetchOriginal = globalThis.fetch;
afterEach(() => { globalThis.fetch = fetchOriginal; });

function responder(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}

test('GET eventos consulta la ruta real de Spring Boot', async () => {
  globalThis.fetch = async (url, config) => {
    assert.equal(url, '/api/v1/eventos');
    assert.equal(config.cache, 'no-store');
    return responder([{ id: 7, nombre: 'Rock Fest', lugarId: 1 }]);
  };
  const resultado = await api.eventos();
  assert.equal(resultado[0].id, 7);
});

test('GET inventario incluye el ID del evento elegido', async () => {
  globalThis.fetch = async (url) => {
    assert.equal(url, '/api/v1/eventos/7/inventario');
    return responder({ eventoId: 7, sectores: [{ sectorId: 3, precio: 12000, disponibles: 5 }] });
  };
  const resultado = await api.inventarioEvento(7);
  assert.equal(resultado.sectores[0].disponibles, 5);
});

test('POST cotizar envía eventoId, sectorId y cantidad numéricos', async () => {
  globalThis.fetch = async (url, config) => {
    assert.equal(url, '/api/v1/cotizaciones/cotizar');
    assert.equal(config.method, 'POST');
    assert.equal(config.headers['Content-Type'], 'application/json');
    assert.deepEqual(JSON.parse(config.body), { eventoId: 7, sectorId: 3, cantidad: 2 });
    return responder({ total: 24000, entradasDisponibles: 5 });
  };
  const resultado = await api.cotizar({ eventoId: 7, sectorId: 3, cantidad: 2 });
  assert.equal(resultado.total, 24000);
});

test('Error 400 de Spring Boot conserva el detalle de la validación', async () => {
  globalThis.fetch = async () => responder({ error: 'VALIDACION', detalle: 'Disponibilidad insuficiente' }, 400);
  await assert.rejects(() => api.cotizar({ eventoId: 1, sectorId: 1, cantidad: 100 }), (error) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 400);
    assert.equal(error.codigo, 'VALIDACION');
    assert.equal(error.message, 'Disponibilidad insuficiente');
    return true;
  });
});

test('Una falla de red se convierte en un mensaje comprensible', async () => {
  globalThis.fetch = async () => { throw new TypeError('Failed to fetch'); };
  await assert.rejects(() => api.eventos(), /No se pudo conectar/);
});

test('Validación de cantidades enteras positivas', () => {
  for (const valor of ['1', '2', 3, '100']) assert.equal(cantidadValida(valor), true);
  for (const valor of ['', '0', '-1', '2.5', 'hola']) assert.equal(cantidadValida(valor), false);
});

test('Los precios se presentan en pesos argentinos', () => {
  const resultado = dinero('24000.00');
  assert.match(resultado, /24\.000/);
});
