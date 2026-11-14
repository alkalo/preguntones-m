# Tienda, parcelas y otros sumideros de duros

Fecha de la lectura: 13 de noviembre de 2026. No se ha rediseñado ningún precio. No se ha vuelto a bajar el servidor por SFTP: un `pull` escribiría archivos del proyecto, y esta pasada solo podía escribir estas notas. Las cifras de `servidor/plugins/` son las de la copia local (Essentials tocado el 13 nov 2026 a las 17:26; Parcelas y GriefPrevention, el 12 nov 2026).

Convención: **implementado** significa que el juego cobra esa cifra. **Solo diseño** significa que el número está en un informe y no hay código ni config que lo cobre. El precio final de `/puesto` es `base × 4`. La `base` es el entero escrito en `Catalogo.java`; `ESCALA = 4` se aplica en `add()`.

## Qué vende la tienda oficial, a qué precio y quién puede comprarlo

### Takeaway

Hay tres mercados distintos. `/puesto` es la tienda del servidor: precios fijos en código, cualquiera con `parcelas.usar` (por defecto, todo el mundo), y el duro cobrado se destruye. El cofre de venta de la parcela es jugador a jugador, precio por unidad de 1 a 500, con un 10 % que entra en la caja del barrio. El cartel `[Trade]` de Essentials también es jugador a jugador y la config no le pone impuesto. La tienda web de 4,99 € no gasta duros.

### Cited Findings

