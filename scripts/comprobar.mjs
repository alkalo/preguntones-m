import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

export const PLUGINS = [
  { id: 'viajes', jar: 'Viajes.jar' },
  { id: 'censura', jar: 'Censura.jar' },
  { id: 'recuperar', jar: 'Recuperar.jar' },
  { id: 'mundos', jar: 'Mundos.jar' },
  { id: 'parcelas', jar: 'Parcelas.jar' },
  { id: 'bingo', jar: 'Bingo.jar' },
  { id: 'xo', jar: 'Xo.jar' },
  { id: 'susto', jar: 'Susto.jar' },
  { id: 'reinicio', jar: 'Reinicio.jar' },
  { id: 'impulso', jar: 'Impulso.jar' },
  { id: 'karma', jar: 'Karma.jar' },
];

export function exigeComprobacion(action) {
  return action === 'push' || action === 'reiniciar';
}

function partes(ruta) {
  return String(ruta).replace(/\\/g, '/').replace(/^\.?\//, '').split('/').filter(Boolean);
}

export function pluginsDelEnvio(rutas) {
  const pedidos = [];
  for (const ruta of rutas) {
    const trozos = partes(ruta);
    if (trozos[0]?.toLowerCase() !== 'plugins') continue;
    if (trozos.length === 1) return PLUGINS.map((plugin) => plugin.id);
    const plugin = PLUGINS.find((item) => item.jar.toLowerCase() === trozos[1].toLowerCase());
    if (plugin) pedidos.push(plugin.id);
  }
  return [...new Set(pedidos)];
}

function powershell(script, argumentos) {
  const ejecutable = process.platform === 'win32' ? 'powershell.exe' : 'pwsh';
  const resultado = spawnSync(
    ejecutable,
    ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', script, ...argumentos],
    { cwd: repoRoot, stdio: 'inherit' },
  );
  if (resultado.error) {
    console.error(resultado.error.message);
    return 1;
  }
  return resultado.status === 0 ? 0 : (resultado.status ?? 1);
}

function compilar(id, soloPruebas) {
  const script = path.join(repoRoot, 'tools', id, 'compilar.ps1');
  console.log(soloPruebas ? `Comprobando ${id}.` : `Dejando listo ${id} para subirlo.`);
  return powershell(script, soloPruebas ? ['-SoloPruebas'] : []);
}

export function ejecutarComprobacion() {
  console.log('Comprobando todo antes de tocar el servidor.');
  const dirPruebas = path.join(repoRoot, 'scripts', 'pruebas');
  const archivos = fs.readdirSync(dirPruebas)
    .filter((nombre) => nombre.endsWith('.test.mjs'))
    .map((nombre) => path.join(dirPruebas, nombre));
  const node = spawnSync(process.execPath, ['--test', ...archivos], {
    cwd: repoRoot,
    stdio: 'inherit',
  });
  if (node.error) {
    console.error(node.error.message);
    return 1;
  }
  if (node.status !== 0) return node.status ?? 1;

  for (const plugin of PLUGINS) {
    const codigo = compilar(plugin.id, true);
    if (codigo !== 0) {
      console.error(`Falló ${plugin.id}. No sigo con el resto.`);
      return codigo;
    }
  }
  console.log('Todo compiló y las pruebas pasaron.');
  return 0;
}

export function instalarPlugins(rutas) {
  for (const id of pluginsDelEnvio(rutas)) {
    const codigo = compilar(id, false);
    if (codigo !== 0) {
      console.error(`Falló ${id}. El jar no se copia al servidor local.`);
      return codigo;
    }
  }
  return 0;
}

const esPrincipal = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (esPrincipal) {
  if (!fs.existsSync(path.join(repoRoot, 'scripts', 'pruebas'))) {
    console.error('Faltan las pruebas del sincronizador.');
    process.exit(1);
  }
  process.exit(ejecutarComprobacion());
}
