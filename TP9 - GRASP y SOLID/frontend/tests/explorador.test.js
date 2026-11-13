import test, { afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { api, ApiError } from '../src/services/api.js';
import { etiquetaEstado, metricasInventario, paginaValida } from '../src/utils/explorador.js';

const fetchOriginal = globalThis.fetch;
afterEach(() => { globalThis.fetch = fetchOriginal; });
function responder(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status, headers: { 'content-type': 'application/json' },
  });
}

const paginaEjemplo = {
  contenido: [{ id: 5, estado: 'EMITIDA' }], pagina: 0,
  tamano: 10, totalElementos: 1, totalPaginas: 1,
};

test('GET lugares y sectores usa los controladores existentes', async () => {
  const rutas = [];
  globalThis.fetch = async (url) => { rutas.push(url); return responder([]); };
  await api.lugares();
  await api.sectores();
  assert.deepEqual(rutas, ['/api/v1/lugares', '/api/v1/sectores']);
});

test('GET ventas del explorador incluye pagina y tamano en la URL', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/v1/explorador/ventas?pagina=2&tamano=10');
    assert.equal(options.cache, 'no-store');
    return responder({ ...paginaEjemplo, pagina: 2 });
  };
  const respuesta = await api.ventasExplorador(2, 10);
  assert.equal(respuesta.pagina, 2);
});

test('GET entradas con filtro de evento', async () => {
  globalThis.fetch = async (url) => {
    assert.equal(url, '/api/v1/explorador/entradas?pagina=0&tamano=10&eventoId=7');
    return responder(paginaEjemplo);
  };
  assert.equal((await api.entradasExplorador(0, 10, 7)).totalElementos, 1);
});

test('GET entradas sin evento consulta todas las entradas', async () => {
  globalThis.fetch = async (url) => {
    assert.equal(url, '/api/v1/explorador/entradas?pagina=1&tamano=20');
    return responder({ ...paginaEjemplo, pagina: 1 });
  };
  await api.entradasExplorador(1, 20);
});

test('El explorador conserva errores de validación y estado HTTP', async () => {
  globalThis.fetch = async () => responder({ error: 'VALIDACION', detalle: 'Pagina invalida' }, 400);
  await assert.rejects(() => api.ventasExplorador(-1, 10), (error) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 400);
    assert.equal(error.message, 'Pagina invalida');
    return true;
  });
});

test('Se verifica el contrato de paginación para no mostrar datos incorrectos', () => {
  assert.equal(paginaValida(paginaEjemplo), true);
  assert.equal(paginaValida({ ...paginaEjemplo, contenido: null }), false);
  assert.equal(paginaValida({ ...paginaEjemplo, totalElementos: -1 }), false);
  assert.equal(paginaValida({ ...paginaEjemplo, pagina: '0' }), false);
});

test('Las métricas del inventario suman estados entre sectores', () => {
  const totals = metricasInventario([
    { totalEntradas: 3, disponibles: 1, reservadas: 0, emitidas: 1, utilizadas: 1 },
    { totalEntradas: 2, disponibles: 2, reservadas: 0, emitidas: 0, utilizadas: 0 },
  ]);
  assert.deepEqual(totals, { entradas: 5, disponibles: 3, reservadas: 0, emitidas: 1, utilizadas: 1 });
});

test('Los estados reciben etiquetas comprensibles', () => {
  assert.equal(etiquetaEstado('UTILIZADA'), 'Utilizada');
  assert.equal(etiquetaEstado('PAGADA'), 'Pagada');
  assert.equal(etiquetaEstado('DESCONOCIDA'), 'DESCONOCIDA');
});
