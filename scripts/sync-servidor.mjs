import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { ejecutarComprobacion, exigeComprobacion, instalarPlugins } from './comprobar.mjs';
import { AVISO_MUNDO, esMundoParcelas, pushPisaElMundo } from './mundo-parcelas.mjs';

const require = createRequire(import.meta.url);
const { Client } = require('ssh2');

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const localRoot = path.join(repoRoot, 'servidor');
const configPath = process.env.GAMZY_CONFIG || path.join(process.env.USERPROFILE || '', '.preguntones', 'conexion.json');

const DEFAULT_PATHS = [
  'world',
  'server.properties',
  'eula.txt',
  'ops.json',
  'whitelist.json',
  'banned-players.json',
  'banned-ips.json',
  'usercache.json',
];

function loadConfig() {
  if (!fs.existsSync(configPath)) {
    throw new Error(`No está el archivo de conexión: ${configPath}`);
  }
  const cfg = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  for (const key of ['panelUrl', 'serverUrl', 'serverId', 'panelUser', 'panelPassword']) {
    if (!cfg[key]) throw new Error(`Falta ${key} en ${configPath}`);
  }
  if (!cfg.sftp?.host || !cfg.sftp?.port || !cfg.sftp?.username) {
    throw new Error(`Faltan datos SFTP en ${configPath}`);
  }
  return cfg;
}

function cookieJar() {
  const jar = new Map();
  return {
    store(res) {
      const list = typeof res.headers.getSetCookie === 'function' ? res.headers.getSetCookie() : [];
      for (const cookie of list) {
        const pair = cookie.split(';')[0];
        const eq = pair.indexOf('=');
        if (eq > 0) jar.set(pair.slice(0, eq).trim(), pair.slice(eq + 1).trim());
      }
    },
    header() {
      return [...jar.entries()].map(([key, value]) => `${key}=${value}`).join('; ');
    },
    xsrf() {
      return decodeURIComponent(jar.get('XSRF-TOKEN') || '');
    },
  };
}

async function panelLogin(cfg) {
  const jar = cookieJar();
  const loginUrl = `${cfg.panelUrl}/auth/login`;
  const first = await fetch(loginUrl, { redirect: 'manual' });
  jar.store(first);
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
    'X-XSRF-TOKEN': jar.xsrf(),
    Referer: loginUrl,
    Origin: cfg.panelUrl,
    Cookie: jar.header(),
  };
  const res = await fetch(loginUrl, {
    method: 'POST',
    redirect: 'manual',
    headers,
    body: JSON.stringify({ user: cfg.panelUser, password: cfg.panelPassword }),
  });
  jar.store(res);
  const text = await res.text();
  if (!res.ok) {
    throw new Error(`El panel rechazó el acceso (${res.status}): ${text.slice(0, 240)}`);
  }
  let data = null;
  try { data = JSON.parse(text); } catch { /* el panel a veces responde vacío */ }
  if (data?.data?.complete === false) {
    throw new Error('La cuenta del panel pide un segundo factor. Entra una vez en la web y vuelve a lanzar el sincronizador.');
  }
  return jar;
}

