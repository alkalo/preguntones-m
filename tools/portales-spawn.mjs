// Arco del este: único paso al survival. Arco del norte: la XO.
// El botón del survival lo pone Viajes encima del oro, en 17 66 8,
// fuera de la protección del spawn (el spawn está en 0, 12 y el radio es 16).

export function enObra(x, z) {
  return (x >= 12 && x <= 20 && z >= 4 && z <= 12)
    || (x >= -8 && x <= 8 && z >= -19 && z <= -9)
    || (x >= -3 && x <= 3 && z >= -24 && z <= -20)
}

export function sueloPlaza(x, z) {
  return Math.abs(x) <= 20 && Math.abs(z) <= 20 ? 'stone_bricks' : 'dirt_path'
}

export function bloquePortal(x, z, y) {
  const survival = arcoSurvival(x, z, y)
  if (survival) return survival
  return arcoXo(x, z, y)
}

function arcoSurvival(x, z, y) {
  const ix = x - 16
  const iz = z - 8
  if (Math.abs(ix) > 3 || Math.abs(iz) > 3) return null
  const pilar = Math.abs(ix) === 2 && Math.abs(iz) === 2
  if (y === 64) {
    if (Math.abs(ix) === 3 || Math.abs(iz) === 3) return 'deepslate_bricks'
    if (x === 15 && (z === 7 || z === 9)) return 'sea_lantern'
    if (x === 17 && (z === 7 || z === 9)) return 'sea_lantern'
    if (x === 16 || z === 8) return 'crying_obsidian'
    return 'amethyst_block'
  }
  if (pilar && y >= 65 && y <= 67) return 'crying_obsidian'
  if (pilar && y === 68) return 'gold_block'
  if (y === 65 && x === 17 && z === 8) return 'gold_block'
  if (y === 67 && x === 16 && z === 8) return 'sea_lantern'
  if (y === 68 && x >= 15 && x <= 18 && z >= 6 && z <= 10) return 'gold_block'
  return null
}

function arcoXo(x, z, y) {
  const ix = x
  const iz = z + 14
  if (Math.abs(ix) > 7 || Math.abs(iz) > 4) return null
  const pilarOeste = ix === -6 && Math.abs(iz) === 3
  const pilarEste = ix === 6 && Math.abs(iz) === 3
  if (y === 64) {
    if (Math.abs(ix) === 7 || Math.abs(iz) === 4) return 'deepslate_bricks'
    if (x === 0 && (z === -13 || z === -15)) return 'sea_lantern'
    if (ix <= -2) return 'red_concrete'
    if (ix >= 2) return 'lime_concrete'
    return 'quartz_block'
  }
  if ((pilarOeste || pilarEste) && y >= 65 && y <= 67) return pilarOeste ? 'red_concrete' : 'lime_concrete'
  if ((pilarOeste || pilarEste) && y === 68) return 'quartz_block'
  const ala = y === 65 && iz >= -3 && iz <= 2 && Math.abs(ix) >= 2 && Math.abs(ix) <= 5
  if (ala && ix < 0) return 'red_concrete'
  if (ala && ix > 0) return 'lime_concrete'
  const dintel = y === 68 && Math.abs(ix) <= 6 && (Math.abs(iz) === 3 || (Math.abs(ix) === 6 && Math.abs(iz) <= 3))
  if (dintel) return 'quartz_block'
  return null
}