- El comando `/puesto` abre el menú y exige `parcelas.usar` o `parcelas.admin`. El permiso `parcelas.usar` es `default: true`. El grupo LuckPerms `default` lo tiene explícito. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 157-166; [plugin.yml](tools/parcelas/plugin.yml) líneas 13-16 y 34-36; [default.yml](servidor/plugins/LuckPerms/yaml-storage/groups/default.yml) línea 16.
- Cada clic de compra hace `withdrawPlayer` por el precio del ítem y entrega el stack. No hay `depositPlayer` ni `sumarCaja` en ese camino: el duro sale de la circulación. Si el inventario no cabe, se devuelve el importe. — [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 122-145.
- La bandera no está en el catálogo. `/bandera` y el botón Bandera del panel cobran **40 duros** fijos y dan un estandarte blanco que, al colocarlo, crea un punto de viaje. El máximo colocado es **5**. Comprar no mira ese tope; colocarlo sí. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 12-13; [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 76-88 y 171-172; [MenuPantalla.java](tools/viajes/src/com/preguntones/viajes/MenuPantalla.java) líneas 64 y 99; [NombreBandera.java](tools/viajes/src/com/preguntones/viajes/NombreBandera.java) líneas 4 y 12-14; [Banderas.java](tools/viajes/src/com/preguntones/viajes/Banderas.java) líneas 105-109 y 142-147.
- Fórmula del catálogo: `precio_cobrado = base × 4`. `paso(v) = max(2, techo(v / 2) × 2)` redondea las piezas derivadas al par, con suelo 2 antes de la escala (o sea 8 duros), pero ninguna receta de este archivo baja de base 4, así que el mínimo real es **16 duros**. El comentario de cabecera cuadra con el cálculo: 32 troncos de roble = 96 duros, tablones = 24, madera de corteza = 128. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 7-15, 530-555, 578-611.
- El constructor de `Tienda` llama a `catalogo()`. Si un nombre de material no existe en esa versión de Paper, el plugin lanza y no abre la tienda a medias. No se ha ejecutado el catálogo contra el `Material` del servidor. — [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 52 y 427-443.
- Secciones, en este orden: Luces, Madera, Piedra, Color, Cristal, Plantas, Tierra, Nether, End, Cobre, Mar, Muebles, Mecanismos. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 21-36.
- Compras a 16 duros (el escalón más barato de `/puesto`):

| Qué | Cantidad | Duros | Dónde |
| --- | ---: | ---: | --- |
| Mesa de trabajo | 1 | 16 | Muebles, base 4 |
| Botón de piedra | 16 | 16 | Piedra, base 4 |
| Puerta, botón, placa, cartel y puerta de valla de roble, abeto, abedul, selva, acacia y roble oscuro | 6 / 16 / 8 / 8 / 8 | 16 | Madera, base de tronco 24, `paso` |
| Puerta y botón de manglar, cerezo y roble pálido | 6 y 16 | 16 | Madera, bases 28 y 32 |

  El pelado y el sin pelar de tronco y corteza cuestan lo mismo. El bambú no vende tronco ni corteza (`BAMBOO` se salta esas cuatro líneas) y su botón sale a 40, no a 16. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 99-118, 172, 415, 530-555.

- Madera. Seis especies comparten la base 24 (roble, abeto, abedul, selva, acacia, roble oscuro). Manglar y cerezo comparten la 28. Cada fila de tronco vale también para el pelado. Precio ya escalado.

| Pieza | Ud. | Base 24 | Base 28 | Roble pálido 32 | Bambú 80 | Hongo 36 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Tronco o tallo | 32 | 96 | 112 | 128 | no se vende | 144 |
| Corteza o hifas | 32 | 128 | 152 | 176 | no se vende | 192 |
| Tablones | 32 | 24 | 32 | 32 | 80 | 40 |
| Escalera | 16 | 24 | 24 | 24 | 64 | 32 |
| Losa | 64 | 24 | 32 | 32 | 80 | 40 |
| Valla | 16 | 24 | 24 | 32 | 72 | 32 |
| Puerta de valla | 8 | 16 | 24 | 24 | 56 | 24 |
| Puerta | 6 | 16 | 16 | 16 | 40 | 24 |
| Trampilla | 8 | 24 | 32 | 32 | 80 | 40 |
| Botón | 16 | 16 | 16 | 16 | 40 | 24 |
| Placa | 8 | 16 | 24 | 24 | 56 | 24 |
| Cartel | 8 | 16 | 24 | 24 | 56 | 24 |
| Cartel colgante | 4 | 48 | 56 | 64 | 160 | 72 |
| Estante | 4 | 24 | 32 | 32 | 80 | 40 |

  Hongos: carmesí y distorsionado, tallo e hifas en vez de tronco y corteza. Extras fijos del bambú y el manglar, ya escalados: raíces de manglar 32/64, raíces embarradas 32/80, bloque de bambú, bambú pelado y mosaico 16/80, losa de mosaico 32/80, escalera de mosaico 16/120. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 99-118 y 530-555. Cálculo: `paso` y `× 4`, comprobado con el comentario de las líneas 8-10.

- Obra (bloque, escalera, losa, muro). El muro solo existe si el código pasa un material de muro. Precio ya escalado. `esc.` = escalera de 16; `losa` = 32; `muro` = 16.

| Base (ud. del bloque, base) | Bloque | Esc. | Losa | Muro | Familias |
| --- | ---: | ---: | ---: | ---: | --- |
| 32, 16 | 64 | 48 | 32 | 32 | Piedra, roca, granito, diorita, andesita, pizarra rocosa, toba. Todas con muro. |
| 32, 20 | 80 | 64 | 40 | 40 | Roca musgosa, ladrillos de piedra, pizarra pulida, ladrillos y baldosas de pizarra, toba pulida, ladrillos de toba (con muro). Granito, diorita y andesita pulidos, sin muro. |
| 32, 24 | 96 | 72 | 48 | 48 | Ladrillos musgosos. Ladrillos del Nether y piedra negra pulida. |
| 32, 28 | 112 | 88 | 56 | 56 | Ladrillos rojos del Nether y ladrillos de piedra negra. |
| 32, 36 | 144 | 112 | 72 | 72 | Ladrillos del End. |
| 16, 16 | 64 | 96 | 64 | 64 | Ladrillos de barro. |
| 16, 20 | 80 | 120 | 80 | 80 | Ladrillos. Piedra negra. |
| 16, 24 | 96 | 144 | 96 | 96 | Arenisca y arenisca roja. |
| 16, 28 | 112 | 168 | 112 | 112 | Arenisca lisa y arenisca roja lisa, sin muro. Ladrillos de resina, con muro. |
| 16, 32 | 128 | 192 | 128 | 128 | Cuarzo, sin muro. Prismarina, con muro. |
| 16, 36 | 144 | 216 | 144 | 144 | Cuarzo liso y púrpur, sin muro. Ladrillos de prismarina y prismarina oscura, sin muro. |

  El techo de una compra de `/puesto` en este catálogo es **216 duros** (escalera de un bloque de base 16/36). — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 121-173, 307-345, 340-346, 374-377, 565-576.

- Piedra que no sale de `obra()`, ya escalada:

| Pieza | Ud. | Duros |
| --- | ---: | ---: |
| Botón de piedra | 16 | 16 |
| Losa de piedra lisa | 32 | 40 |
| Ladrillos cincelados, pizarra cincelada, toba cincelada, ladrillos de toba cincelados, estalactita, grumo de resina | 16 | 48 |
| Pizarra, calcita, bloque de caliza | 32 | 64 |
| Ladrillos agrietados, piedra lisa, ladrillos y baldosas de pizarra agrietados | 32 | 80 |
| Racimo de amatista | 4 | 80 |
| Arenisca cortada, losa de arenisca cortada, y las mismas de arenisca roja; resina; amatista | 16 o 32 la losa | 96 |
| Arenisca cincelada y arenisca roja cincelada | 16 | 112 |
| Resina cincelada | 16 | 128 |
| Pizarra reforzada | 8 | 144 |

  — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 127-173.

- Luces, ya escaladas. Las de color o de oxidación repiten el precio.

| Pieza | Ud. | Duros | Variantes |
| --- | ---: | ---: | --- |
| Antorchas, pepinos de mar | 32 y 8 | 24 | 1 cada una |
| Velas sin teñir, liquen luminoso | 8 y 16 | 32 | 1 cada una |
| Antorchas de alma, antorchas de cobre, velas de color, calabazas | 16, 16, 8, 4 | 48 | velas ×16 colores |
| Faroles, piedra luminosa | 4 y 16 | 64 | 1 |
| Bombilla y farol de cobre encerados | 4 | 64 | ×4 estados de oxidación, cada uno |
| Faroles de alma, luz de hongo | 4 | 80 | 1 |
| Lámparas, varas del End, lámparas de mar, tres luces de rana | 4, 4, 4, 2 | 96 | luces de rana: ocre, perlada y verde |

  — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 71-96.

- Color. Terracota sin teñir: 32 por 64. El resto, 16 colores al mismo precio:

| Pieza | Ud. | Duros |
| --- | ---: | ---: |
| Tinte | 16 | 32 |
| Estandarte | 1 | 48 |
| Lana | 16 | 64 |
| Alfombra, terracota teñida, cama | 32, 32, 1 | 80 |
| Azulejo, polvo de hormigón, hormigón | 16, 32, 32 | 96 |

  — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 175-185.

- Cristal: cristal 32/64, panel 32/32, cristal opaco 16/112, cristal teñido 16/64 ×16, panel teñido 32/48 ×16. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 188-194.

- Plantas, ya escaladas. El grupo de 16 por 24 incluye diente de león, amapola, orquídea, allium, houstonia, cuatro tulipanes, margarita, aciano, lirio, pétalos, hojarasca, hierba, helecho, arbusto seco, caña y nenúfar. El de 8 por 24: hierba alta, helecho grande, arbusto, tres enredaderas. El de 4 por 40: girasol, lila, rosal, peonía, dos azaleas, antorcha floral, flor de cactus, dos flores de ojo, dos hojas, dos hongos del Nether, dos raíces, brotes, dos champiñones, raíces colgantes. Hojas: 32 por 32 (once tipos, incluida la de roble pálido). Brotes: 4 por 48 (nueve, más el propágulo). Sueltos: bambú 64/32, cactus y calabaza y flores silvestres 8 o 16 por 40, calabaza tallada y bayas luminosas 4 u 8 por 48, musgo pálido colgante 16/80, planta odre y arbusto de luciérnagas 4/80, espora y corazón del creaking 1/96. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 196-278.

- Tierra, ya escalada: tierra, tierra estéril, tierra enraizada y barro 32/32; arcilla, grava y arena 32/48; camino de tierra 32/24; arena roja y barro compacto 32/64; césped, podzol y micelio 16/64; musgo 32/48; tapiz de musgo 32/32; musgo pálido 16/96; tapiz pálido 16/64; nieve 32/32; hielo 16/48; hielo compacto 16/80; hielo azul 4/192. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 281-304.

- Nether suelto, ya escalado: piedra del Nether 32/32; valla 16/64; agrietados 32/96; cincelados 16/64; basalto 32/64; basalto pulido y liso 32/80; ladrillos negros agrietados 32/112; piedra negra cincelada 16/80; botón 8/24; placa 8/32; arena y tierra de almas 32/64; magma 16/64; dos bloques de verruga 16/80; dos nicelios y bloque de hueso 16/64; obsidiana llorosa 16/112; pilar y ladrillos de cuarzo 16/128; cuarzo cincelado 16/144. Los bloques de obra de esta sección están en la tabla de obra. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 307-337.

- End suelto: piedra del End 32/112, pilar de púrpur 16/144, flor de chorus 4/96. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 340-345.

- Cobre. El bloque sin encerar: 16 por 80. El juego encerado, en los cuatro estados de oxidación, ya escalado: bloque, cortado, losa y reja 96 (losas de 32, el resto de 16); escalera 16/144; cincelado 16/112; puerta 4/48; trampilla 8/48; barrotes 16/64; cadena 8/48; estatua 1/80; pararrayos 1/48. Las bombillas y faroles están en Luces, no aquí. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 348-371.

- Mar suelto: esponja 2/96, esponja mojada 4/64, alga y hierba marina 16/24, bloque de alga seca 16/48. Coral, cinco tipos (tubo, cerebro, burbuja, fuego, cuerno): pieza y abanico 4/48, bloque 16/112, bloque muerto 16/64. Prismarina en la tabla de obra. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 374-387 y 558-563.

- Muebles sueltos, ya escalados:

| Duros | Ud. | Piezas |
| ---: | ---: | --- |
| 16 | 1 | Mesa de trabajo |
| 24 | 8 o 1 | Maceta; compostador |
| 32 | 32 o 1 | Escalera de mano; horno |
| 48 | varios | Marcos 8, cuadros 4, maceta decorada 2, atril, campana no, maniquí, hoguera, bloque de notas 4, telar, mesa de mapas, mesa de flechas, mesa de herrería, cortapiedras, afiladora, caldero, pararrayos: los de 1 ud. a 48 salvo los que la fila de abajo sube |
| 64 | 4, 1, 16 | Estanterías; hoguera de alma; bloque de panal |
| 80 | 4, 1, 2 | Marcos luminosos; estantería cincelada; cadenas 4; trampilla de hierro |
| 96 | 1 o 16 | Campana, alto horno, barrotes, puerta de hierro, colmena |
| 144 | 1 | Gramola |
| 192 | 1 | Cabeza de dragón |

  Campana, gramola y cabeza de dragón están en sus filas (bases 24, 36 y 48). Discos «baratos» (13, 13, cat, blocks, chirp, far, mall, mellohi, stal, strad, ward, 11, wait, Lava Chicken): 1 por 48. Discos «caros» (5, otherside, Pigstep, Relic, Creator, Creator caja, Precipice, Tears): 1 por 112. Fragmentos de cerámica, 24 nombres: 1 por 48. Patrones flor, mampostería y borde dentado: 1 por 48. Patrones creeper, calavera, globo, piglin, flujo y ráfaga: 1 por 80. Cabezas de esqueleto, zombi, creeper y piglin: 1 por 80. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 390-527.

- Mecanismos, ya escalados:

| Duros | Ud. | Piezas |
| ---: | ---: | --- |
| 24 | 16 u 8 | Antorchas de redstone; palancas |
| 48 | varios | Redstone 32, repetidores 4, sensor de luz 1, soltadores 4, slime 4, dianas 4 |
| 64 | 4 o 2 | Miel; raíles detectores y activadores; placas de hierro y de oro |
| 80 | varios | Comparadores 2, pistones 4, pistones pegajosos 2, dispensadores 2, tolva 1, raíles 16, vagoneta 1 |
| 96 | varios | Observadores 2, bloque de redstone 4, raíles propulsores 8 |

  — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 437-461.

- Cofre de venta, distinto de `/puesto`. Se craftea con cuatro lingotes de oro, un cofre y un cartel: no cuesta duros. Solo en parcela propia del barrio, máximo 4 por parcela. El dueño pone el precio **por unidad, de 1 a 500**. El comprador paga `unidades × precio`, con tope de **2000 duros** por compra. El impuesto es `total × 10 / 100` hacia abajo: en una venta de 4 la caja se queda 0; en una de 40, se queda 4 y el dueño 36. El 10 % va a la caja; el resto, al dueño. No puedes comprarte a ti mismo. En mora no deja comprar. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 67, 110, 270-276, 477-483, 869-934, 1258-1262; [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 5-21 y 33-41; [Pruebas.java](tools/parcelas/src/com/preguntones/parcelas/Pruebas.java) líneas 52-58.
- Cartel de trueque de Essentials: `enabledSigns` solo tiene `trade`. El comentario de la config dice que buy y sell no están porque uno fabrica objetos y el otro fabrica duros. El grupo `default` puede crear y usar el cartel. No hay una clave de impuesto del cartel en `Essentials/config.yml`. El precio lo pone quien carga el cartel. — [Essentials/config.yml](servidor/plugins/Essentials/config.yml) líneas 19-22; [default.yml](servidor/plugins/LuckPerms/yaml-storage/groups/default.yml) líneas 13-14.
- `worth.yml` trae el ejemplo de serie de Essentials (troncos a 2, etc.). El grupo `default` no tiene `essentials.sell` ni `essentials.worth`, y los carteles buy/sell están apagados. Esas cifras no son una tienda que el jugador nuevo pueda usar. — [worth.yml](servidor/plugins/Essentials/worth.yml) líneas 1-8; [default.yml](servidor/plugins/LuckPerms/yaml-storage/groups/default.yml) líneas 4-18.
- La web vende un paquete VIP a **4,99 € con IVA**, no en duros: etiqueta verde, color del nombre y la frase «entra al barrio». La propia página dice que, si no hay botón de pago, todavía no se cobra, y que con euros no se compran duros, herramientas ni terreno. El grupo LuckPerms `vecino` es ese adorno y no añade un precio en duros. — [tienda.html](web/tienda.html); [condiciones.html](web/condiciones.html); [identidad.js](web/js/identidad.js) línea 10; [vecino.yml](servidor/plugins/LuckPerms/yaml-storage/groups/vecino.yml).

### Inferences

- Retocar `/puesto` es editar la base en `Catalogo.java` (y recordar el `× 4`) o el 40 de la bandera en tres sitios de texto más el cobro. No hay un yaml de la tienda.
- El cofre de venta y el cartel `[Trade]` no tienen lista de precios: el sumidero fijo ahí es solo el 10 % del cofre. El cartel, con la config leída, mueve duros enteros de una cuenta a otra.
- 16 duros es la primera compra posible en la tienda del servidor. 216 es la más cara de un solo clic de ese menú. Hielo azul y cabeza de dragón quedan en 192.

### Gaps

- No se ha comprobado, arrancando Paper 26.1.2, que todos los `Material.valueOf` del catálogo existan. Si uno falta, la tienda no arranca.
- No se ha leído el código de EssentialsX para ver si el cartel `[Trade]` retiene algo que la config no declara. En el yaml no hay impuesto.
- No se ha mirado si Tebex está cobrando ya el 4,99 € en el panel. La web dice que el cobro llega cuando haya botón. En cualquier caso no es un gasto de duros.

## Qué cuesta una parcela: reclamar, ampliar, vender, impuesto, bandera, botón, tipos y topes

### Takeaway

Solo hay un tipo de parcela, de 16 de lado. La primera libre cuesta 25 duros y el duro se destruye. La siguiente sube 30 y luego 10 más cada vez: 25, 55, 95, 145, 205 y 275. Juntar dos parcelas no cobra el camino. Vender a otro jugador admite de 20 a 2000 y el barrio se queda el 10 % entero, hacia abajo. No hay precio de bandera de parcela ni de botón de parcela: la bandera de viaje es la de 40 duros, y el botón es un objeto de la tienda.

### Cited Findings

- La config viva y los defectos del código coinciden:

| Clave | Valor | Efecto |
| --- | ---: | --- |
| `precios.libre` | 25 | Precio de la primera parcela libre |
| `precios.minimo` | 20 | Suelo al vender o al ofrecer |
| `precios.maximo` | 2000 | Techo al vender o al ofrecer |
| `precios.max-por-jugador` | 6 | Tope por persona |
| `precios.impuesto-porcentaje` | 10 | Solo en la reventa entre jugadores |
| `clan.max-personas` | 8 | Personas del clan, sin coste en duros |
| `clan.parcelas-por-persona` | 6 | Está en el yaml y ningún Java lo lee |

  — [servidor/plugins/Parcelas/config.yml](servidor/plugins/Parcelas/config.yml) líneas 9-17; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 86-90. `topeDe` devuelve siempre `maxParcelas`. — mismas líneas 721-726. El clan lo repite en el chat: el tope sigue en 6 por persona. — [Clanes.java](tools/parcelas/src/com/preguntones/parcelas/Clanes.java) líneas 25 y 106.

- Fórmula de una parcela nueva, con `n` parcelas ya poseídas por esa persona:

```
precio(0) = libre
precio(n) = libre + suma(i de 0 a n-1) de (30 + 10 * i)
          = 25 + 30*n + 5*n*(n-1)     con libre = 25
```

| Ya tienes | Pagas por la siguiente | Acumulado si solo reclamas libres |
| ---: | ---: | ---: |
| 0 | 25 | 25 |
| 1 | 55 | 80 |
| 2 | 95 | 175 |
| 3 | 145 | 320 |
| 4 | 205 | 525 |
| 5 | 275 | 800 |
| 6 | no hay siguiente | 800 |

  El cuaderno imprime la misma serie. Reclamar hace `withdrawPlayer` y no llama a `sumarCaja`: esos duros no entran en la caja. Abandonar no devuelve nada. En mora no deja reclamar ni comprar. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 268-309, 503-515, 553-559; [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 126 y 138.

- No hay un comando de ampliar una parcela. La cuadrícula es una sola: lado 16, salto 20, así que entre dos casas hay un camino de 4. Si las dos son de la misma persona, ese camino estrecho pasa a hierba sin un cobro extra. Los pasillos anchos del spawn no se tocan. El precio de «hacer la casa más grande» es el de la segunda parcela (55 si solo tienes una). — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 80-85 y 304-308; [config.yml](servidor/plugins/Parcelas/config.yml) líneas 2-8; [Cuadricula.java](tools/parcelas/src/com/preguntones/parcelas/Cuadricula.java) líneas 8 y 110.

- Reventa y oferta directa usan el mismo reparto. El comprador paga el precio entero. `tasa = precio * impuesto / 100` (división entera). El dueño recibe `precio - tasa` y `sumarCaja(tasa)` mete la tasa en la caja. A 20 duros, el mínimo, la tasa es 2 y el vendedor cobra 18. A 25, sería 2 y 23, pero 25 es el reclamo de una libre, que no pasa por aquí. Ofrecer exige que el otro esté conectado; vender deja el cartel para quien escriba `/parcela comprar` estando dentro. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 344-360, 385-406, 422-439, 478-499.

- La bandera de 40 y el tope de 5 están en la pregunta anterior. No hay otra «flag» de parcela con precio. Los botones de madera y el de piedra son filas de `/puesto`, no una tasa por colocar un botón en la parcela. El script de pintar botones de región no contiene la palabra duro. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 172 y 551.

- Un amigo construye y no paga. El clan no cambia el precio ni el tope. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 533-536; [Clanes.java](tools/parcelas/src/com/preguntones/parcelas/Clanes.java) líneas 106 y 242.

### Inferences

- El escalón que de verdad encarece el terreno es el reclamo sucesivo, no un impuesto de mantenimiento. No hay renta diaria ni semanal sobre la parcela.
- Comprar la libre número 1 destruye 25. Comprar la parcela de otro a 25 solo destruye 2 (la tasa) y mueve 23. Para el balance no son el mismo sumidero.
- `clan.parcelas-por-persona: 6` es una clave muerta. Cambiarla en el yaml no cambia el juego mientras `topeDe` ignore esa clave.

### Gaps

- No hay, en el código leído, un segundo tamaño de parcela ni un precio por «tipo» (esquina, comercial, granja). Si el servidor guardó parcelas a mano fuera de esta cuadrícula, esta fórmula no las describe.
- La copia local de `Parcelas/config.yml` no se ha contrastado hoy por SFTP con el disco del host. Coincide con los `getInt(..., defecto)` del código.

## Qué otros sumideros quitan duros al jugador o a la economía

### Takeaway

Lo que se destruye de verdad es: el reclamo de parcela libre, toda compra de `/puesto` y de bandera, y el interés del banco. El 10 % de las reventas no se destruye: se aparta en la caja y puede volver como préstamo. El capital devuelto vuelve a la caja. La XO, las misiones y el dragón crean duros; no los quitan.

### Cited Findings

- Impuesto del cofre: constante `Cuentas.IMPUESTO = 10`, no la clave `precios.impuesto-porcentaje`. Hoy las dos valen 10. Si alguien cambia solo el yaml, la reventa de parcelas cambia y el cofre no. Fórmula: `total * 10 / 100`. Pruebas: 4 → 0 de tasa; 40 → 4 de tasa y 36 al dueño. Tope de una compra: 2000. Precio por unidad: 1 a 500. — [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 5-31; [Pruebas.java](tools/parcelas/src/com/preguntones/parcelas/Pruebas.java) líneas 52-58.
- Banco. Un solo préstamo. Capital de 1 hasta lo que haya en la caja. Plazos fijos: 1 día al 10 %, 3 días al 20 %, 7 días al 30 %. Interés = `(capital * porcentaje + 99) / 100`, y como mínimo 1 si capital y porcentaje son positivos. Es el techo al duro. Pruebas: 25 al 10 % = 3; 100 al 30 % = 30; 1 al 10 % = 1. Al pagar, el interés se resta primero y no entra en la caja; el capital sí vuelve a la caja. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 247-269 y 475-484; [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 24-30; [Pruebas.java](tools/parcelas/src/com/preguntones/parcelas/Pruebas.java) línea 55.
- Ejemplos de interés destruido, con la fórmula del código:

| Capital | 1 día, 10 % | 3 días, 20 % | 7 días, 30 % |
| ---: | ---: | ---: | ---: |
| 1 | 1 | 1 | 1 |
| 25 | 3 | 5 | 8 |
| 50 | 5 | 10 | 15 |
| 100 | 10 | 20 | 30 |

- Aval. Hasta 50 de capital se puede avalar con duros, sin parcela. Por encima de 50 hace falta una parcela, que queda bloqueada. Si al vencer el aval es la parcela y el saldo no cubre, la parcela vuelve al barrio y la deuda se cierra. Si el aval es en duros y no llega, se cobra lo que haya, el resto sigue debiendo y, en mora, se bloquean `/pay`, `/pagar`, el cofre de venta y las parcelas nuevas. Los duros que entren después saldan primero. `/puesto` no mira `enMora`. Pagar antes cuesta el total ya firmado: no hay descuento por devolver pronto. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 282-284, 383-400, 455-484, 558-572; [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 874-876; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 278-280.
- La caja nace una vez con **150 duros** si no existe `banco.yml` o si `caja-lista` es falso. No es un cobro a un jugador. El informe dice lo mismo y añade que no se repone al reiniciar; el código solo siembra en esa carga. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 38 y 628-640; [economia-del-barrio.md](reports/economia-del-barrio.md) líneas 21-31.
- Essentials es la moneda (Vault). Saldo inicial **25**. Mínimo 0. Máximo **2000**. Símbolo « duro» como sufijo. El kit `inicio` da 8 panes, un pico de madera, una pala de oro y 16 antorchas: cero duros. `/home`, `/spawn` y `/sethome` están permitidos y la config no les pone un precio; el retraso de teletransporte de Essentials es 3 segundos. Viajes espera 15 segundos y tampoco cobra. — [Essentials/config.yml](servidor/plugins/Essentials/config.yml) líneas 9-13 y 24-28; [kits.yml](servidor/plugins/Essentials/kits.yml) líneas 5-12; [default.yml](servidor/plugins/LuckPerms/yaml-storage/groups/default.yml) líneas 5-12; [viajes/config.yml](tools/viajes/config.yml) líneas 6-12.
- GriefPrevention, en la copia local, tiene `ClaimBlocksPurchaseCost: 8.0` y `ClaimBlocksSellValue: 0`. Son duros por bloque de claim extra, no por la parcela del barrio. El plugin de viajes solo pregunta si hay un claim para no soltar a alguien dentro; no cobra. El grupo `default` no menciona permisos de GriefPrevention. Los bloques iniciales (100) y los 40 por hora no son duros. — [GriefPrevention/config.yml](servidor/plugins/GriefPrevention/config.yml) líneas 1-20; [ViajesPlugin.java](tools/viajes/src/com/preguntones/viajes/ViajesPlugin.java) líneas 673-691.
- La XO no cobra la entrada. Quien gana recibe `4 × max(personas en la pista, 1)` creados con `depositPlayer`. El texto «4 duros por persona» es el premio, no un boleto. — [Bote.java](tools/xo/src/com/preguntones/xo/Bote.java) líneas 3-12; [XoPlugin.java](tools/xo/src/com/preguntones/xo/XoPlugin.java) líneas 755-772; [Pruebas.java](tools/xo/src/com/preguntones/xo/Pruebas.java) líneas 60-61.
- Las misiones del survival ingresan duros, no los gastan. Por repetición, hasta el tope de veces: madera 6, piedra 8, caza 10, comida 6, pesca 8, camino 8, cama 8, aldeano 10, hierro 12, diamante 16, encantar 12, blaze 14, escombro 20, enderman 10. Una vez: Nether 16, End 18. El dragón, una vez por ciclo, da 20 a quien esté del grupo conectado en ese momento. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 450-451 y 686-703 y 766-797.
- `/pay` mueve duros entre jugadores. En mora se cancela. No hay una comisión en el código de Parcelas. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 558-572.
- Karma no retira duros. El descuento de esmeraldas del aldeano no es la moneda del barrio. — búsqueda en [tools/karma](tools/karma) sin `withdraw` ni `duro`.

### Inferences

- Para una pasada de balance, los sumideros que encogen la masa monetaria son tres: reclamo de libre, `/puesto` (bandera incluida) e interés. El 10 % es un aparcamiento en la caja, reutilizable.
- Un jugador en mora puede seguir vaciando duros en `/puesto`, porque esa tienda no consulta la deuda. El cofre de otro jugador y la parcela sí están cerrados.
- El tope de 2000 de Essentials es el mismo número que el precio máximo de una parcela y que el tope de una compra en el cofre. No es una tasa. Qué pasa con un depósito que se pasaría de 2000 no está escrito en la config local.

### Gaps

- No está demostrado que un jugador sin rango pueda ejecutar la compra de bloques de GriefPrevention a 8 duros. El precio está en la config; el permiso no está en el grupo `default`, y GriefPrevention suele traer permisos por defecto que esta lectura no ha abierto en el jar.
- No hay, en el código del proyecto, una renta, una tasa de warp, un coste de `/home` ni un coste de clan.
- El informe [economia-del-barrio.md](reports/economia-del-barrio.md) describe el cofre y el banco que ya están en el código. No aporta un precio distinto. No sustituye a `Cuentas.java` si algún día divergen.

## Cuál es el camino más barato a la primera parcela, a la tienda y a una mejora que se note

### Takeaway

Con el saldo inicial de 25, la primera parcela libre se paga justo y no sobra nada. La interacción más barata con `/puesto` son 16 duros, así que no cabe en el mismo saldo que la parcela. La mejora de casa que el código reconoce, unir el camino, exige la segunda parcela: 55 duros más, 80 en total. La bandera, 40, es más barata que esa segunda parcela y no agranda la casa.

### Cited Findings

- Al entrar, Essentials da 25 duros. La web y el cuaderno dicen lo mismo, y el código de parcelas cobra 25 por la primera libre. El kit no suma más dinero. — [Essentials/config.yml](servidor/plugins/Essentials/config.yml) línea 9; [entrar.html](web/entrar.html); [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 96 y 126; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 86 y 287-302.
- Camino garantizado, sin depender de otro jugador: pala de oro en una parcela libre = **25 duros**. Es el 100 % del saldo inicial. — mismas fuentes.
- Suelo del mercado de segunda mano: **20 duros**, si alguien ha puesto `/parcela vender 20`. Es más barato que reclamar, y solo existe si hay vendedor. De esos 20, 2 van a la caja y 18 al vendedor. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 87 y 483-490.
- Primera compra en `/puesto`: **16 duros** (mesa de trabajo, botón de piedra, o puerta/botón/cartel de las maderas baratas). 25 + 16 = 41, por encima del saldo inicial. Quien gasta los 25 en la parcela necesita otros 16 antes de tocar la tienda del servidor. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 15 y 611.
- El cofre de otro jugador puede cobrar **1 duro** por una unidad. Es el mínimo implementado de una compra entre jugadores, y depende de que haya género a ese precio. — [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 7-8.
- Mejoras con precio fijo, de menor a mayor, solo con cifras que el juego cobra:

| Paso | Duros | Qué queda desbloqueado | ¿Destruye el duro? |
| --- | ---: | --- | --- |
| Unidad en un cofre ajeno al mínimo | 1 | El objeto de ese hueco | No: 0 de tasa; el dueño recibe 1 |
| Stack más barato de `/puesto` | 16 | Ese stack (mesa, botón, puerta…) | Sí |
| Parcela de otro al mínimo legal | 20 | Una parcela ya construida, si existe | 2 a la caja, 18 al vendedor |
| Primera libre | 25 | Parcela de 16×16, borde rojo, protección | Sí, los 25 |
| Bandera | 40 | Un punto de viaje propio, hasta 5 | Sí |
| Segunda libre | 55 | Otra parcela; si es la de al lado, el camino estrecho pasa a hierba | Sí, los 55 |
| Dos libres seguidas | 80 | Casa doble | Sí, 80 |
| Cinco banderas | 200 | El tope de puntos de viaje | Sí |
| Las seis libres | 800 | El tope de terreno | Sí, 800 |

  — fórmulas y líneas ya citadas en las dos preguntas anteriores.

- El cofre de venta propio cuesta materiales (4 lingotes de oro, cofre, cartel), no duros. Poner la primera tienda de jugador es más barato en duros que cualquier clic de `/puesto`: cuesta 0 duros. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 270-276.

### Inferences

- El orden barato para quien quiere terreno y no quiere depender de un vendedor es: entrar con 25, reclamar, y dejar la tienda para cuando las misiones hayan ingresado al menos 16. Esas misiones son ingresos, no precios de esta nota; la más pequeña implementada es de 6 duros, así que una sola no paga el clic de 16.
- «Mejora que se note» en el propio código es la segunda parcela. 55 es 2,2 veces la primera y 3,4 veces el clic más barato de `/puesto`. La bandera (40) se sitúa entre la primera parcela y la segunda.
- De la primera a la sexta el precio se multiplica por 11 (25 → 275). El paquete completo de terreno, 800, es 32 veces la entrada.

### Gaps

- No hay un precio implementado para «la mejora» como objeto único. 55 es la lectura de lo que el juego trata como ampliar la casa. Otra lectura válida es la bandera a 40, si la mejora es poder volver a un sitio.
- El camino de 20 duros no está siempre disponible. El de 25 sí, mientras queden parcelas libres y la persona no tenga ya 6 ni esté en mora.

## Qué precios viven en código, cuáles en config y cuáles solo en los informes

### Takeaway

La escalera que un jugador nuevo pisa está en config de Essentials (los 25 de entrada) y en config de Parcelas (25 / 20 / 2000 / 6 / 10 %), con la curva +30 +10 escrita en Java. Todo `/puesto` y la bandera de 40 están solo en Java. Los informes o repiten esas cifras o hablan de un mercado que no es el que cobra el servidor.

### Cited Findings

- **Config, y el juego lo usa.** `servidor/plugins/Essentials/config.yml`: saldo inicial 25, máximo 2000. `servidor/plugins/Parcelas/config.yml`: libre 25, mínimo 20, máximo 2000, tope 6, impuesto 10, clan 8 personas. `servidor/plugins/GriefPrevention/config.yml`: 8.0 duros por bloque de claim comprado y 0 al venderlos. Los defectos de `getInt` en Parcelas son los mismos números, así que un yaml vacío cobraría igual. — [Essentials/config.yml](servidor/plugins/Essentials/config.yml) líneas 9-11; [Parcelas/config.yml](servidor/plugins/Parcelas/config.yml) líneas 9-17; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 86-90; [GriefPrevention/config.yml](servidor/plugins/GriefPrevention/config.yml) líneas 17-19.
- **Código, sin yaml.** Catálogo completo de `/puesto` y `ESCALA = 4`. Bandera 40. Curva de parcelas `30 + 10 * i`. Impuesto del cofre, topes 1–500 y 2000, interés y plazos 10/20/30. Caja inicial 150. Premio de la XO, 4 por persona, que es un ingreso. Misiones y dragón, ingresos. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java); [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) línea 77; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 553-559; [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java); [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 38 y 255-257; [Bote.java](tools/xo/src/com/preguntones/xo/Bote.java); [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 686-703.
- **Config presente y no leída por el plugin de parcelas.** `clan.parcelas-por-persona: 6`. — [config.yml](servidor/plugins/Parcelas/config.yml) línea 17 frente a [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 725-726.
- **Informe que coincide con el código, no es una lista aparte.** [economia-del-barrio.md](reports/economia-del-barrio.md): 10 %, ejemplos 4→0 y 40→4, plazos 10/20/30, redondeo hacia arriba con mínimo 1, caja 150, interés destruido, precio 1–500, tope 2000, cuatro cofres, receta del cofre. Sirve de prosa; la fuente para retocar es el Java.
- **Solo diseño, o cita de otro servidor. No lo cobra Preguntones.** En [Diseño mercado servidor Preguntones.md](reports/Diseño%20mercado%20servidor%20Preguntones.md) línea 23: un único sistema de carteles Essentials, sin recompra administrativa, y una subasta futura con el **5 %** y **48 horas** que el texto atribuye a Towny Reborn, más las cifras de esa wiki (150 por chunk, 2000 por pueblo, y el resto de la frase). El juego real tiene `/puesto` con precios fijos, cofre con 10 % y no tiene casa de subastas. El 5 % de esa frase no es el impuesto del barrio.
- **Euros, no duros.** Comisión de Tebex del 5 %, fijo de PayPal y el 4,99 € del VIP. El 4,99 está publicado en la web; las comisiones de pasarela están en el informe de diseño como coste del operador, no como precio que paga el jugador en duros. — [Diseño mercado servidor Preguntones.md](reports/Diseño%20mercado%20servidor%20Preguntones.md) alrededor de la sección de Tebex; [identidad.js](web/js/identidad.js) línea 10.
- **Texto de jugador, no una segunda tarifa.** El cuaderno, `web/entrar.html`, `web/index.html` y las preguntas de la XO repiten 25, la serie hasta 275, la bandera a 40 y los 4 duros de la XO. Si el código cambiara y esos textos no, mentirían. Hoy coinciden. — [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 138 y 271; [preguntas.yml](tools/xo/preguntas.yml) líneas 328-330.

### Inferences

- Una pasada de balance que solo edite `reports/` no cambia lo que se cobra. La curva de parcelas se parte entre yaml (`libre`) y Java (los +30 y +10). El impuesto del cofre ni siquiera mira el yaml.
- El documento de diseño describe un servidor anterior a esta tienda: allí el cartel era el único precio y el impuesto de subasta era el 5 % de Towny. El servidor que hay ahora hunde duros en `/puesto` y en el terreno, que es lo contrario de «el cartel es el único sistema».

### Gaps

- No se ha abierto el `config.yml` de dentro de `Parcelas.jar`. El jar del repo solo lista `plugin.yml`. La config que manda, si el servidor arrancó con la copia local, es `servidor/plugins/Parcelas/config.yml`.
- No se ha resincronizado el host. Si alguien cambió un yaml en el panel después de la copia local, esta nota no lo ve. Parcelas en disco local sigue igual que los defectos del código, así que un desvío solo podría estar en Essentials, GriefPrevention o en un `banco.yml` con la caja ya movida. El saldo de la caja en marcha no se ha leído.
