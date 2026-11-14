import { createRequire } from 'node:module'
import { prohibirEscribirMundoParcelas } from '../scripts/mundo-parcelas.mjs'
const require = createRequire(import.meta.url)
const { Vec3 } = require('C:/Users/Ale/Documents/preguntones-m/tools/node_modules/vec3')
const { Anvil } = require('C:/Users/Ale/Documents/preguntones-m/tools/node_modules/prismarine-provider-anvil')
const dir = process.env.TEMP + '/pt-arcos'
prohibirEscribirMundoParcelas(dir)
const anvil = new (Anvil('1.20.1'))(dir)
const chunk = await anvil.load(1, 0)
const pos = new Vec3(1, 66, 8)
chunk.setBlockStateId(pos, 5753)
await anvil.save(1, 0, chunk)
const otra = await anvil.load(1, 0)
console.log(otra.getBlock(pos).name)
