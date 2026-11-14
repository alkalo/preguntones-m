import { createRequire } from 'node:module'
import { prohibirEscribirMundoParcelas } from '../scripts/mundo-parcelas.mjs'
import { enObra, sueloPlaza, bloquePortal } from './portales-spawn.mjs'

const require = createRequire(import.meta.url)
const mc = require('C:/Users/Ale/Documents/preguntones-m/tools/node_modules/minecraft-data')('1.20.1')
const { Vec3 } = require('C:/Users/Ale/Documents/preguntones-m/tools/node_modules/vec3')
const { Anvil } = require('C:/Users/Ale/Documents/preguntones-m/tools/node_modules/prismarine-provider-anvil')

const id = (name) => {
  const block = mc.blocksByName[name]
  if (!block) throw new Error('bloque desconocido: ' + name)
  return block.defaultState
}
const AIRE = id('air')

const dir = process.argv[2]
if (!dir) throw new Error('falta la carpeta de regiones')
prohibirEscribirMundoParcelas(dir)
const anvil = new (Anvil('1.20.1'))(dir)
const pos = new Vec3(0, 0, 0)
let trozos = 0

for (let cx = -2; cx <= 1; cx++) {
  for (let cz = -2; cz <= 1; cz++) {
    const chunk = await anvil.load(cx, cz)
    if (!chunk) continue
    let cambio = false
    for (let lx = 0; lx < 16; lx++) {
      for (let lz = 0; lz < 16; lz++) {
        const wx = cx * 16 + lx
        const wz = cz * 16 + lz
        if (!enObra(wx, wz)) continue
        pos.x = lx
        pos.z = lz
        for (let y = 65; y <= 70; y++) {
          pos.y = y
          chunk.setBlockStateId(pos, AIRE)
        }
        pos.y = 64
        chunk.setBlockStateId(pos, id(sueloPlaza(wx, wz)))
        for (let y = 64; y <= 70; y++) {
          const nombre = bloquePortal(wx, wz, y)
          if (!nombre) continue
          pos.y = y
          chunk.setBlockStateId(pos, id(nombre))
        }
        cambio = true
      }
    }
    if (cambio) {
      await anvil.save(cx, cz, chunk)
      trozos++
    }
  }
}
console.log('arcos', trozos)
