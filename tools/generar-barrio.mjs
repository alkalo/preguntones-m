import fs from 'node:fs'
import { prohibirEscribirMundoParcelas } from '../scripts/mundo-parcelas.mjs'
import { bloquePortal } from './portales-spawn.mjs'
import path from 'node:path'
import zlib from 'node:zlib'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const root = path.resolve(import.meta.dirname, '..')
const mc = require(path.join(root, 'tools/node_modules/minecraft-data'))('1.20.1')
const registry = require(path.join(root, 'tools/node_modules/prismarine-registry'))('1.20.1')
const ChunkColumn = require(path.join(root, 'tools/node_modules/prismarine-chunk'))(registry)
const { Vec3 } = require(path.join(root, 'tools/node_modules/vec3'))
const Anvil = require(path.join(root, 'tools/node_modules/prismarine-provider-anvil')).Anvil('1.20.1')

const ROAD = 4
const SIZE = 16
const STRIDE = SIZE + ROAD
const FIRST = 25
const FROM = -18
const TO = 17
const PLAZA = 20
const GROUND = 64
const BEDROCK = 48
const EDGE = 390

const id = (name) => {
  const block = mc.blocksByName[name]
  if (!block) throw new Error('bloque desconocido: ' + name)
  return block.defaultState
}

const BEDROCK_ID = id('bedrock')
const STONE = id('stone')
const DIRT = id('dirt')
const GRASS = id('grass_block')
const BRICK = id('stone_bricks')
const FENCE = id('oak_fence')
const GOLD = id('gold_block')
const EMERALD = id('emerald_block')
const DARK = id('deepslate')
const LAMP = id('sea_lantern')
const FARMLAND = id('farmland')
const WHEAT = id('wheat')
const CHEST = id('chest')
const BELL = id('bell')
const LANTERN = id('lantern')
const PLAINS = mc.biomesByName.plains.id

// 17 de ancho. La fila de abajo es el punto cian, apoyado en el podio.
const MARK = [
  '......DDDDD......',
  '....DDGGGGGDD....',
  '...DGEEEEEEEGD...',
  '..DGEEEDDDDEEGD..',
  '..GEEED.....DEEG.',
  '..GEEED.....DEEG.',
  '...GEED.....DEEG.',
  '....GEDD...DDEG..',
  '.....GDDDDDDEG...',
  '......DGGGDD.....',
  '.......DEED......',
  '.......DEED......',
  '......DGGGD......',
  '.......DDD.......',
  '.................',
  '......CCCCC......',
  '.....CCCCCCC.....',
  '.....CCCCCCC.....',
  '......CCCCC......'
]
if (MARK.some((row) => row.length !== 17)) {
  throw new Error('la interrogación no mide 17: ' + MARK.map((row) => row.length).join(','))
}

const INK = { D: DARK, G: GOLD, E: EMERALD, C: LAMP }

function origen(indice) {
  if (indice >= 0) return FIRST + indice * STRIDE
  return -FIRST - SIZE + 1 - (-indice - 1) * STRIDE
}

function indiceEn(n) {
  for (let i = FROM; i <= TO; i++) {
    const o = origen(i)
    if (n >= o && n < o + SIZE) return i
  }
  return null
}

function eje(n) {
  if (Math.abs(n) <= PLAZA) return { tipo: 'plaza' }
  const idx = indiceEn(n)
  if (idx !== null) return { tipo: 'plot', i: idx }
  for (let i = FROM; i < TO; i++) {
    const fin = origen(i) + SIZE
    const sig = origen(i + 1)
    if (sig - fin === ROAD && n >= fin && n < sig) return { tipo: 'camino', a: i, b: i + 1 }
  }
  if (n > PLAZA && n < FIRST) return { tipo: 'anillo' }
  if (n < -PLAZA && n >= origen(-1) + SIZE) return { tipo: 'anillo' }
  return { tipo: 'fuera' }
}

function parcela(x, z) {
  const ex = eje(x)
  const ez = eje(z)
  if (ex.tipo !== 'plot' || ez.tipo !== 'plot') return null
  return { i: ex.i, j: ez.i, x0: origen(ex.i), z0: origen(ez.i) }
}

function esCamino(x, z) {
  const ex = eje(x)
  const ez = eje(z)
  if (ex.tipo === 'fuera' || ez.tipo === 'fuera') return false
  if (ex.tipo === 'plaza' && ez.tipo === 'plaza') return false
  if (ex.tipo === 'plot' && ez.tipo === 'plot') return false
  return ex.tipo === 'camino' || ex.tipo === 'anillo' || ez.tipo === 'camino' || ez.tipo === 'anillo' || ex.tipo === 'plaza' || ez.tipo === 'plaza'
}

function enCiudad(x, z) {
  return Math.abs(x) <= 360 && Math.abs(z) <= 360
}

function monumento(x, y, z) {
  if (z !== 0 && z !== -1) return 0
  const col = x + 8
  if (col < 0 || col >= 17) return 0
  const row = MARK.length - 1 - (y - 67)
  if (row < 0 || row >= MARK.length) return 0
  return INK[MARK[row][col]] || 0
}

