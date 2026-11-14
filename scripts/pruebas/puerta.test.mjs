import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { exigeComprobacion, pluginsDelEnvio, PLUGINS } from '../comprobar.mjs';

test('solo subir o aplicar cambios exige la comprobación', () => {
  assert.equal(exigeComprobacion('push'), true);
  assert.equal(exigeComprobacion('reiniciar'), true);
  assert.equal(exigeComprobacion('pull'), false);
  assert.equal(exigeComprobacion('copia'), false);
  assert.equal(exigeComprobacion('status'), false);
  assert.equal(exigeComprobacion(undefined), false);
});

test('el envío reconstruye solo los jars nuestros que van a subirse', () => {
  assert.deepEqual(pluginsDelEnvio(['server.properties', 'plugins/EssentialsX.jar']), []);
  assert.deepEqual(pluginsDelEnvio(['plugins/Parcelas.jar', 'plugins/Parcelas/config.yml']), ['parcelas']);
  assert.deepEqual(pluginsDelEnvio(['plugins\\Viajes.jar', './plugins/Viajes.jar']), ['viajes']);
  assert.deepEqual(pluginsDelEnvio(['plugins']), PLUGINS.map((plugin) => plugin.id));
  assert.deepEqual(pluginsDelEnvio(['plugins/']), PLUGINS.map((plugin) => plugin.id));
});

test('el sincronizador comprueba antes de entrar al panel', () => {
  const fuente = fs.readFileSync(path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'sync-servidor.mjs'), 'utf8');
  const puerta = fuente.indexOf('if (exigeComprobacion(action))');
  const entrada = fuente.indexOf('const cfg = loadConfig();');
  assert.ok(puerta > 0);
  assert.ok(entrada > puerta);
  assert.match(fuente, /ejecutarComprobacion\(\)/);
  assert.match(fuente, /instalarPlugins\(paths\)/);
  assert.match(fuente, /No subo nada y no aplico cambios/);
});
