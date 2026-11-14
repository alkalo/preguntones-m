import { createRequire } from 'node:module'
import { bloquePortal } from './portales-spawn.mjs'
const require = createRequire(import.meta.url)
const { Vec3 } = require('C:/Users/Ale/Documents/preguntones-m/tools/node_modules/vec3')
const { Anvil } = require('C:/Users/Ale/Documents/preguntones-m/tools/node_modules/prismarine-provider-anvil')
const dir = process.argv[2]
const anvil = new (Anvil('1.20.1'))(dir)
const pos = new Vec3(0, 0, 0)
const ver = async (x, y, z) => {
  const chunk = await anvil.load(Math.floor(x / 16), Math.floor(z / 16))
  pos.x = ((x % 16) + 16) % 16
  pos.y = y
  pos.z = ((z % 16) + 16) % 16
  console.log(x, y, z, chunk ? chunk.getBlock(pos).name : 'sin-trozo', 'esperado', bloquePortal(x, z, y))
}
await ver(17, 65, 8)
await ver(17, 66, 8)
await ver(16, 68, 8)
await ver(0, 64, -14)
await ver(-4, 65, -14)
await ver(6, 67, -17)
await ver(0, 65, -22)
await ver(16, 65, 8)