const pos = new Vec3(0, 0, 0)

function put(chunk, x, y, z, block) {
  pos.x = x
  pos.y = y
  pos.z = z
  chunk.setBlockStateId(pos, block)
}

function superficie(wx, wz) {
  const plot = parcela(wx, wz)
  if (plot) {
    const lx = wx - plot.x0
    const lz = wz - plot.z0
    const borde = lx === 0 || lx === SIZE - 1 || lz === 0 || lz === SIZE - 1
    return { suelo: borde ? id('yellow_concrete') : GRASS, bajo: DIRT, valla: false }
  }
  if (esCamino(wx, wz)) return { suelo: id('dirt_path'), bajo: DIRT, valla: false }
  if (enCiudad(wx, wz)) return { suelo: BRICK, bajo: STONE, valla: false }
  return { suelo: GRASS, bajo: DIRT, valla: false }
}

const out = path.join(root, 'servidor', 'world-barrio')
prohibirEscribirMundoParcelas(out)
fs.rmSync(out, { recursive: true, force: true })
const regionDir = path.join(out, 'region')
fs.mkdirSync(regionDir, { recursive: true })
const anvil = new Anvil(regionDir)

const minChunk = Math.floor(-EDGE / 16)
const maxChunk = Math.floor(EDGE / 16)
let hechos = 0
const total = (maxChunk - minChunk + 1) ** 2

for (let cx = minChunk; cx <= maxChunk; cx++) {
  for (let cz = minChunk; cz <= maxChunk; cz++) {
    const chunk = new ChunkColumn()
    for (let lx = 0; lx < 16; lx++) {
      for (let lz = 0; lz < 16; lz++) {
        const wx = cx * 16 + lx
        const wz = cz * 16 + lz
        const capa = superficie(wx, wz)
        put(chunk, lx, BEDROCK, lz, BEDROCK_ID)
        for (let y = BEDROCK + 1; y <= GROUND - 2; y++) put(chunk, lx, y, lz, STONE)
        put(chunk, lx, GROUND - 1, lz, capa.bajo)
        put(chunk, lx, GROUND, lz, capa.suelo)
        pos.x = lx
        pos.y = GROUND
        pos.z = lz
        chunk.setBiome(pos, PLAINS)

        const radio2 = wx * wx + wz * wz
        if (radio2 <= 25) put(chunk, lx, GROUND + 1, lz, BRICK)
        if (radio2 <= 9) put(chunk, lx, GROUND + 2, lz, BRICK)
        const marca = (yy) => monumento(wx, yy, wz)
        for (let y = 67; y < 67 + MARK.length; y++) {
          const bloque = marca(y)
          if (bloque) put(chunk, lx, y, lz, bloque)
        }
        for (let y = 64; y <= 70; y++) {
          const nombre = bloquePortal(wx, wz, y)
          if (nombre) put(chunk, lx, y, lz, id(nombre))
        }
      }
    }
    await anvil.save(cx, cz, chunk)
    hechos++
    if (hechos % 200 === 0) console.log('trozos', hechos, '/', total)
  }
}

fs.cpSync(path.join(root, 'servidor', 'world', 'datapacks'), path.join(out, 'datapacks'), { recursive: true })
const icon = path.join(root, 'servidor', 'world', 'icon.png')
if (fs.existsSync(icon)) fs.copyFileSync(icon, path.join(out, 'icon.png'))

const levelSrc = path.join(root, 'servidor', 'world', 'level.dat')
const levelOut = path.join(out, 'level.dat')
const raw = zlib.gunzipSync(fs.readFileSync(levelSrc))
function findInt(name) {
  const needle = Buffer.from(name, 'utf8')
  let i = 0
  while ((i = raw.indexOf(needle, i)) !== -1) {
    if (i >= 2 && raw.readUInt16BE(i - 2) === needle.length) return i + needle.length
    i += 1
  }
  return -1
}
const spawn = raw.indexOf(Buffer.from('pos'))
const sx = findInt('SpawnX')
if (sx >= 0) {
  raw.writeInt32BE(0, sx)
  raw.writeInt32BE(65, findInt('SpawnY'))
  raw.writeInt32BE(12, findInt('SpawnZ'))
  console.log('spawn clasico')
} else {
  const posAt = findInt('pos')
  if (posAt < 0) throw new Error('no encuentro el spawn en level.dat')
  // TAG_Int_Array: name, then int count, then values. findInt points at the payload start,
  // which for an array is the count. The three ints follow if the count is 3.
  const count = raw.readInt32BE(posAt)
  if (count !== 3) throw new Error('pos inesperado ' + count)
  raw.writeInt32BE(0, posAt + 4)
  raw.writeInt32BE(65, posAt + 8)
  raw.writeInt32BE(12, posAt + 12)
  console.log('spawn moderno')
}
fs.writeFileSync(levelOut, zlib.gzipSync(raw))
console.log('barrio listo', hechos, 'trozos en', out)
