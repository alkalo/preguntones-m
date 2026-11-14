import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const mundoVivo = path.resolve(repoRoot, 'servidor', 'world');

const RAICES = new Set(['world', 'world_nether', 'world_the_end', 'world-barrio']);

export const AVISO_MUNDO = 'Prohibido reemplazar el mundo de parcelas. world es el barrio que se juega: no se sube, ni por regiones, ni como world-barrio, ni cambiando level-name. Si hay que tocar bloques, se avisa, lo confirmas tú, y se hace sobre una copia del mundo que está ahora en el servidor. Un mundo generado no sirve.';

function partes(ruta) {
  const normal = String(ruta).replace(/\\/g, '/').replace(/^\.?\//, '');
  const salida = [];
  for (const parte of normal.split('/')) {
    if (!parte || parte === '.') continue;
    if (parte === '..') salida.pop();
    else salida.push(parte);
  }
  return salida;
}

export function esMundoParcelas(ruta) {
  const raiz = partes(ruta)[0];
  return raiz != null && RAICES.has(raiz.toLowerCase());
}

export function propiedadesCambianDeMundo(texto) {
  const linea = String(texto).split(/\r?\n/).find((fila) => /^\s*level-name\s*=/.test(fila));
  if (!linea) return false;
  return linea.slice(linea.indexOf('=') + 1).trim() !== 'world';
}

export function prohibirEscribirMundoParcelas(dir) {
  const destino = path.resolve(dir);
  const dentro = destino === mundoVivo || destino.startsWith(mundoVivo + path.sep);
  if (dentro) {
    throw new Error(AVISO_MUNDO);
  }
}

export function pushPisaElMundo(rutas, localRoot) {
  if (rutas.some(esMundoParcelas)) return true;
  return rutas.some((ruta) => {
    if (partes(ruta).join('/') !== 'server.properties') return false;
    const archivo = path.join(localRoot, 'server.properties');
    if (!fs.existsSync(archivo)) return false;
    return propiedadesCambianDeMundo(fs.readFileSync(archivo, 'utf8'));
  });
}
