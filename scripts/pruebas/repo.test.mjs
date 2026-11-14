import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { PLUGINS } from '../comprobar.mjs';

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');

function tienePruebas(dir) {
  for (const entrada of fs.readdirSync(dir, { withFileTypes: true })) {
    const completo = path.join(dir, entrada.name);
    if (entrada.isDirectory() && tienePruebas(completo)) return true;
    if (entrada.name === 'Pruebas.java') return true;
  }
  return false;
}

function leer(relativo) {
  return fs.readFileSync(path.join(repo, relativo), 'utf8');
}

test('cada plugin con compilación entra en la comprobación y tiene pruebas', () => {
  const tools = path.join(repo, 'tools');
  const conScript = fs.readdirSync(tools).filter((nombre) => fs.existsSync(path.join(tools, nombre, 'compilar.ps1')));
  assert.deepEqual(conScript.slice().sort(), PLUGINS.map((plugin) => plugin.id).slice().sort());
  for (const plugin of PLUGINS) {
    const script = leer(path.join('tools', plugin.id, 'compilar.ps1'));
    assert.match(script, /SoloPruebas/, plugin.id);
    assert.match(script, /Pruebas\.java/, plugin.id);
    const yml = leer(path.join('tools', plugin.id, 'plugin.yml'));
    assert.match(yml, /^name:\s+\S+/m, plugin.id);
    assert.match(yml, /^main:\s+\S+/m, plugin.id);
    assert.equal(tienePruebas(path.join(tools, plugin.id, 'src')), true, plugin.id);
  }
});

test('la web y los mensajes de Discord se pueden leer', () => {
  const web = path.join(repo, 'web');
  const paginas = fs.readdirSync(web).filter((nombre) => nombre.endsWith('.html'));
  assert.ok(paginas.length >= 5);
  for (const pagina of paginas) {
    const html = fs.readFileSync(path.join(web, pagina), 'utf8');
    assert.match(html, /<!DOCTYPE html>|<html/i, pagina);
  }
  for (const archivo of ['discord/mensajes.json', 'discord/estructura.json']) {
    const datos = JSON.parse(leer(archivo));
    assert.equal(typeof datos, 'object');
    assert.ok(datos);
  }
});
