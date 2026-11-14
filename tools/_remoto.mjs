import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { Client } = require('ssh2');
const nbt = require('prismarine-nbt');

const cfg = JSON.parse(fs.readFileSync(path.join(process.env.USERPROFILE, '.preguntones', 'conexion.json'), 'utf8'));
const localLevel = 'C:/Users/Ale/Documents/preguntones-m/servidor/world/level.dat';
const raw = fs.readFileSync(localLevel);
const parsed = nbt.simplify(await nbt.parse(raw));
const data = parsed.Data || parsed;
console.log('local level name', data.LevelName || data.LevelName);
console.log('local keys', Object.keys(data).slice(0, 30).join(','));
console.log('spawn', data.SpawnX, data.SpawnY, data.SpawnZ, 'version', data.Version && data.Version.Name);

const conn = new Client();
await new Promise((resolve, reject) => {
  conn.on('ready', resolve);
  conn.on('error', reject);
  conn.connect({
    host: cfg.sftp.host,
    port: cfg.sftp.port,
    username: cfg.sftp.username,
    password: cfg.panelPassword,
    readyTimeout: 20000,
  });
});
const sftp = await new Promise((resolve, reject) => conn.sftp((err, s) => (err ? reject(err) : resolve(s))));
function readdir(remote) {
  return new Promise((resolve, reject) => sftp.readdir(remote, (err, list) => (err ? reject(err) : resolve(list))));
}
async function show(remote) {
  try {
    const list = await readdir(remote);
    const names = list
      .filter((i) => i.filename !== '.' && i.filename !== '..')
      .map((i) => `${i.filename}${i.longname.startsWith('d') ? '/' : ''}`)
      .slice(0, 40);
    console.log(remote, names.join(' | '));
  } catch (err) {
    console.log(remote, 'ERR', err.message);
  }
}
await show('world');
await show('world/dimensions');
await show('world/dimensions/minecraft/overworld');
await show('world/region');
await show('world/playerdata');
conn.end();
