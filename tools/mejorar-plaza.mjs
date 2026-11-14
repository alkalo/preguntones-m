import { createRequire } from 'node:module'
import path from 'node:path'
import { prohibirEscribirMundoParcelas } from '../scripts/mundo-parcelas.mjs'

const require = createRequire(import.meta.url)
const root = path.resolve(import.meta.dirname, '..')
const mc = require(path.join(root, 'tools/node_modules/minecraft-data'))('1.20.1')
const registry = require(path.join(root, 'tools/node_modules/prismarine-registry'))('1.20.1')
const ChunkColumn = require(path.join(root, 'tools/node_modules/prismarine-chunk'))(registry)
const { Vec3 } = require(path.join(root, 'tools/node_modules/vec3'))
const { Anvil } = require(path.join(root, 'tools/node_modules/prismarine-provider-anvil'))

const id = (name) => mc.blocksByName[name].defaultState
const BRICK = id('stone_bricks')
const STONE = id('stone')
const DIRT = id('dirt')
const BEDROCK = id('bedrock')
const GOLD = id('gold_block')
const EMERALD = id('emerald_block')
const DARK = id('deepslate')
const LAMP = id('sea_lantern')
const QUARTZ = id('quartz_block')
const SMOOTH = id('smooth_quartz')
const CRYING = id('crying_obsidian')
const AMETHYST = id('amethyst_block')
const LODESTONE = id('lodestone')
const RED = id('red_concrete')
const LIME = id('lime_concrete')
const WHITE = id('white_concrete')
const BLACK = id('black_concrete')
const LANTERN = id('lantern')
const FENCE = id('oak_fence')
const PLAINS = mc.biomesByName.plains.id

function trazo(x, y) {
  const d = Math.hypot(x, y - 5)
  const a = Math.atan2(5 - y, x)
  if (d > 3.1 && d < 6.4 && a > -0.5 && a < 2.7) return 1
  if (y >= 7 && y <= 10 && x >= -1 && x <= 7 && Math.abs(x - (10 - y) * 1.8) <= 1.5) return 1
  if (Math.abs(x) <= 1 && y >= 10 && y <= 14) return 1
  if (Math.abs(x) <= 2 && y >= 16 && y <= 18) return 2
  return 0
}

function colorTrazo(x, y) {
  const propio = trazo(x, y)
  if (propio === 2) return LAMP
  if (propio !== 1) return 0
  const borde = [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => trazo(x + dx, y + dy) !== 1)
  if (borde) return DARK
  const juntoAlBorde = [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => {
    const v = trazo(x + dx, y + dy)
    if (v !== 1) return false
    return [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([ex, ey]) => trazo(x + dx + ex, y + dy + ey) !== 1)
  })
  return juntoAlBorde ? GOLD : EMERALD
}

const pos = new Vec3(0, 0, 0)
function put(chunk, x, y, z, block) {
  pos.x = x
  pos.y = y
  pos.z = z
  chunk.setBlockStateId(pos, block)
}

function decorar(chunk, wx, wz, lx, lz) {
  const radio = Math.hypot(wx, wz)
  put(chunk, lx, 48, lz, BEDROCK)
  for (let y = 49; y <= 62; y++) put(chunk, lx, y, lz, STONE)
  put(chunk, lx, 63, lz, DIRT)
  put(chunk, lx, 64, lz, BRICK)

  if (wx === 0 && wz <= 28 && wz >= -16 && radio > 16) put(chunk, lx, 64, lz, SMOOTH)
  if (Math.abs(wx) <= 2 && wz === -18) put(chunk, lx, 64, lz, CRYING)
  if (wx === 0 && wz === -18) put(chunk, lx, 65, lz, LODESTONE)

  if (radio <= 16) put(chunk, lx, 65, lz, BRICK)
  if (radio <= 11) put(chunk, lx, 66, lz, BRICK)
  if (radio <= 7) put(chunk, lx, 67, lz, QUARTZ)
  if (radio <= 4) put(chunk, lx, 68, lz, SMOOTH)

  for (const dz of [-1, 0, 1]) {
    if (wz !== dz) continue
    for (let gy = 0; gy <= 19; gy++) {
      const bloque = colorTrazo(wx, gy)
      if (bloque) put(chunk, lx, 87 - gy, lz, bloque)
    }
  }

  if (wx >= 22 && wx <= 28 && wz >= 38 && wz <= 44) {
    const marco = wx === 22 || wx === 28 || wz === 38 || wz === 44
    put(chunk, lx, 64, lz, marco ? CRYING : AMETHYST)
    if (wx === 25 && wz === 41) put(chunk, lx, 65, lz, LODESTONE)
    if (marco && (wx + wz) % 2 === 0) put(chunk, lx, 65, lz, CRYING)
  }

  if (Math.abs(radio - 20) < 0.8 && Math.abs(wx) % 7 < 2 && Math.abs(wz) % 7 < 2) {
    put(chunk, lx, 65, lz, QUARTZ)
    put(chunk, lx, 66, lz, FENCE)
    put(chunk, lx, 67, lz, LANTERN)
  }

  pos.x = lx
  pos.y = 64
  pos.z = lz
  chunk.setBiome(pos, PLAINS)
}

const dir = process.argv[2]
if (!dir) throw new Error('falta la carpeta de regiones')
prohibirEscribirMundoParcelas(dir)
const anvil = new (Anvil('1.20.1'))(dir)
for (let cx = -3; cx <= 3; cx++) {
  for (let cz = -3; cz <= 3; cz++) {
    const chunk = new ChunkColumn()
    for (let lx = 0; lx < 16; lx++) {
      for (let lz = 0; lz < 16; lz++) {
        decorar(chunk, cx * 16 + lx, cz * 16 + lz, lx, lz)
      }
    }
    await anvil.save(cx, cz, chunk)
  }
}
const centro = await anvil.load(0, 0)
const lejos = await anvil.load(5, 5)
console.log('centro', centro.getBlock(new Vec3(0, 80, 0)).name, 'parcela', lejos ? lejos.getBlock(new Vec3(8, 64, 8)).name : 'ausente')
