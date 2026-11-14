import { createRequire } from 'node:module'
import path from 'node:path'
import { prohibirEscribirMundoParcelas } from '../scripts/mundo-parcelas.mjs'

const require = createRequire(import.meta.url)
const root = path.resolve(import.meta.dirname, '..')
const mc = require(path.join(root, 'tools/node_modules/minecraft-data'))('1.20.1')
const { Vec3 } = require(path.join(root, 'tools/node_modules/vec3'))
const { Anvil } = require(path.join(root, 'tools/node_modules/prismarine-provider-anvil'))

const STRIDE = 16
const SIZE = 16
const FIRST = 21
const FROM = -23
const TO = 22
const PLAZA = 20
const AMARILLO = mc.blocksByName.yellow_concrete.defaultState
const AIRE = mc.blocksByName.air.defaultState
const SUELO = new Set(['grass_block', 'coarse_dirt', 'dirt', 'stone_bricks', 'yellow_concrete'])

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

function parcela(x, z) {
  if (Math.abs(x) <= PLAZA && Math.abs(z) <= PLAZA) return null
  const i = indiceEn(x)
  const j = indiceEn(z)
  if (i === null || j === null) return null
  return { x0: origen(i), z0: origen(j) }
}

const dir = process.argv[2]
if (!dir) throw new Error('falta la carpeta de regiones')
prohibirEscribirMundoParcelas(dir)
const anvil = new (Anvil('1.20.1'))(dir)
const pos = new Vec3(0, 0, 0)
let tocados = 0
let bordes = 0
let carteles = 0

for (let cx = -24; cx <= 24; cx++) {
  for (let cz = -24; cz <= 24; cz++) {
    const chunk = await anvil.load(cx, cz)
    if (!chunk) continue
    let cambio = false
    for (let lx = 0; lx < 16; lx++) {
      for (let lz = 0; lz < 16; lz++) {
        const wx = cx * 16 + lx
        const wz = cz * 16 + lz
        const plot = parcela(wx, wz)
        if (!plot) continue
        const enBorde = wx === plot.x0 || wx === plot.x0 + SIZE - 1 || wz === plot.z0 || wz === plot.z0 + SIZE - 1
        pos.x = lx
        pos.z = lz
        if (enBorde) {
          pos.y = 64
          const suelo = chunk.getBlock(pos)
          if (suelo && SUELO.has(suelo.name) && suelo.name !== 'yellow_concrete') {
            chunk.setBlockStateId(pos, AMARILLO)
            bordes++
            cambio = true
          }
          pos.y = 65
          const valla = chunk.getBlock(pos)
          if (valla && (valla.name === 'oak_fence' || valla.name === 'oak_fence_gate')) {
            chunk.setBlockStateId(pos, AIRE)
            cambio = true
          }
        }
        if (wx === plot.x0 + 8 && wz === plot.z0) {
          pos.y = 65
          const cartel = chunk.getBlock(pos)
          if (cartel && cartel.name.includes('sign')) {
            chunk.setBlockStateId(pos, AIRE)
            carteles++
            cambio = true
          }
        }
      }
    }
    if (cambio) {
      await anvil.save(cx, cz, chunk)
      tocados++
    }
  }
}
console.log('trozos', tocados, 'bordes', bordes, 'carteles', carteles)
