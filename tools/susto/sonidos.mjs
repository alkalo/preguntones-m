import { spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';

const require = createRequire(import.meta.url);
const ffmpeg = require('ffmpeg-static');
const rate = 44100;

const gritos = [
  { nombre: 'grito0', segundos: 1.4, onda: (t) => alarido(t, 480, 920, 0.55) },
  { nombre: 'grito1', segundos: 1.4, onda: (t) => alarido(t, 260, 640, 0.7) },
  { nombre: 'grito2', segundos: 1.4, onda: (t) => quebrado(t) },
  { nombre: 'grito3', segundos: 1.4, onda: (t) => salto(t) },
  { nombre: 'grito4', segundos: 1.4, onda: (t) => alarido(t, 620, 1100, 0.4) },
  { nombre: 'grito5', segundos: 1.4, onda: (t) => doble(t) }
];

const risas = [
  { nombre: 'risa0', segundos: 0.8, onda: (t) => carcajada(t, 5, 0.14, 380) },
  { nombre: 'risa1', segundos: 0.9, onda: (t) => carcajada(t, 4, 0.2, 220) },
  { nombre: 'risa2', segundos: 0.75, onda: (t) => carcajada(t, 4, 0.15, 460) }
];

const dir = process.argv[2];
if (!dir) {
  console.error('Falta la carpeta de salida');
  process.exit(1);
}
mkdirSync(dir, { recursive: true });

for (const pieza of [...gritos, ...risas]) {
  const muestras = render(pieza.segundos, pieza.onda);
  const wav = path.join(dir, `${pieza.nombre}.wav`);
  const ogg = path.join(dir, `${pieza.nombre}.ogg`);
  writeFileSync(wav, wavDe(muestras));
  const hecho = spawnSync(ffmpeg, ['-y', '-i', wav, '-c:a', 'libvorbis', '-q:a', '6', ogg], { stdio: 'pipe' });
  if (hecho.status !== 0) {
    console.error(hecho.stderr.toString());
    process.exit(hecho.status || 1);
  }
  console.log(`${pieza.nombre} rms ${rms(muestras).toFixed(3)} ticks ${Math.ceil(pieza.segundos * 20)}`);
}

function render(segundos, onda) {
  const n = Math.floor(rate * segundos);
  const muestras = new Float32Array(n);
  for (let i = 0; i < n; i++) muestras[i] = onda(i / rate, i);
  let media = 0;
  for (const muestra of muestras) media += muestra;
  media /= n;
  let pico = 0;
  for (let i = 0; i < n; i++) {
    const sucio = Math.tanh((muestras[i] - media) * 2.4);
    muestras[i] = sucio;
    pico = Math.max(pico, Math.abs(sucio));
  }
  const ganancia = 0.98 / (pico || 1);
  const ataque = Math.floor(rate * 0.008);
  const cola = Math.floor(rate * 0.02);
  for (let i = 0; i < n; i++) {
    let borde = 1;
    if (i < ataque) borde = i / ataque;
    else if (i > n - cola) borde = (n - i) / cola;
    muestras[i] *= ganancia * borde;
  }
  return muestras;
}

function alarido(t, desde, hasta, rasp) {
  const env = ataque(t, 0.012) * suelta(t, 1.15, 0.25);
  const subida = Math.min(1, t / 0.18);
  const f0 = desde + (hasta - desde) * subida + Math.sin(t * 47) * 28;
  return env * (8 * voz(t, f0, rasp) + (t < 0.03 ? ruido(t, 2) * 1.4 : 0));
}

function quebrado(t) {
  const golpes = [0.0, 0.22, 0.41, 0.63, 0.9];
  const tonos = [700, 540, 880, 460, 760];
  let suma = 0;
  for (let i = 0; i < golpes.length; i++) {
    const u = t - golpes[i];
    if (u < 0 || u > 0.2) continue;
    const env = Math.sin(Math.PI * u / 0.2);
    suma += env * voz(t, tonos[i] * (1 - u * 0.8), 0.65);
  }
  return suma * 6;
}

function salto(t) {
  if (t < 0.16) return ruido(t, 4) * 0.03;
  const u = t - 0.16;
  const env = ataque(u, 0.006) * suelta(u, 1.0, 0.22);
  const f0 = 980 - u * 520 + Math.sin(u * 60) * 40;
  return env * voz(t, f0, 0.5) * 9;
}

function doble(t) {
  const env = ataque(t, 0.01) * suelta(t, 1.12, 0.28);
  const f0 = 390 + t * 280 + Math.sin(t * 33) * 22;
  return env * (voz(t, f0, 0.45) + voz(t, f0 * 1.05, 0.35)) * 5;
}

function voz(t, f0, rasp) {
  let suma = 0;
  for (let h = 1; h <= 22; h++) {
    const f = f0 * h;
    if (f > 11000) break;
    const a = Math.exp(-((f - 800) ** 2) / (2 * 340 ** 2))
      + 0.9 * Math.exp(-((f - 1450) ** 2) / (2 * 480 ** 2))
      + 0.5 * Math.exp(-((f - 3000) ** 2) / (2 * 700 ** 2));
    suma += Math.sin(2 * Math.PI * f * t) * a / Math.sqrt(h);
  }
  const pulso = Math.max(0, Math.sin(2 * Math.PI * f0 * t));
  return suma * (0.25 + 0.75 * pulso ** 1.3) + ruido(t, 9) * rasp;
}

function carcajada(t, golpes, separacion, tono) {
  let suma = 0;
  for (let i = 0; i < golpes; i++) {
    const inicio = 0.04 + i * separacion;
    const u = t - inicio;
    const largo = separacion * 0.78;
    if (u < 0 || u > largo) continue;
    const env = Math.sin(Math.PI * Math.min(1, u / largo)) ** 0.7;
    const f = tono * (1 - i * 0.06) * (1 - u / largo * 0.4);
    const aire = u < 0.025 ? ruido(t, 8 + i) : 0;
    suma += env * (aire * 0.8 + Math.sin(f * t * 6.2) * 0.75 + ruido(t, 9) * 0.18);
  }
  return suma;
}

function ataque(t, segundos) {
  return Math.min(1, t / segundos);
}

function suelta(t, desde, largo) {
  if (t < desde) return 1;
  return Math.max(0, 1 - (t - desde) / largo);
}

function sierra(fase) {
  const ciclo = fase - Math.floor(fase);
  return ciclo * 2 - 1;
}

function ruido(t, sal) {
  const n = Math.sin((t * 48000 + sal * 17) * 12.9898) * 43758.5453;
  return (n - Math.floor(n)) * 2 - 1;
}

function rms(muestras) {
  let suma = 0;
  for (const muestra of muestras) suma += muestra * muestra;
  return Math.sqrt(suma / muestras.length);
}

function wavDe(muestras) {
  const datos = Buffer.alloc(44 + muestras.length * 2);
  datos.write('RIFF', 0);
  datos.writeUInt32LE(36 + muestras.length * 2, 4);
  datos.write('WAVE', 8);
  datos.write('fmt ', 12);
  datos.writeUInt32LE(16, 16);
  datos.writeUInt16LE(1, 20);
  datos.writeUInt16LE(1, 22);
  datos.writeUInt32LE(rate, 24);
  datos.writeUInt32LE(rate * 2, 28);
  datos.writeUInt16LE(2, 32);
  datos.writeUInt16LE(16, 34);
  datos.write('data', 36);
  datos.writeUInt32LE(muestras.length * 2, 40);
  for (let i = 0; i < muestras.length; i++) {
    const valor = Math.max(-1, Math.min(1, muestras[i]));
    datos.writeInt16LE(Math.round(valor * 32767), 44 + i * 2);
  }
  return datos;
}