async function panelFetch(cfg, jar, apiPath, options = {}) {
  const res = await fetch(`${cfg.panelUrl}${apiPath}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
      'X-XSRF-TOKEN': jar.xsrf(),
      Referer: cfg.serverUrl,
      Origin: cfg.panelUrl,
      Cookie: jar.header(),
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers || {}),
    },
  });
  jar.store(res);
  const text = await res.text();
  return { res, text };
}

async function serverState(cfg, jar) {
  const { res, text } = await panelFetch(cfg, jar, `/api/client/servers/${cfg.serverId}/resources`);
  if (!res.ok) throw new Error(`No pude leer el estado del servidor (${res.status}): ${text.slice(0, 240)}`);
  const json = JSON.parse(text);
  return json.attributes?.current_state || 'desconocido';
}

async function sendCommand(cfg, jar, command) {
  const { res, text } = await panelFetch(cfg, jar, `/api/client/servers/${cfg.serverId}/command`, {
    method: 'POST',
    body: JSON.stringify({ command }),
  });
  if (!res.ok && res.status !== 204) {
    throw new Error(`El comando no se envió (${res.status}): ${text.slice(0, 240)}`);
  }
}

async function sendPower(cfg, jar, signal) {
  const { res, text } = await panelFetch(cfg, jar, `/api/client/servers/${cfg.serverId}/power`, {
    method: 'POST',
    body: JSON.stringify({ signal }),
  });
  if (!res.ok && res.status !== 204) {
    throw new Error(`No pude ${signal} el servidor (${res.status}): ${text.slice(0, 240)}`);
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForState(cfg, jar, wanted, timeoutMs) {
  const start = Date.now();
  let state = await serverState(cfg, jar);
  while (state !== wanted && Date.now() - start < timeoutMs) {
    await sleep(2000);
    state = await serverState(cfg, jar);
  }
  if (state !== wanted) throw new Error(`El servidor sigue en «${state}» y esperaba «${wanted}».`);
  return state;
}

function connectSftp(cfg) {
  return new Promise((resolve, reject) => {
    const conn = new Client();
    conn.on('ready', () => {
      conn.sftp((err, sftp) => (err ? reject(err) : resolve({ conn, sftp })));
    });
    conn.on('error', reject);
    conn.connect({
      host: cfg.sftp.host,
      port: cfg.sftp.port,
      username: cfg.sftp.username,
      password: cfg.panelPassword,
      readyTimeout: 20000,
    });
  });
}

function stat(sftp, remote) {
  return new Promise((resolve, reject) => sftp.stat(remote, (err, stats) => (err ? reject(err) : resolve(stats))));
}

function readdir(sftp, remote) {
  return new Promise((resolve, reject) => sftp.readdir(remote, (err, list) => (err ? reject(err) : resolve(list))));
}

function mkdir(sftp, remote) {
  return new Promise((resolve) => sftp.mkdir(remote, () => resolve()));
}

function fastGet(sftp, remote, local) {
  return new Promise((resolve, reject) => sftp.fastGet(remote, local, (err) => (err ? reject(err) : resolve())));
}

function fastPut(sftp, local, remote) {
  return new Promise((resolve, reject) => sftp.fastPut(local, remote, (err) => (err ? reject(err) : resolve())));
}

function missing(err) {
  const code = err?.code;
  return code === 2 || code === 'ENOENT' || /no such file/i.test(err?.message || '');
}

async function downloadPath(sftp, remote, local) {
  let stats;
  try {
    stats = await stat(sftp, remote);
  } catch (err) {
    if (missing(err)) {
      console.log(`no está en el servidor: ${remote}`);
      return;
    }
    throw err;
  }
  if (stats.isDirectory()) {
    fs.mkdirSync(local, { recursive: true });
    for (const item of await readdir(sftp, remote)) {
      if (item.filename === '.' || item.filename === '..' || item.filename === 'session.lock') continue;
      await downloadPath(sftp, `${remote}/${item.filename}`, path.join(local, item.filename));
    }
    return;
  }
  fs.mkdirSync(path.dirname(local), { recursive: true });
  await fastGet(sftp, remote, local);
  console.log(`bajado ${remote}`);
}

async function uploadPath(sftp, local, remote) {
  if (esMundoParcelas(remote)) throw new Error(AVISO_MUNDO);
  if (!fs.existsSync(local)) {
    console.log(`no está en local: ${local}`);
    return;
  }
  const stats = fs.statSync(local);
  if (stats.isDirectory()) {
    const hijos = fs.readdirSync(local);
    if (hijos.some((name) => name !== 'session.lock' && esMundoParcelas(`${remote}/${name}`))) {
      throw new Error(AVISO_MUNDO);
    }
    await mkdir(sftp, remote);
    for (const name of fs.readdirSync(local)) {
      if (name === 'session.lock') continue;
      await uploadPath(sftp, path.join(local, name), `${remote}/${name}`);
    }
    return;
  }
  await mkdir(sftp, path.posix.dirname(remote));
  await fastPut(sftp, local, remote);
  console.log(`subido ${remote}`);
}

const COPIA_LIGERA = [
  'server.properties',
  'plugins/Parcelas',
  'plugins/LuckPerms',
  'plugins/AuthMe',
  'plugins/Essentials',
  'plugins/Mundos',
  'plugins/GriefPrevention',
  'plugins/GriefPreventionData',
  'plugins/LibertyBans',
  'plugins/Xo',
];

const COPIA_MUNDO = ['world', ...COPIA_LIGERA];

function carpetaCopias() {
  return path.join(process.env.USERPROFILE || '', '.preguntones', 'copias');
}

function recortarCopias(base, cuantas = 4) {
  if (!fs.existsSync(base)) return;
  const dirs = fs.readdirSync(base, { withFileTypes: true })
    .filter((entrada) => entrada.isDirectory())
    .map((entrada) => entrada.name)
    .sort();
  while (dirs.length > cuantas) {
    const vieja = dirs.shift();
    fs.rmSync(path.join(base, vieja), { recursive: true, force: true });
    console.log(`Copia antigua borrada: ${vieja}`);
  }
}

async function pedirCopiaPanel(cfg, jar, body) {
  return panelFetch(cfg, jar, `/api/client/servers/${cfg.serverId}/backups`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

async function listarCopiasPanel(cfg, jar) {
  const { res, text } = await panelFetch(cfg, jar, `/api/client/servers/${cfg.serverId}/backups`);
  if (!res.ok) throw new Error(`No pude listar las copias (${res.status}): ${text.slice(0, 240)}`);
  const json = JSON.parse(text);
  return json.data || [];
}

async function borrarCopiaPanel(cfg, jar, uuid) {
  const { res, text } = await panelFetch(cfg, jar, `/api/client/servers/${cfg.serverId}/backups/${uuid}`, {
    method: 'DELETE',
  });
  if (!res.ok && res.status !== 204) {
    throw new Error(`No pude borrar la copia vieja (${res.status}): ${text.slice(0, 240)}`);
  }
}

async function crearCopiaPanel(cfg, jar) {
  const ignored = 'server.jar\nlibraries\nversions\nlogs\ncache\ncrash-reports';
  let intento = await pedirCopiaPanel(cfg, jar, { name: 'barrio', is_locked: false, ignored });
  if (intento.res.status === 400 || intento.res.status === 422) {
    intento = await pedirCopiaPanel(cfg, jar, { name: 'barrio', is_locked: false });
  }
  if ((intento.res.status === 400 || intento.res.status === 429) && /limit|too many|maximum|máximo/i.test(intento.text)) {
    const lista = await listarCopiasPanel(cfg, jar);
    const propias = lista
      .map((item) => item.attributes)
      .filter((item) => item && item.name === 'barrio' && !item.is_locked)
      .sort((a, b) => String(a.created_at).localeCompare(String(b.created_at)));
    if (propias.length === 0) {
      throw new Error(`El panel no admite otra copia: ${intento.text.slice(0, 240)}`);
    }
    await borrarCopiaPanel(cfg, jar, propias[0].uuid);
    intento = await pedirCopiaPanel(cfg, jar, { name: 'barrio', is_locked: false });
  }
  if (!intento.res.ok) {
    throw new Error(`El panel no creó la copia (${intento.res.status}): ${intento.text.slice(0, 240)}`);
  }
  const json = JSON.parse(intento.text);
  return json.attributes || json;
}

async function esperarCopiaPanel(cfg, jar, uuid) {
  const inicio = Date.now();
  while (Date.now() - inicio < 240000) {
    const { res, text } = await panelFetch(cfg, jar, `/api/client/servers/${cfg.serverId}/backups/${uuid}`);
    if (!res.ok) throw new Error(`No pude leer la copia (${res.status}): ${text.slice(0, 240)}`);
    const json = JSON.parse(text);
    const attrs = json.attributes || json;
    if (attrs.is_successful) return attrs;
    if (attrs.completed_at) throw new Error('La copia del panel terminó mal.');
    await sleep(2000);
  }
  throw new Error('La copia del panel no terminó a tiempo.');
}

async function descargarCopia(cfg, dest, rutas) {
  const { conn, sftp } = await connectSftp(cfg);
  try {
    for (const remote of rutas) {
      await downloadPath(sftp, remote, path.join(dest, remote));
    }
  } finally {
    conn.end();
  }
}

async function copia(cfg) {
  const base = carpetaCopias();
  const sello = new Date().toISOString().replace(/[:.]/g, '-');
  const dest = path.join(base, sello);
  fs.mkdirSync(dest, { recursive: true });
  const jar = await panelLogin(cfg);
  const state = await serverState(cfg, jar);
  console.log(`Servidor: ${state}`);
  if (state === 'running') {
    await sendCommand(cfg, jar, 'save-all');
    await sleep(2000);
    console.log('Mundo guardado antes de la copia.');
  }
  try {
    const panel = await esperarCopiaPanel(cfg, jar, (await crearCopiaPanel(cfg, jar)).uuid);
    fs.writeFileSync(path.join(dest, 'panel.json'), JSON.stringify({
      uuid: panel.uuid,
      nombre: panel.name || 'barrio',
      creado: panel.created_at || sello,
    }, null, 2));
    console.log(`Copia del panel: ${panel.uuid}`);
    try {
      await descargarCopia(cfg, dest, COPIA_LIGERA);
    } catch (err) {
      console.warn(`La copia del panel está hecha. No pude bajar los datos sueltos: ${err.message}`);
    }
  } catch (err) {
    console.warn(`El panel no guardó la copia (${err.message}). Bajo el barrio por SFTP.`);
    fs.writeFileSync(path.join(dest, 'panel.json'), JSON.stringify({
      error: String(err.message || err).slice(0, 400),
    }, null, 2));
    await descargarCopia(cfg, dest, COPIA_MUNDO);
    if (!fs.existsSync(path.join(dest, 'world'))) {
      throw new Error('No quedó copia del barrio ni en el panel ni en local.');
    }
  }
  recortarCopias(base, 4);
  console.log(`Copia local en ${dest}`);
}

function assertWorldClosed() {
  const lock = path.join(localRoot, 'world', 'session.lock');
  if (fs.existsSync(lock)) {
    throw new Error('El mundo está abierto en Minecraft local. Ciérralo y vuelve a sincronizar.');
  }
}

async function pull(cfg, paths) {
  assertWorldClosed();
  try {
    const jar = await panelLogin(cfg);
    const state = await serverState(cfg, jar);
    console.log(`Servidor: ${state}`);
    if (state === 'running') {
      await sendCommand(cfg, jar, 'save-all');
      await sleep(2000);
      console.log('Mundo guardado en el servidor.');
    }
  } catch (err) {
    console.warn(`Sigo con la descarga. No pude avisar al panel: ${err.message}`);
  }
  const { conn, sftp } = await connectSftp(cfg);
  try {
    fs.mkdirSync(localRoot, { recursive: true });
    for (const remote of paths) {
      await downloadPath(sftp, remote, path.join(localRoot, remote));
    }
  } finally {
    conn.end();
  }
  console.log(`Copia local en ${localRoot}`);
}

async function push(cfg, paths, reiniciar) {
  if (pushPisaElMundo(paths, localRoot)) throw new Error(AVISO_MUNDO);
  assertWorldClosed();
  await copia(cfg);
  const jar = await panelLogin(cfg);
  const before = await serverState(cfg, jar);
  console.log(`Servidor: ${before}`);
  let stopped = false;
  try {
    if (reiniciar && before !== 'offline') {
      if (before === 'running' || before === 'starting') {
        await sendCommand(cfg, jar, 'save-all');
        await sendPower(cfg, jar, 'stop');
      }
      await waitForState(cfg, jar, 'offline', 90000);
      stopped = true;
      console.log('Servidor parado para aplicar los cambios.');
    } else if (!reiniciar) {
      console.log('Sin reinicio. Los archivos quedan subidos y entran cuando pidas reiniciar.');
    }
    const { conn, sftp } = await connectSftp(cfg);
    try {
      for (const remote of paths) {
        await uploadPath(sftp, path.join(localRoot, remote), remote);
      }
    } finally {
      conn.end();
    }
  } finally {
    if (stopped || (reiniciar && before === 'offline')) {
      await sendPower(cfg, jar, 'start');
      console.log('Servidor arrancando otra vez.');
    }
  }
}

async function reiniciarServidor(cfg) {
  const jar = await panelLogin(cfg);
  const before = await serverState(cfg, jar);
  console.log(`Servidor: ${before}`);
  if (before === 'running' || before === 'starting') {
    await sendCommand(cfg, jar, 'save-all');
    await sleep(2000);
    await sendPower(cfg, jar, 'stop');
    await waitForState(cfg, jar, 'offline', 90000);
  }
  await sendPower(cfg, jar, 'start');
  console.log('Servidor arrancando otra vez.');
}

async function status(cfg) {
  const jar = await panelLogin(cfg);
  const state = await serverState(cfg, jar);
  const world = path.join(localRoot, 'world');
  console.log(`${cfg.name}`);
  console.log(`Panel: ${cfg.serverUrl}`);
  console.log(`Juego: ${cfg.gameHost}:${cfg.gamePort}`);
  console.log(`Estado: ${state}`);
  console.log(`Mundo local: ${fs.existsSync(world) ? world : 'todavía no descargado'}`);
}

const [action, ...args] = process.argv.slice(2);
const reiniciar = args.includes('--reiniciar');
const rest = args.filter((arg) => arg !== '--confirmar-mundo' && arg !== '--reiniciar');
const paths = rest.length > 0 ? rest : (action === 'push' ? DEFAULT_PATHS.filter((ruta) => ruta !== 'world') : DEFAULT_PATHS);

if (action === 'push' && pushPisaElMundo(paths, localRoot)) {
  console.error(AVISO_MUNDO);
  process.exit(1);
}

if (exigeComprobacion(action)) {
  const codigo = ejecutarComprobacion();
  if (codigo !== 0) {
    console.error('Hay fallos. No subo nada y no aplico cambios.');
    process.exit(codigo || 1);
  }
  if (action === 'push') {
    const instala = instalarPlugins(paths);
    if (instala !== 0) {
      console.error('Hay fallos. No subo nada y no aplico cambios.');
      process.exit(instala || 1);
    }
  }
}

try {
  const cfg = loadConfig();
  if (action === 'pull') await pull(cfg, paths);
  else if (action === 'push') {
    await push(cfg, paths, reiniciar);
  }
  else if (action === 'copia') await copia(cfg);
  else if (action === 'status') await status(cfg);
  else if (action === 'reiniciar') await reiniciarServidor(cfg);
  else {
    console.log('Uso: sync-servidor.ps1 pull | push [--reiniciar] | reiniciar | copia | status');
    console.log('push y reiniciar comprueban todo antes y se niegan si hay fallos.');
    process.exit(1);
  }
} catch (err) {
  console.error(err.message || err);
  process.exit(1);
}
