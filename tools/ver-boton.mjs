import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const registry = require('C:/Users/Ale/Documents/preguntones-m/tools/node_modules/prismarine-registry')('1.20.1')
const Block = require('C:/Users/Ale/Documents/preguntones-m/tools/node_modules/prismarine-block')(registry)
for (let id = 5748; id <= 5771; id++) {
  const bloque = Block.fromStateId(id, 0)
  console.log(id, JSON.stringify(bloque.getProperties()))
}
