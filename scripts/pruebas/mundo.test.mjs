import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import {
  AVISO_MUNDO,
  esMundoParcelas,
  prohibirEscribirMundoParcelas,
  propiedadesCambianDeMundo,
  pushPisaElMundo,
} from '../mundo-parcelas.mjs';

test('el barrio y sus dimensiones no se pueden subir', () => {
  for (const ruta of ['world', 'world/region/r.0.0.mca', 'WORLD', 'world_nether', 'world_the_end/DIM1', 'world-barrio', '../world', 'plugins/../world']) {
    assert.equal(esMundoParcelas(ruta), true, ruta);
  }
  for (const ruta of ['plugins/Parcelas.jar', 'server.properties', 'plugins/world-copia.txt']) {
    assert.equal(esMundoParcelas(ruta), false, ruta);
  }
});

test('level-name solo puede seguir siendo world', () => {
  assert.equal(propiedadesCambianDeMundo('level-name=world\nmotd=hola'), false);
  assert.equal(propiedadesCambianDeMundo(' level-name = world '), false);
  assert.equal(propiedadesCambianDeMundo('motd=hola'), false);
  assert.equal(propiedadesCambianDeMundo('level-name=world_nether'), true);
  assert.equal(propiedadesCambianDeMundo('level-name=World'), true);
});

test('un push con mundo o con otro level-name se niega', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'preguntones-'));
  fs.writeFileSync(path.join(dir, 'server.properties'), 'level-name=world\n');
  assert.equal(pushPisaElMundo(['plugins/Parcelas.jar'], dir), false);
  assert.equal(pushPisaElMundo(['world'], dir), true);
  assert.equal(pushPisaElMundo(['server.properties'], dir), false);

  fs.writeFileSync(path.join(dir, 'server.properties'), 'level-name=world_nether\n');
  assert.equal(pushPisaElMundo(['server.properties'], dir), true);
  assert.equal(pushPisaElMundo(['plugins/Otro.jar'], dir), false);

  const vacio = fs.mkdtempSync(path.join(os.tmpdir(), 'preguntones-vacio-'));
  assert.equal(pushPisaElMundo(['server.properties'], vacio), false);
  assert.match(AVISO_MUNDO, /no se sube/);
});

test('no se puede escribir dentro del mundo de parcelas', () => {
  const repo = path.resolve(path.dirname(fileURLToPath(new URL('../mundo-parcelas.mjs', import.meta.url))), '..');
  const mundo = path.join(repo, 'servidor', 'world');
  assert.throws(() => prohibirEscribirMundoParcelas(mundo), /mundo de parcelas/);
  assert.throws(() => prohibirEscribirMundoParcelas(path.join(mundo, 'region')), /mundo de parcelas/);
  const fuera = fs.mkdtempSync(path.join(os.tmpdir(), 'preguntones-fuera-'));
  assert.doesNotThrow(() => prohibirEscribirMundoParcelas(fuera));
});
