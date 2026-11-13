import test, { afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api, ApiError } from '../src/services/api.js';
import { seleccionarCotizacion, comprobanteValido, mensajeErrorPago } from '../src/utils/pago.js';

const original = globalThis.fetch;
afterEach(() => { globalThis.fetch = original; });
const responder = (body, status = 200) => new Response(JSON.stringify(body), {
  status, headers: { 'content-type': 'application/json' },
});

const seleccion = {
  eventoId: 4, evento: 'Rock Fest', sectorId: 9, sector: 'Platea',
  cantidad: 2, total: 24000, precioUnitario: 12000,
};
const comprobante = {
  ventaId: 15, estado: 'PAGADA', clienteId: 7, eventoId: 4, sectorId: 9,
  cantidad: 2, total: 24000,
  entradas: [
    { entradaId: 50, estado: 'EMITIDA', codigoQR: 'unico-qr-1' },
    { entradaId: 51, estado: 'EMITIDA', codigoQR: 'unico-qr-2' },
  ],
};

test('GET clientes consulta la ruta correspondiente de Java', async () => {
  globalThis.fetch = async (url) => {
    assert.equal(url, '/api/v1/clientes');
    return responder([{ id: 7, nombre: 'Ana Perez', email: 'ana@example.com' }]);
  };
  const clientes = await api.clientes();
  assert.equal(clientes[0].id, 7);
});

test('POST iniciar-pago envía exactamente el contrato de PagoRequest', async () => {
  globalThis.fetch = async (url, opts) => {
    assert.equal(url, '/api/v1/pagos/iniciar-pago');
    assert.equal(opts.method, 'POST');
    assert.equal(opts.headers['Content-Type'], 'application/json');
    assert.deepEqual(JSON.parse(opts.body), {
      clienteId: 7, eventoId: 4, sectorId: 9, cantidad: 2, tarjetaPrueba: 'APROBADA',
    });
    return responder(comprobante);
  };
  const resultado = await api.iniciarPago({
    clienteId: 7, eventoId: 4, sectorId: 9, cantidad: 2, tarjetaPrueba: 'APROBADA',
  });
  assert.equal(resultado.entradas.length, 2);
  assert.ok(comprobanteValido(resultado, 2));
});

test('HTTP 402 se interpreta como rechazo de la pasarela ficticia', async () => {
  globalThis.fetch = async () => responder({ error: 'PAGO_DENEGADO', detalle: 'Pago denegado' }, 402);
  await assert.rejects(() => api.iniciarPago({
    clienteId: 7, eventoId: 4, sectorId: 9, cantidad: 2, tarjetaPrueba: 'RECHAZADA',
  }), (error) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 402);
    assert.equal(error.codigo, 'PAGO_DENEGADO');
    assert.match(mensajeErrorPago(error), /rechazado/);
    return true;
  });
});

test('HTTP 409 exige volver a cotizar por pérdida de disponibilidad', async () => {
  globalThis.fetch = async () => responder({ error: 'CONFLICTO', detalle: 'No hay entradas' }, 409);
  await assert.rejects(() => api.iniciarPago({
    clienteId: 7, eventoId: 4, sectorId: 9, cantidad: 2, tarjetaPrueba: 'APROBADA',
  }), (error) => error.status === 409 && /disponibilidad/.test(mensajeErrorPago(error)));
});

test('Cotización confirmada produce una selección numérica con total informativo', () => {
  assert.deepEqual(seleccionarCotizacion(seleccion), {
    eventoId: 4, sectorId: 9, cantidad: 2, evento: 'Rock Fest', sector: 'Platea',
    totalCotizado: 24000, precioUnitario: 12000,
  });
  assert.throws(() => seleccionarCotizacion({ ...seleccion, cantidad: 0 }));
});

test('Solo se muestra comprobante cuando venta PAGADA y todas entradas EMITIDA con QR', () => {
  assert.ok(comprobanteValido(comprobante, 2));
  assert.equal(comprobanteValido({ ...comprobante, estado: 'PENDIENTE' }, 2), false);
  assert.equal(comprobanteValido({ ...comprobante, entradas: [comprobante.entradas[0]] }, 2), false);
  assert.equal(comprobanteValido({ ...comprobante,
    entradas: [{ entradaId: 50, estado: 'DISPONIBLE', codigoQR: 'qr' }, comprobante.entradas[1]] }, 2), false);
  assert.equal(comprobanteValido({ ...comprobante,
    entradas: [{ entradaId: 50, estado: 'EMITIDA', codigoQR: '' }, comprobante.entradas[1]] }, 2), false);
});
