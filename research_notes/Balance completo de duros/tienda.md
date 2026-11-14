# Precios del puesto de duros

Fecha de la lectura: 13 de noviembre de 2026. Fuente de verdad: `Catalogo.java` y `Tienda.java` como están en el repo. No se ha editado código de juego. Los precios de abajo salen de esa fórmula, no de una lista a mano. `Catalogo.filas()` se ejecutó contra el `paper-api` empaquetado en `tools/parcelas/lib/paper-api.jar`, que se identifica como `1.21.11-R0.1-SNAPSHOT` en su `apiVersioning.json`. El servidor de juego se describe como Paper 26.1.2; esa jar es anterior. En la 1.21.11 los 893 nombres resolvieron y `saltados` quedó vacío.

La bandera (40) y la ficha de aldeano (40) están en el mismo menú y no usan esta fórmula. No entran en el recuento ni en la propuesta. Los euros de la web tampoco.

## ¿Cuál es la fórmula de precio en Catalogo.java?

### Takeaway

El jugador paga `entero × ESCALA`, con `ESCALA = 4`. El entero es el que está escrito en cada `add`, o el que devuelve `paso` en la madera y en la obra. `paso` sube al par siguiente, con suelo 2, antes de multiplicar por 4. En este archivo ese suelo no se usa: el clic más barato son 16 duros, no 8. Escaleras, losas y muros de obra cuestan los bloques de la receta; la madera reparte el precio del tronco con las mismas proporciones, y `paso` a veces las encarece un poco.

### Cited Findings

