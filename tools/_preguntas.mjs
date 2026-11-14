import fs from 'node:fs'
const s = fs.readFileSync('C:/Users/Ale/Documents/preguntones-m/servidor/world/datapacks/preguntones/data/preguntones/function/xo/banco.mcfunction', 'utf8')
const re = /\{s:"([^"]+)",v:(\d)b\}/g
const out = ['preguntas:']
let n = 0
let m
while ((m = re.exec(s))) {
  out.push('  - texto: "' + m[1].replaceAll('"', '') + '"')
  out.push('    verdad: ' + (m[2] === '1'))
  n++
}
fs.mkdirSync('C:/Users/Ale/Documents/preguntones-m/tools/xo', { recursive: true })
fs.writeFileSync('C:/Users/Ale/Documents/preguntones-m/tools/xo/preguntas.yml', out.join('\n') + '\n')
console.log('preguntas', n)
