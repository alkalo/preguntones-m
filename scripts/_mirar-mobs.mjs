import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { Client } = require('ssh2');
const cfg = JSON.parse(fs.readFileSync(path.join(process.env.USERPROFILE, '.preguntones', 'conexion.json'), 'utf8'));

function connect() {
  return new Promise((resolve, reject) => {
    const conn = new Client();
    conn.on('ready', () => conn.sftp((err, sftp) => (err ? reject(err) : resolve({ conn, sftp }))));
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

function readdir(sftp, remote) {
  return new Promise((resolve, reject) => sftp.readdir(remote, (err, list) => (err ? reject(err) : resolve(list))));
}

async function list(sftp, remote) {
  try {
    const items = await readdir(sftp, remote);
    const names = items
      .filter((item) => item.filename !== '.' && item.filename !== '..')
      .map((item) => `${item.filename}${item.longname.startsWith('d') ? '/' : ''} ${item.attrs.size}`);
    console.log(`\n== ${remote} (${names.length}) ==`);
    console.log(names.join('\n') || '(vacío)');
  } catch (err) {
    console.log(`\n== ${remote} == ${err.message}`);
  }
}

const { conn, sftp } = await connect();
try {
  await list(sftp, '.');
  await list(sftp, 'world/dimensions/minecraft/survival');
  await list(sftp, 'world/dimensions/minecraft/survival/entities');
  await list(sftp, 'world/dimensions/minecraft/survival/region');
  await list(sftp, 'world/dimensions/minecraft/overworld');
  await list(sftp, 'world/dimensions/minecraft/survival_nether');
} finally {
  conn.end();
}