- El comentario de cabecera es la regla que el código dice seguir: «El precio sigue la receta y es el atajo caro: talar sigue saliendo mejor. 32 troncos de roble son 96 duros. Sus tablones salen a 24, porque un tronco da cuatro. La madera de corteza sale a 128, porque cuatro troncos dan tres. Escaleras, losas y muros cuestan los bloques que gastan. La bandera no está aquí: sigue a 40». — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 7-12.
- `ESCALA` es la constante 4. `add` hace `precio * ESCALA` y esa cifra es la que guarda la fila. `poner` llama a `add` si `Material.valueOf` existe; si el nombre no existe, lo anota en `SALTADOS` y no vende la fila. `Tienda` arranca mal si `SALTADOS` no está vacío. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 15 y 610-619; [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 479-495.
- `paso(valor) = max(2, techo(valor / 2) × 2)`. Redondea hacia arriba al par, con mínimo 2, y ese 2 es anterior a la escala (8 duros si llegara a usarse). — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 578-580.
- La madera recibe un entero `troncos`, que ya es el precio de 32 troncos antes de la escala. Roble, abeto, abedul, selva, acacia y roble oscuro entran con 24. Manglar y cerezo con 28. Roble pálido con 32. Bambú con 80, y el código se salta tronco y corteza cuando el prefijo es `BAMBOO`. Carmesí y distorsionado entran con 36 y venden tallo e hifas en vez de tronco y corteza. Pelado y sin pelar cuestan lo mismo. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 99-118 y 530-555.
- Fórmulas de la madera, todas antes de `× 4`. `tablones = paso(troncos / 4)` para 32 tablones. `corteza = paso(troncos × 4 / 3)` para 32 de madera con corteza. Escalera: 16 por `paso(troncos × 3 / 16)`. Losa: 64 por el mismo entero que los tablones. Valla: 16 por `paso(troncos × 5 / 24)`. Puerta de valla: 8 por `paso(troncos / 6)`. Puerta: 6 por `paso(troncos / 8)`. Trampilla: 8 por `paso(troncos / 4)`. Botón: 16 por `paso(troncos / 8)`. Placa y cartel: 8 por `paso(troncos / 6)`. Cartel colgante: 4 por `paso(troncos / 2)`. Estante: 4 por `paso(troncos / 4)`. — mismas líneas 531-555.
- La obra vende el bloque a `bloques` unidades por el entero `precio`, y deriva el resto: escalera 16 por `paso(precio × 24 / bloques)`, losa 32 por `paso(precio × 16 / bloques)`, muro 16 por el mismo entero que la losa. El muro solo se crea si el argumento no es nulo. El 24 y el 16 son los bloques que gasta la receta de 16 escaleras (6 bloques dan 4 escaleras) y de 32 losas o 16 muros (3 bloques dan 6 losas; 6 bloques dan 6 muros). — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 565-576.
- Los colores no tienen `paso`. Dieciséis nombres, del blanco al negro, entran con la misma cantidad y el mismo entero. La lana, la alfombra, la terracota teñida, el azulejo, el polvo, el hormigón, la cama, el estandarte y el tinte son una llamada cada uno. Las velas de color y los cristales teñidos usan la misma función. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 78, 177-185, 192-193 y 582-598.
- El clic cobra el precio de la fila y entrega la cantidad, recortada al máximo del material si el montón es más corto. El precio no se prorratea. En esta pasada no se pudo llamar a `getMaxStackSize` porque el registro de Paper no arranca fuera del servidor. Ninguna cantidad escrita pasa de 64. Los carteles se venden de 8, los colgantes de 4 y los estandartes de 1; en Java el montón de cartel y de estandarte es 16, y el del cartel colgante también es 16. — [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 381-399 y 523-533; [Hanging Sign](https://minecraft.wiki/w/Hanging_Sign).
- La bandera no está en el catálogo. El botón del menú y `/bandera` cobran 40 fijos. La ficha de aldeano también es 40, constante aparte. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 12-13; [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 82 y 417-444; [Aldeanos.java](tools/parcelas/src/com/preguntones/parcelas/Aldeanos.java) línea 42.

Ejemplo barato, la puerta de roble. El tronco entra con 24. `paso(24 / 8) = paso(3) = 4`, y `4 × 4 = 16` duros por 6 puertas. — líneas 100, 549, 578-580 y 610-611.

La receta de puerta de madera es 6 tablones = 3 puertas, así que 6 puertas son 12 tablones. Esos 12 tablones, al precio de la propia tienda, son `12 / 32 × 24 = 9` duros. El clic de 16 queda 1,78 veces por encima. El salto lo hace `paso`: el valor crudo es 3 y el par siguiente es 4. En el roble, la fórmula `troncos / 8` y la receta real (`troncos × 3 / 32 = 2,25`) caen las dos en ese mismo 4.

Ejemplo de en medio, el roble del comentario. 32 troncos: entero 24 × 4 = 96 duros, 3 duros el tronco. 32 tablones: `paso(24 / 4) = paso(6) = 6`, × 4 = 24 duros. 32 maderas con corteza: `paso(24 × 4 / 3) = paso(32) = 32`, × 4 = 128 duros. Cuadra con las líneas 8-10. 16 escaleras: `paso(24 × 3 / 16) = paso(4,5) = 6`, × 4 = 24 duros. Antes de `paso` serían 4,5 × 4 = 18 duros, que es lo que cuestan los 24 tablones de la receta (6 tablones = 4 escaleras). `paso` añade un tercio. 64 losas salen a los mismos 24 duros que 32 tablones: un tablón da dos losas, así que el montón de 64 es el rendimiento exacto.

Ejemplo caro, la escalera de cuarzo liso. La obra entra con 16 bloques y entero 36. El bloque son `36 × 4 = 144` duros. La escalera es `paso(36 × 24 / 16) = paso(54) = 54`, × 4 = 216 duros por 16 escaleras. 54 ya es par, así que `paso` no añade nada: 16 escaleras cuestan exactamente 24 bloques, y 24 bloques de un montón de 16 son `24 / 16 × 144 = 216`. El mismo 216 sale en la escalera de púrpur, en la de ladrillos de prismarina y en la de prismarina oscura. Es el clic más caro del catálogo. — líneas 337, 343, 376-377, 565-568 y 610-611.

### Inferences

- Casi toda la tienda es un entero par, escrito a mano o fabricado por `paso`, multiplicado por 4. Por eso no hay precios impares. El abanico real va de 16 en 16 pasos pares hasta 216: hay 21 precios distintos y todos son múltiplos de 4.
- Donde la receta cae en un par exacto (losas de madera, piedra lisa, arenisca, escalera de cuarzo), el atajo no es más caro que comprar los bloques y craftear. El «atajo caro» lo pone `paso` cuando el crudo no es par, y lo pone la escala cuando se compara con picar o talar, no cuando se compara con otra fila de la misma tienda.
- La puerta de valla usa `troncos / 6`. La receta son 2 tablones y 4 palos, y 4 palos son otros 2 tablones: 4 tablones por puerta, o sea `troncos / 4` para 8 puertas. Esa fila es la excepción de la madera: sale más barata que la madera que lleva.

### Gaps

- No se arrancó Paper 26.1.2. La lista de 893 es la de la API 1.21.11 contra la que compila el plugin. Un nombre que esa API ya tiene también lo tiene un servidor más nuevo, salvo que 26.1 hubiera borrado un material. No hay señal de eso en el fuente.
- `isItem()` y `getMaxStackSize()` no se ejecutaron: el registro de ítems de Paper exige el servidor. Si algún material de los 893 no fuera ítem, `Tienda.add` lo tiraría en silencio y el menú tendría menos filas. No hay en la lista aire, agua ni variantes de muro técnico.
- La receta del estante (`_SHELF`) y la de la puerta de cobre no se han contrastado con la wiki en esta pasada. No entran en los ejemplos ni en la propuesta.

## ¿Cuántas filas hay por categoría, cuál es el mínimo y el máximo, y cuáles son los extremos?

### Takeaway

El catálogo que devuelve `filas()` tiene 893 filas, ninguna repetida y ninguna saltada por nombre desconocido. El clic más barato son 16 duros (38 filas). El más caro son 216 (4 filas). Por pieza, lo más barato es la losa de las maderas comunes, a 0,375 duros, y lo más caro es la cabeza de dragón, a 192. Los extremos de verdad no son esos topes: son las filas que cuestan menos que craftearlas con otras filas de la misma tienda, y las que cuestan varias veces sus materiales.

### Cited Findings

- Recuento al ejecutar `filas()`, en el orden del menú. Páginas con la fórmula de `Tienda`: `(cosas + 44) / 45`, división entera. — [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 340 y 516; [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 21-36 y 52-68.

| Sección | Filas | Mínimo del clic | Máximo del clic | Páginas | La pieza más barata | La pieza más cara |
| --- | ---: | ---: | ---: | ---: | --- | --- |
| Luces | 41 | 24 | 96 | 1 | 0,75 (antorcha) | 48 (luz de rana, 2 por 96) |
| Madera | 195 | 16 | 192 | 5 | 0,375 (losa de roble y las otras cinco de base 24) | 40 (cartel colgante de bambú, 4 por 160) |
| Piedra | 120 | 16 | 168 | 3 | 1 (botón de piedra) | 20 (pizarra reforzada, 8 por 144) |
| Color | 145 | 32 | 96 | 4 | 2 (tinte) | 80 (cama) |
| Cristal | 35 | 32 | 112 | 1 | 1 (panel) | 7 (cristal opaco) |
| Plantas | 76 | 24 | 96 | 2 | 0,5 (bambú, 64 por 32) | 96 (espora y corazón del creaking) |
| Tierra | 21 | 24 | 192 | 1 | 0,75 (camino de tierra) | 48 (hielo azul) |
| Nether | 49 | 24 | 216 | 2 | 1 (piedra del Nether, 32 por 32) | 13,5 (escalera de cuarzo liso) |
| End | 10 | 72 | 216 | 1 | 2,25 (losa y muro de ladrillos del End) | 24 (flor de chorus, 4 por 96) |
| Cobre | 49 | 48 | 144 | 2 | 3 (losa de cobre encerado) | 80 (estatua) |
| Mar | 35 | 24 | 216 | 1 | 1,5 (alga y hierba marina) | 48 (esponja, 2 por 96) |
| Muebles | 94 | 16 | 192 | 3 | 1 (escalera de mano) | 192 (cabeza de dragón) |
| Mecanismos | 23 | 24 | 96 | 1 | 1,5 (antorcha de redstone y palanca) | 80 (tolva y vagoneta) |

Suma de filas: 893. Suma de páginas: 27.

- A 16 duros hay 38 filas. Treinta son de las seis maderas de base 24: puerta de valla (8), puerta (6), botón (16), placa (8) y cartel (8), por roble, abeto, abedul, selva, acacia y roble oscuro. Seis más son la puerta y el botón de manglar, cerezo y roble pálido. Las otras dos son el botón de piedra (16) y la mesa de trabajo (1). — líneas 100-110, 172, 415, 548-553.
- A 216 duros hay exactamente cuatro filas, todas escaleras de 16: cuarzo liso, púrpur, ladrillos de prismarina y prismarina oscura. A 192 hay ocho: las cuatro hifas (carmesí y distorsionado, peladas y sin pelar, 32), la escalera de cuarzo (16), la escalera de prismarina (16), el hielo azul (4) y la cabeza de dragón (1). — líneas 117-118, 304, 333, 337, 343, 375-377, 434.
- El precio por pieza más bajo del catálogo es 0,375 duros: losa de roble, abeto, abedul, selva, acacia y roble oscuro, 64 por 24. El más alto es 192: una cabeza de dragón. Por debajo de 1 duro la pieza están también el bambú suelto (64 por 32, a 0,5), las losas de manglar, cerezo y roble pálido (0,5), las de hongo (0,625), los tablones de roble y familia (0,75), las antorchas (0,75) y el camino de tierra (0,75).
- Comparación con la receta, usando el precio de las otras filas como coste de los materiales. Ratio por debajo de 1: comprar la pieza sale más barato que comprar lo que la receta gasta. Ratio por encima de 2: la pieza cuesta más del doble que sus materiales en la misma tienda.

| Pieza que vende la tienda | Pagas | Los materiales, a precio de tienda | Ratio |
| --- | ---: | ---: | ---: |
| 8 puertas de valla de roble (y las otras cinco de base 24) | 16 | 32 tablones = 24 | 0,67 |
| 8 puertas de valla de carmesí o distorsionado | 24 | 32 tablones = 40 | 0,60 |
| 8 puertas de valla de bambú | 56 | 32 tablones = 80 | 0,70 |
| 16 botones de piedra | 16 | 16 de piedra = 32 | 0,50 |
| 32 caminos de tierra | 24 | 32 de tierra = 32 | 0,75 |
| 32 alfombras | 80 | unos 21,33 de lana = 85,33 | 0,94 |
| 16 de hielo compacto | 80 | 144 de hielo = 432 | 0,19 |
| 4 de hielo azul, contra el hielo | 192 | 324 de hielo = 972 | 0,20 |
| 4 de hielo azul, contra el hielo compacto de la tienda | 192 | 36 de compacto = 180 | 1,07 |
| 32 de roca musgosa | 80 | 32 de roca + 32 de enredadera = 160 | 0,50 |
| 32 de ladrillos musgosos | 96 | 32 de ladrillos de piedra + 32 de enredadera = 176 | 0,55 |
| 4 soltadores | 48 | 28 de roca + 4 de redstone = 62 | 0,77 |
| 4 pistones | 80 | tablones, roca, hierro al precio de los barrotes, y redstone = 111 | 0,72 |
| 16 raíles | 80 | 6 lingotes al precio de los barrotes, más un palo = 96,4 | 0,83 |
| 32 de tierra estéril | 32 | 16 de tierra + 16 de grava = 40 | 0,80 |
| 16 tintes verdes | 32 | 16 de cactus = 80 | 0,40 |
| 1 mesa de trabajo | 16 | 4 tablones de roble = 3 | 5,33 |
| 1 cama | 80 | 3 lanas + 3 tablones de roble = 14,25 | 5,61 |
| 4 carteles colgantes de bambú | 160 | 4 bloques de bambú pelado + 1,33 cadenas = 46,67 | 3,43 |
| 4 carteles colgantes de roble | 48 | 4 troncos pelados + 1,33 cadenas = 38,67 | 1,24 |

- Recetas usadas en esa tabla, de Java. Puerta de valla: tablones y palos; 2 tablones + 4 palos = 1 puerta, y 4 palos salen de 2 tablones. Hielo compacto: se craftea con 9 de hielo desde la 1.13. Hielo azul: «hace falta 81 de hielo normal, pasado por hielo compacto, para un solo bloque». Cartel colgante: 2 cadenas y 6 troncos pelados (o bloque de bambú pelado, o tallo pelado) dan 6 carteles. Cama: lana del mismo color y tablones. Camino de tierra: la pala sobre tierra, no gasta un segundo bloque. — [Fence Gate](https://minecraft.wiki/w/Fence_Gate); [Packed Ice](https://minecraft.wiki/w/Packed_Ice); [Blue Ice](https://minecraft.wiki/w/Blue_Ice); [Hanging Sign](https://minecraft.wiki/w/Hanging_Sign); [artículo oficial del cartel colgante](https://www.minecraft.net/en-us/article/hanging-sign); [Bed](https://minecraft.wiki/w/Bed).
- Lo que sí sigue la receta, para no tratar todo el catálogo de desajuste. 32 tablones de roble = 8 troncos, ratio 1. 64 losas de roble = 32 tablones, ratio 1. 16 escaleras de piedra = 24 de piedra, ratio 1. 16 de arenisca = 64 de arena, ratio 1. 16 escaleras de cuarzo liso = 24 bloques, ratio 1. 16 escaleras de cobre cortado encerado = 24 de cobre cortado, ratio 1. El horno (32) es el doble de 8 rocas (16). El estandarte (48) es 1,97 veces 6 lanas y un palo. La vela de color (8 por 48) es exactamente 8 velas sin teñir (32) más 8 tintes (16). — líneas 71-78, 122, 153, 333, 361, 418; [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java).
- Unos cuantos crafteos caros, sin ser el caso de la mesa o la cama. El comparador (2 por 80) sale a 3,2 veces su piedra, su redstone y su cuarzo. El observador (2 por 96) a 2,82 veces. El sensor de luz (48) a 3,66 veces tres cristales, tres cuarzos y tres losas. El azulejo (16 por 96) a 2,4 veces la terracota teñida que se hornea. El ladrillo (16 por 80) a 3,33 veces el bloque de arcilla, que es el bloque del que salen los cuatro ladrillos. Con la escala actual eso son decenas de duros; el ratio no lo arregla bajar `ESCALA`, porque los materiales bajan igual.

### Inferences

- El mínimo de 16 y el máximo de 216 describen el clic, no el bloque. La losa de roble es el bloque más barato del servidor y la escalera de prismarina es el clic más caro, pero un bloque de esa escalera sale a 13,5 duros y la cabeza de dragón, que no es el clic más caro, sale a 192.
- El hielo azul está bien de precio respecto del hielo compacto de la tienda (un 7 % por encima) y muy por debajo del hielo del que salen los dos. Quien compre hielo para craftear tira el dinero. La fila que sobra, si las tres siguen a la venta, es la del hielo suelto: nadie la usa para llegar al azul.
- La puerta de valla, el botón de piedra, el camino, la roca musgosa y el hielo compacto no son un redondeo. Salen a la mitad o menos de lo que la tienda cobra por sus piezas. Ahí craftear con material comprado no compensa, que es lo contrario del comentario de cabecera.
- La mesa y la cama son el otro extremo: cinco veces sus materiales. Una cama de 80 duros es más cara que 32 troncos de roble (96 queda cerca) y se craftea con tres lanas y tres tablones.
- El bambú parece un tronco de lujo (entero 80) y no lo es. 32 tablones de bambú cuestan 80 y 32 de roble cuestan 24. Por dentro, la cadena es coherente con el rendimiento pobre de Java: el ítem de bambú es barato (64 por 32), el bloque sale cerca de 9 ítems, y el tablón sale cerca de medio bloque. El desajuste gordo del bambú es el cartel colgante, porque `troncos / 2` trata el 80 como si fuera el precio de un tronco.

### Gaps

- No se ha abierto la receta del estante ni la de la puerta de cobre. El estante de roble se vende 4 por 24, el mismo entero que la trampilla; sin la receta no se sabe si eso es prima o descuento.
- El tinte verde es el único tinte contrastado contra un bloque que no es una flor. El tinte rojo contra la amapola sale a 32 frente a 24, una prima pequeña. No se ha repetido el ejercicio con cacao, lapislázuli o saco de tinta, que la tienda no vende.
- La pizarra reforzada se vende 8 por 144. No se ha comprobado en la wiki, en esta pasada, la regla actual de si se dropea en survival. El precio está; el juicio de «bloque que no se puede conseguir» no.

## ¿El comentario del código se cumple: talar sigue saliendo mejor?

### Takeaway

Sí. Comprar madera o piedra cuesta ocho veces lo que la misión paga por talar o picar esa misma cantidad, y además la misión te deja los bloques. El comentario se cumple de sobra. Lo que no se cumple es que el puesto sea un atajo usable: un montón de troncos cuesta más que toda la misión de madera, y la casa de piedra de más abajo cuesta más que todas las misiones del código juntas.

### Cited Findings

- La misión `madera` paga 6 duros cada vez que el contador de troncos sube 16, y paga como mucho 3 veces. Cuenta roble, abeto, abedul, selva, acacia, roble oscuro, manglar, cerezo, tallo carmesí y tallo distorsionado. No cuenta el roble pálido ni el bambú. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 740 y 780-784.
- 16 troncos de roble en el puesto son la mitad de 96, o sea 48 duros. 48 / 6 = 8. 32 troncos son 96 duros. Talando esos 32 se cobran dos veces la misión, 12 duros, y los troncos se quedan. Comprarlos cuesta 96 y no suma el contador de `MINE_BLOCK`, así que no paga la misión. La diferencia a favor de talar, valorando el tronco al precio del puesto, es 96 + 12 = 108 duros.
- El tope de la misión de madera es 18 duros (6 × 3) por 48 troncos. Un solo clic de 32 troncos (96) es más que ese tope entero.
- La misión `piedra` paga 8 duros por 32 bloques, hasta 4 veces. Cuenta piedra, roca, pizarra y pizarra rocosa. Las cuatro filas del puesto son 32 por 64 duros. 64 / 8 = 8, el mismo factor que la madera. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 741 y 787-788; [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 122-123 y 137-138.
- Tope de ingresos de las misiones repetibles, sumando `duros × veces` del array: madera 18, piedra 32, caza 30, comida 12, pesca 16, camino 24, cama 8, aldeano 10, hierro 24, diamante 32, encantar 12, blaze 28, escombro 20, enderman 20. Total 286. Una vez: Nether 16 y End 18. El dragón, 20 a quien esté del grupo. Total con eso: 340 duros. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 483, 729-733 y 738-754.
- Al entrar, el saldo es 25 duros y el máximo de la cuenta es 2000. 96 duros de troncos son 3,8 veces el saldo inicial. — [Essentials/config.yml](servidor/plugins/Essentials/config.yml) líneas 9-11.
- El hierro no se vende en mena. Los barrotes (16 por 96) y la puerta de hierro (1 por 96) cuestan lo mismo, y las dos recetas gastan 6 lingotes, así que la tienda trata el lingote a 16 duros. La misión paga 12 por 8 menas. Seis lingotes de tienda son 96 duros frente a 9 duros de paga por seis menas: unas diez veces, no ocho. Es otra base, no la escala de la madera. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 420-422; [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) línea 748.

### Inferences

- «Talar sigue saliendo mejor» es cierto para cualquier fila cuyo precio sea positivo, porque talar no gasta duros. La lectura útil del comentario es la prima: cuántos duros pide el atajo respecto de lo que el juego ya paga por hacer el trabajo. Hoy esa prima es ×8 en la madera común y en la piedra contada por la misión. Con duros escasos (25 al entrar, 340 si se cobran todas las misiones una vez hasta su tope), ×8 no es una prima de conveniencia. Es un precio al que no se construye.
- Bajar la escala a 1 dejaría 32 troncos en 24 duros y 32 de roca en 16. Eso es el doble de lo que la misión paga por el mismo trabajo (12 y 8). Talar seguiría siendo mejor: te pagan y no gastas. Comprar pasaría a ser un atajo de una o dos misiones, no de toda la lista.
- El ×8 no arregla las filas que están por debajo de su receta. Esas siguen siendo un descuento aunque se divida todo entre 4. Hacen falta enteros distintos, no solo otra `ESCALA`.

### Gaps

- No hay en el código un tiempo de tala ni un «duro por minuto». El ×8 sale de la misión, que es el único precio en duros que el juego pone al hecho de talar. No mide si 16 troncos llevan un minuto o cinco.
- El saldo vivo de los jugadores no se ha leído. 25 es el saldo inicial de la config, no lo que la gente tiene después de una semana.

## ¿Cuántos duros cuesta una construcción representativa?

### Takeaway

Con la fórmula de ahora, la cabaña de roble definida abajo se paga a 240 duros, la casa de piedra a 600 y un juego de una fila decorativa de cada a 472. El puesto no vende medias filas: 240, 600 y 472 son los clics, no el prorrateo de los bloques que se colocan. El cofre no está en el catálogo.

### Cited Findings

- Cada clic entrega la cantidad de la fila y cobra el precio entero. Los totales de abajo cuentan los clics mínimos que cubren la lista. El «prorrateo» es `precio × bloques / cantidad`, y solo sirve para ver cuánto duros sobra en el montón que no se coloca. — [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 146-156 y 380-386.

Cabaña de roble. Huella exterior 5×5, paredes de 3 de alto, techo plano de tablones. Columnas de pared: `5 × 4 − 4 = 16`. Paredes: `16 × 3 − 2` de la puerta = 46 tablones. Suelo 25, techo 25. Tablones colocados: 96. Más 1 puerta de roble, 4 antorchas, 1 mesa, 1 horno y 1 cama blanca.

| Fila | Hacen falta | Clics | Se paga | Sobra en el montón | Prorrateo |
| --- | ---: | ---: | ---: | ---: | ---: |
| Tablones de roble, 32 por 24 | 96 | 3 | 72 | 0 | 72 |
| Puerta de roble, 6 por 16 | 1 | 1 | 16 | 5 | 2,67 |
| Antorchas, 32 por 24 | 4 | 1 | 24 | 28 | 3 |
| Mesa, 1 por 16 | 1 | 1 | 16 | 0 | 16 |
| Horno, 1 por 32 | 1 | 1 | 32 | 0 | 32 |
| Cama blanca, 1 por 80 | 1 | 1 | 80 | 0 | 80 |
| Total | | 8 | 240 | | 205,67 |

Esos 240 son el saldo inicial (25) casi diez veces, y 100 duros menos que el tope de todas las misiones (340). La cama sola son 80, un tercio del total, por un objeto de tres lanas y tres tablones.

Casa de piedra. Huella 8×8, paredes de 4 de alto, techo plano de losas de ladrillo de piedra. Suelo: 64 de roca. Columnas: `8 × 4 − 4 = 28`. Paredes: `28 × 4 = 112`, menos 2 de la puerta y menos 6 paneles = 104 ladrillos de piedra. Techo: 64 losas. Más 6 paneles, 1 puerta de roble y 8 antorchas.

| Fila | Hacen falta | Clics | Se paga | Prorrateo |
| --- | ---: | ---: | ---: | ---: |
| Roca, 32 por 64 | 64 | 2 | 128 | 128 |
| Ladrillos de piedra, 32 por 80 | 104 | 4 | 320 | 260 |
| Losa de ladrillo, 32 por 40 | 64 | 2 | 80 | 80 |
| Panel de cristal, 32 por 32 | 6 | 1 | 32 | 6 |
| Puerta de roble, 6 por 16 | 1 | 1 | 16 | 2,67 |
| Antorchas, 32 por 24 | 8 | 1 | 24 | 6 |
| Total | | 11 | 600 | 482,67 |

600 duros es más que los 340 de todas las misiones, el Nether, el End y el dragón. La losa sale de `paso(20 × 16 / 32) = 10`, × 4 = 40 por 32. El ladrillo es el entero 20 × 4 = 80 por 32. — líneas 125 y 565-572.

Pila decorativa: una fila de cada, que es lo que el puesto vende de una vez. No es una casa; es el montón con el que se viste una fachada.

| Fila | Clics | Se paga |
| --- | ---: | ---: |
| Hormigón blanco, 32 por 96 | 1 | 96 |
| Azulejo blanco, 16 por 96 | 1 | 96 |
| Vela blanca, 8 por 48 | 1 | 48 |
| Faroles, 4 por 64 | 1 | 64 |
| Amapolas, 16 por 24 | 1 | 24 |
| Cristal blanco, 16 por 64 | 1 | 64 |
| Alfombra blanca, 32 por 80 | 1 | 80 |
| Total | 7 | 472 |

Esas siete filas salen a 472 duros, casi el doble de la cabaña de madera y por debajo de la casa de piedra. El azulejo y el hormigón, los dos a 96, son cada uno más caros que 32 troncos.

Para comparar un montón de 64 bloques justos, sin geometría: 64 tablones de roble salen a 48 duros (dos clics de 24), 64 de roca a 128, 64 de hormigón blanco a 192 y 64 de azulejo blanco a 384. El azulejo, que es terracota horneada, cuesta ocho veces el tablón.

### Inferences

- El número que sale de la cuenta del jugador es el de los clics. En la cabaña se tiran 34 duros en puertas y antorchas de más (240 − 205,67). En la casa de piedra se tiran unos 117, casi todos en los 24 ladrillos que sobran del cuarto montón (320 − 260).
- Ninguna de las tres cabe en el saldo inicial de 25. La cabaña no cabe ni en el tope de la misión de madera (18) ni en el de la de piedra (32). Hace falta el resto de las misiones, o no se compra.
- 600 > 340: la casa de piedra de 8×8, con el catálogo actual, no se puede pagar aunque se cobren todas las misiones del código hasta su tope y el dragón.

### Gaps

- No hay un cofre, un cubo ni un cristal sin teñir en la cabaña. El cofre no está en `Catalogo.java`. Quien quiera un cofre lo craftea con ocho tablones, que a precio de tienda son 6 duros, frente a una mesa que se vende a 16 por cuatro tablones.
- La geometría es un ejemplo definido para poder multiplicar la fórmula. No es una casa que esté en el mundo ni en otro documento.

## ¿Qué ESCALA o precios por categoría dejarían la tienda como un atajo caro, no como un lujo inútil?

### Takeaway

`ESCALA` pasa de 4 a 1. Con eso, 32 troncos cuestan 24 y 32 de roca cuestan 16: el doble de lo que paga la misión por el mismo trabajo, así que talar sigue saliendo mejor y el atajo se puede pagar. Los enteros de abajo se escriben en el fuente pensando en esa escala nueva. Si se cambia un entero y se deja la escala en 4, el jugador paga el cuádruple de la columna «paga después». No se tocan las misiones, el precio de las parcelas, el tope del cofre de jugador, la bandera de 40 ni la ficha de aldeano de 40.

### Cited Findings

- Los números «ahora» son los del fuente y los que paga el jugador con `ESCALA = 4`, ya contados en las secciones anteriores. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) línea 15 y las líneas citadas en cada fila de la tabla.
- La misión de madera paga 6 por 16 troncos y la de piedra paga 8 por 32 bloques. El doble de esa paga, para la cantidad que vende la tienda, es 24 duros por 32 troncos y 16 duros por 32 de roca. Eso es exactamente el entero que ya está escrito (24 y 16) si `ESCALA` deja de multiplicar. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 740-741.

### Inferences

Cada fila es un cambio. «Paga ahora» es el clic de hoy. «Paga después» es el clic si se aplican este cambio y `ESCALA = 1` a la vez. En la madera derivada, «paga después» es el resultado de `paso`, no el divisor.

| Qué se edita | Ahora en el fuente | Propuesto | Paga ahora | Paga después | Por qué |
| --- | --- | --- | ---: | ---: | --- |
| `ESCALA` | 4 | 1 | ×4 en todas las filas | ×1 | 32 troncos salen a 96 y 32 de roca a 64, ocho veces los 12 y los 8 duros que las misiones pagan por ese trabajo. A 1 salen a 24 y a 16, el doble de esa paga: talar sigue pagando y el puesto pasa a ser un atajo de una o dos misiones. |
| Divisor de la puerta de valla, `troncos / 6` | 6 | 4 | 16 por 8 de roble | 6 por 8 de roble | Ocho puertas de roble cuestan 16 y los tablones que llevan dentro cuestan 24. Con `/ 4`, ocho puertas cuestan lo mismo que esos tablones, como ya hacen las losas. |
| Botón de piedra, entero | 4 | 10 | 16 por 16 | 10 por 16 | Dieciséis botones cuestan la mitad de las dieciséis piedras de las que se cortan. A 10 quedan un poco por encima de esas piedras (8 duros con la escala nueva). |
| Camino de tierra, entero | 6 | 10 | 24 por 32 | 10 por 32 | El camino es la tierra golpeada con la pala, y hoy sale más barato que la tierra (32 por 32). A 10 queda por encima de la tierra (8 con la escala nueva). |
| Alfombra, entero de `colores` | 20 | 24 | 80 por 32 | 24 por 32 | Treinta y dos alfombras gastan unas 21 lanas, que ya cuestan 85. A 24, con la escala nueva, quedan justo por encima de esas lanas. |
| Cama, entero de `colores` | 20 | 8 | 80 | 8 | Una cama son tres lanas y tres tablones, unos 14 duros hoy y unos 3,6 con la escala nueva. 80 es más de cinco veces eso. 8 deja la cama a algo más del doble de sus materiales. |
| Mesa de trabajo, entero | 4 | 2 | 16 | 2 | Cuatro tablones de roble son 3 duros hoy y 0,75 con la escala nueva. 16 es cinco veces la madera. 2 sigue por encima del tablón y deja de costar como medio montón de troncos. |
| Hielo compacto, entero | 20 | 128 | 80 por 16 | 128 por 16 | Dieciséis compactos son 144 de hielo. Con la escala nueva ese hielo vale 108, y 80 hoy es menos de un quinto de los 432 del hielo. 128 es una prima corta sobre 108. Este precio sube aunque la escala baje: el descuento actual es más grande que el ×4. |
| Hielo azul, entero | 48 | 360 | 192 por 4 | 360 por 4 | Cuatro azules son 36 compactos. Al compacto nuevo (128 por 16) eso son 288, y 360 es un cuarto más. Hoy 192 es un quinto de los 972 del hielo del que salen. También sube. |
| Roca musgosa, entero de `obra` | 20 | 48 | 80 por 32 | 48 por 32 | Treinta y dos musgosas son 32 rocas más 32 enredaderas, 160 hoy y 40 con la escala nueva. 48 deja una prima de un quinto. Escaleras, losas y muros de esa familia siguen al bloque, que es lo que ya hace `obra`. |
| Ladrillos musgosos, entero de `obra` | 24 | 52 | 96 por 32 | 52 por 32 | El mismo caso: 32 ladrillos de piedra más 32 enredaderas son 176 hoy y 44 con la escala nueva. 52 queda un poco por encima. |
| Tierra estéril, sacada del grupo de la tierra | 8, junto con tierra, tierra enraizada y barro | 12, solo ella | 32 por 32 | 12 por 32 | La receta son 2 de tierra y 2 de grava por cada 4 estériles. Treinta y dos estériles valen 40 en materiales hoy y 10 con la escala nueva, y la tienda las cobra como tierra lisa. |
| Pistón, entero | 20 | 32 | 80 por 4 | 32 por 4 | Cuatro pistones, a precio de tablón, roca, barrote y redstone, salen a 111 hoy y a unos 28 con la escala nueva. 80 está por debajo. 32 queda un poco por encima de esos 28. |
| Pistón pegajoso, entero | 20 | 24 | 80 por 2 | 24 por 2 | Si el pistón sube a 32 por 4, dos pegajosos son dos pistones más dos slimes, unos 22 duros con la escala nueva. Dejarlos en 20 los pondría por debajo del pistón del que se craftean. |
| Raíl, entero | 20 | 28 | 80 por 16 | 28 por 16 | Dieciséis raíles son 6 lingotes y un palo. Al precio de los barrotes eso son 96 hoy y unos 24 con la escala nueva, y el raíl se vende a 80. |
| Soltador, entero | 12 | 20 | 48 por 4 | 20 por 4 | Cuatro soltadores son 28 rocas y 4 de redstone, 62 hoy y unos 15,5 con la escala nueva. 48 está por debajo. |
| Cartel colgante de bambú | no tiene entero propio: `paso(80 / 2) = 40` | 16, solo el bambú | 160 por 4 | 16 por 4 | Cuatro carteles llevan cuatro bloques de bambú pelado y una cadena y un tercio, unos 47 duros hoy y unos 12 con la escala nueva. 160 es el triple. El de roble, con la misma fórmula, ya cae cerca de sus cadenas y se queda. |

Lo que no se cambia, a propósito:

- Las misiones, el precio de reclamar o vender una parcela, el 1 a 500 del cofre de jugador y su tope de 2000. Esta nota no los recalcula.
- La bandera de 40 y la ficha de aldeano de 40. No están en `ESCALA`.
- El comparador, el observador, el sensor de luz, el azulejo y el ladrillo. Su ratio contra los materiales es de unas 2,4 a 3,7 veces. Con la escala a 1 el sensor pasa de 48 a 12 duros y el comparador de 80 a 20. Sigue siendo una prima alta por un crafteo fino, y el problema gordo era el ×4, no esa prima.
- El bambú como tablón. 32 tablones de bambú pasan de 80 a 20, y 32 de roble de 24 a 6. El bambú sigue más caro por tablón porque 9 cañas dan un bloque y el bloque da 2 tablones, mientras un tronco da 4. Eso es la receta, no un entero suelto. El ítem de bambú (64 por 32 hoy, 8 con la escala nueva) ya está por debajo del bloque.
- Los discos y la cabeza de dragón. Un disco común pasa de 48 a 12, uno raro de 112 a 28, la cabeza de 192 a 48, y la escalera más cara de 216 a 54. La cabeza sigue un poco por debajo de esas 16 escaleras. En duros absolutos las dos caben en un par de saldos iniciales; hoy la escalera cuesta más que una misión entera de madera.

Las tres construcciones, si se aplican la escala y las filas de arriba que les tocan:

- La cabaña pasa de 240 a 46 duros de clics: tablones 18, puerta 4, antorchas 6, mesa 2, horno 8, cama 8. Sigue por encima de talarla (los 96 tablones son 24 troncos, 18 duros de madera, y la misión pagaría por talarlos).
- La casa de piedra no cambia ningún entero propio. 600 / 4 = 150 duros. Cabe en el tope de 340 y no cabe en la misión de piedra sola (32).
- El juego decorativo pasa de 472 a 122: el hormigón, el azulejo, la vela, el farol, la amapola y el cristal bajan con la escala, y la alfombra queda en 24 en vez de en 20 por el entero nuevo.

### Gaps

- No se ha simulado el menú dentro de un servidor arrancado, así que el 893 no está confirmado con `isItem()`. La propuesta no depende de una fila de más o de menos: depende de `ESCALA` y de los enteros citados.
- 128 y 360 del hielo son el punto donde la receta de 9 y de 81 vuelve a ser más cara que comprar el hielo suelto, con una prima corta. Si se prefiere que el hielo azul sea un capricho barato y no un reflejo de los 81 bloques, ese par de enteros es el que hay que discutir. Con los enteros de hoy, comprar el azul es el camino barato y la fila del hielo no sirve para craftear.
