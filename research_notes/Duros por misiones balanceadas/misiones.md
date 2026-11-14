# Sistema de misiones de Preguntones

Fecha de la lectura: 13 de noviembre de 2026. El catálogo que se ejecuta está en Java, dentro del plugin Mundos. No hay un YAML, JSON, datapack ni plugin de quests con la lista. Los duros de cada misión ya se pagan hoy; esta nota no propone importes nuevos.

## ¿Dónde están definidas las misiones?

### Takeaway

Las 14 misiones de estadística, los dos pagos de entrar al Nether y al End, y el pago del dragón viven en `tools/mundos/src/com/preguntones/mundos/MundosPlugin.java`. El jar desplegado en `servidor/plugins/Mundos.jar` contiene la clase `MundosPlugin$Mision` y los textos «Has talado» y «Has entrado». La config solo guarda los números del ciclo (días, blazes, horas), no la lista de misiones.

### Cited Findings

- El array `misiones()` crea 14 objetos `Mision(clave, meta, veces, duros, texto)` en las líneas 686-702. La clase, con esos cinco campos, está en las líneas 800-814. — [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)
- Nether y End no están en ese array. `revisarMisiones()` llama a `pagarUnaVez` con clave `"nether"`, 16 duros y texto «Has entrado al Nether.» si el mundo se llama `survival_nether`, y con clave `"end"`, 18 duros y «Has entrado al End.» si se llama `fin` (líneas 677-682 y 786-797). — [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)
- El dragón se paga en `alMorirDragon`, no con la clase `Mision`: 20 duros por miembro conectado y el comando `lp user <nombre> parent add cazador` (líneas 434-465). — [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)
- La puerta del End usa `dragon-blazes` (defecto 12) y `dias-reset` (defecto 14). Es un contador de blazes, sin campo `duros` (líneas 91, 508-529). — [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) y [config.yml del repo](C:/Users/Ale/Documents/preguntones-m/tools/mundos/config.yml) líneas 1-5
- `plugin.yml` declara el plugin Mundos, comandos `/ciclo` y `/grupo`, permisos `mundos.usar` (por defecto true) y `mundos.admin` (op), y dependencias suaves LuckPerms, Vault, Xo y Chunky. No declara una misión. — [plugin.yml](C:/Users/Ale/Documents/preguntones-m/tools/mundos/plugin.yml) líneas 1-22
- El config del repo fija `dias-reset: 14` y `dragon-blazes: 12`, y no escribe `aviso-horas` (el código usa 48 si falta). El config bajado en `servidor/plugins/Mundos/config.yml` fija `aviso-horas: 48` y no escribe `dias-reset` ni `dragon-blazes`, así que en ese archivo mandan los defectos del código: 14 y 12. — [config del repo](C:/Users/Ale/Documents/preguntones-m/tools/mundos/config.yml) líneas 1-5; [config del servidor](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Mundos/config.yml) líneas 1-4; [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 89-91
- El jar de `servidor/plugins/Mundos.jar` incluye `com/preguntones/mundos/MundosPlugin$Mision.class` y `MundosPlugin.class`. En el bytecode de la clase principal aparecen las cadenas «Has talado», «Has entrado», «madera», «blaze» y «Mision».
- En `servidor/plugins/` hay Mundos.jar, Parcelas, Karma, Xo, Viajes, EssentialsX, Vault, LuckPerms, TAB, GriefPrevention, DiscordSRV, Tebex y otros. No hay un jar de Quests, Jobs ni BattlePass.
- No hay archivos `.mcfunction` ni un datapack de avances en el repo.
- El libro, la web y Discord nombran el sistema con frases sueltas. No definen ids, metas ni importes, salvo el libro del dragón, que escribe a mano «14 días», «12 blazes» y «20 duros». — [Cuaderno.java](C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 319-327
- El informe `reports/Diseño mercado servidor Preguntones.md` habla, en la línea 23, de «completando la tarea corta de la semana» y de un voto con recompensa. En la línea 13 describe un encargo del sábado (construcción, cofre o elección) cuyo premio sería un título o un aspecto. Ese texto no tiene clase, comando ni config en el repo. Lo que sí está programado el sábado es la XO, otro sistema. — [Diseño mercado servidor Preguntones.md](C:/Users/Ale/Documents/preguntones-m/reports/Diseño%20mercado%20servidor%20Preguntones.md) líneas 13 y 23; [Bote.java](C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/Bote.java) líneas 1-12
- `discord/estructura.json` línea 27 dice que «la lista de tareas de la guía nueva no la acepta la API para un bot». Es una nota del alta de Discord, no una misión de Minecraft. — [estructura.json](C:/Users/Ale/Documents/preguntones-m/discord/estructura.json) línea 27
- Las notas de `research_notes/Diseño mercado servidor Preguntones/` citan plugins ajenos (BattlePass, misiones diarias). No son el catálogo de este servidor.

### Inferences

- Quien quiera cambiar un importe de las 14 misiones lo cambia en el constructor de la línea correspondiente de `misiones()`. Nether (16), End (18) y dragón (20) están en otras llamadas, no en ese array.
- El libro, la web y Discord no leen esos constructores. Un cambio de cifra en Java no actualiza solo el «20 duros» escrito en el libro ni el «14 / 12» de la web.

### Gaps

- No se descompiló el jar línea a línea. Se comprobó que la clase y las cadenas están dentro; no se verificó que cada número coincida con el fuente.
- `servidor/plugins/Mundos/estado.yml` solo tiene `ultimo-reset`, `cazador` y `cazador-nombre` (líneas 1-3). No hay una partida guardada con `hechos` que sirva de ejemplo real. El esquema de guardado sale del código, no de un archivo lleno.

## ¿Cuál es el catálogo completo?

### Takeaway

El código no agrupa las misiones en tutorial, diaria, cadena, parcela ni tienda. Hay una lista plana de 14 misiones de estadística, dos pagos de una vez al estar en Nether o End, un pago al matar al dragón, y una puerta de 12 blazes que no entrega duros. En total son 17 pagos con duros y 1 objetivo de progreso sin pago. Ninguna misión tiene un campo de dificultad; el único «fácil» del plugin es la dificultad del mundo salvaje.

### Cited Findings

El orden del array es el orden de `foto()` (líneas 705-723). Si los dos arrays se desfasan, `System.arraycopy` copia solo hasta el más corto y el resto queda en 0. Hoy los dos miden 14.

Tabla de las 14 misiones. «Tramos» es el campo `veces`: cuántas veces se puede cobrar esa meta dentro del ciclo. «Duros» es el pago de un tramo. El texto es el único título: no hay otro nombre visible.

| Id | Texto al cobrar | Meta por tramo | Tramos | Duros por tramo | Qué suma el progreso | Requisito en código | Se repite |
| --- | --- | --- | --- | --- | --- | --- | --- |
| madera | Has talado madera. | 16 | 3 | 6 | `MINE_BLOCK` de `OAK_LOG`, `SPRUCE_LOG`, `BIRCH_LOG`, `JUNGLE_LOG`, `ACACIA_LOG`, `DARK_OAK_LOG`, `MANGROVE_LOG`, `CHERRY_LOG`, `CRIMSON_STEM`, `WARPED_STEM` | Estar en `survival`, `survival_nether` o `fin` en el sondeo | Hasta 3 por ciclo; el ciclo nuevo pone el contador a cero |
| piedra | Has picado piedra. | 32 | 4 | 8 | `STONE`, `COBBLESTONE`, `DEEPSLATE`, `COBBLED_DEEPSLATE` | El mismo sondeo | Hasta 4 por ciclo |
| caza | Has cazado monstruos. | 4 | 3 | 10 | Muertes de `ZOMBIE`, `SKELETON`, `SPIDER`, `CAVE_SPIDER`, `CREEPER`, `DROWNED`, `HUSK`, `STRAY`, `WITCH`, `PILLAGER`, `VINDICATOR`, `PHANTOM` | El mismo sondeo | Hasta 3 por ciclo |
| comida | Has cazado para comer. | 3 | 2 | 6 | Muertes de `COW`, `PIG`, `CHICKEN`, `SHEEP`, `RABBIT` | El mismo sondeo | Hasta 2 por ciclo |
| pesca | Has pescado. | 6 | 2 | 8 | `Statistic.FISH_CAUGHT` | El mismo sondeo | Hasta 2 por ciclo |
| camino | Has explorado. | 40000 | 3 | 8 | `WALK_ONE_CM` + `SPRINT_ONE_CM` (centímetros; 40000 cm = 400 bloques) | El mismo sondeo | Hasta 3 por ciclo |
| cama | Has dormido en una cama. | 1 | 1 | 8 | `Statistic.SLEEP_IN_BED` | El mismo sondeo | 1 por ciclo |
| aldeano | Has comerciado con un aldeano. | 1 | 1 | 10 | `Statistic.TRADED_WITH_VILLAGER` | El mismo sondeo | 1 por ciclo |
| hierro | Has picado hierro. | 8 | 2 | 12 | `IRON_ORE` y `DEEPSLATE_IRON_ORE` | El mismo sondeo | Hasta 2 por ciclo |
| diamante | Has picado diamante. | 2 | 2 | 16 | `DIAMOND_ORE` y `DEEPSLATE_DIAMOND_ORE` | El mismo sondeo | Hasta 2 por ciclo |
| encantar | Has encantado. | 1 | 1 | 12 | `Statistic.ITEM_ENCHANTED` | El mismo sondeo | 1 por ciclo |
| blaze | Has cazado blazes. | 2 | 2 | 14 | `KILL_ENTITY` de `BLAZE` | El mismo sondeo. Es independiente de la puerta de 12 blazes | Hasta 2 por ciclo |
| escombro | Has picado escombros del Nether. | 1 | 1 | 20 | `ANCIENT_DEBRIS` | El mismo sondeo | 1 por ciclo |
| enderman | Has cazado endermen. | 3 | 2 | 10 | `KILL_ENTITY` de `ENDERMAN` | El mismo sondeo | Hasta 2 por ciclo |

Fuente de la tabla: [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 686-763.

Pagos y objetivo que el código trata aparte:

| Id | Texto | Meta | Tramos | Duros | Condición | Requisito | Repetición |
| --- | --- | --- | --- | --- | --- | --- | --- |
| nether | Has entrado al Nether. | estar en el mundo | 1 | 16 | El sondeo ve al jugador en el mundo llamado `survival_nether` | Ese nombre de mundo | 1 por ciclo (`hechos.nether`) |
| end | Has entrado al End. | estar en el mundo | 1 | 18 | El sondeo ve al jugador en el mundo llamado `fin` | Ese nombre de mundo. El portal del End se cancela hasta el día 14 y 12 blazes; el pago en sí mira el mundo, no el portal | 1 por ciclo (`hechos.end`) |
| dragón | Habéis cazado al dragón. | matar al dragón del End | 1 por ciclo, mientras `cazador` esté vacío | 20 por cada miembro del grupo que esté conectado | `EntityDeathEvent` de `EnderDragon` con killer jugador | Grupo de hasta 4, solo en memoria. Quien no tiene grupo cuenta como grupo de uno. Quien está desconectado no cobra | Otra vez al empezar el ciclo, porque `reiniciarCiclo` borra `cazador` |
| puerta-blaze | «Llevas X/12» y «Este ciclo: X/12 blazes» | 12 blazes de este ciclo (`dragon-blazes`) | no paga | 0 | `KILL_ENTITY` de `BLAZE` menos la base guardada en `caza-blaze` | Para cruzar el portal del End también tienen que haber pasado `dias-reset` días (defecto 14) desde `ultimo-reset` | La base se ancla al entrar y se borra al renovar el ciclo. Los blazes anteriores no cuentan |

Fuentes: [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 277-321, 379-396, 434-465, 502-529, 662-682, 786-797, 892-910.

Señales que el código sí expone, y que no son una etiqueta fácil/media/difícil por misión:

- Los mundos salvajes (`survival`, su `_nether`, y `fin`) quedan en `Difficulty.EASY`, con mobs y phantoms. El barrio y el resto quedan en `Difficulty.PEACEFUL`. El log dice «salvaje, dificultad fácil, con mobs.» — [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 240-265
- La meta, los tramos y los duros de la tabla son los únicos números por misión. El pago más bajo del array es 6 (madera) y el más alto es 20 (escombros). El dragón paga 20 y la entrada al End 18.
- La puerta enseña una fracción `blazes/metaBlaze`. Las 14 misiones no enseñan fracción.
- `/ciclo` nombra, sin cifras, «madera, piedra, caza, explorar, hierro, diamante, Nether y el dragón». No nombra comida, pesca, cama, aldeano, encantar, blaze de pago, escombros, enderman ni la entrada al End. — [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) línea 114

Textos de jugador ya escritos, con la cifra solo donde existe:

- Chat al cobrar una misión de la tabla: `§a` + texto + ` +` + (duros × tramos nuevos de ese sondeo) + ` duros.` Línea 783.
- Chat al entrar: `§aHas entrado al Nether. +16 duros.` y `§aHas entrado al End. +18 duros.` Líneas 678-681 y 797.
- Chat del dragón, solo si Vault acepta el ingreso: `§aHabéis cazado al dragón. +20 duros.` Línea 451.
- Broadcast a todos, sin el importe: `§d<nombres> §fhan cazado al dragón. El survival se borra el <fecha>. El rango Cazador se queda.` Líneas 463-464.
- Broadcast de apertura: `§dEl dragón ya se puede cazar. §fHacen falta <metaBlaze> blazes de este ciclo. Los de antes no cuentan.` Líneas 478-479.
- `/ciclo`, además de la frase de la línea 114: «Se borra <avisoHoras> horas después de que un grupo mate al dragón. Las parcelas no.»; la fecha larga si ya cayó; «El dragón se abre el <fecha>.» o «El dragón ya se puede cazar.»; «Hacen falta <metaBlaze> blazes de este ciclo. El grupo, de hasta 4, se queda el rango Cazador.»; «Llevas §e<blazes>/<meta>§f.» Líneas 113-127.
- Portal cancelado, como máximo cada 4 segundos: «§eEl dragón se abre el <fecha>. Harán falta <meta> blazes de este ciclo.» o «§eEste ciclo: §f<blazes>/<meta> blazes. §eLos de antes no cuentan.» Líneas 516-539.
- Libro «Preguntones», página «Los duros»: «Te los dan solos en el survival: talar, picar y más. y al vender a otro. El euro no los crea.» Sin lista ni cifras. — [Cuaderno.java](C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 190-204
- Libro, página «El survival»: explica explorar, la cama y el borrado, y un botón «Cuándo se borra» que ejecuta `/ciclo`. Sin misiones. Líneas 303-316.
- Libro, página «El dragón»: «A las 48 horas se borra ese mundo.» «Abre a los 14 días. 12 blazes, de ahora. Los de antes, no.» «Hasta 4. Al caer, 20 duros y Cazador.» Líneas 319-329. Esas cifras están escritas en el libro, no leídas de la config.
- Lore del libro: «Veinte páginas.» y «Lo azul se pulsa.» Líneas 57-59. No habla de misiones.
- Marcador TAB, visible al entrar (`hidden-by-default: false`, comando `/sb`): título «Preguntones»; líneas `Duros: %preguntones_duros%`, `Parcela: %preguntones_parcela%`, `Survival: %preguntones_ciclo%`, `XO: %preguntones_xo%`. Cabecera «Preguntones». Pie «Sábado 21:30 · la XO». — [TAB config.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/TAB/config.yml) líneas 2-23
- `%preguntones_ciclo%` no es una misión. Devuelve la fecha corta del borrado (`EEE d HH:mm`, español), o `abre <fecha>`, o `dragón vivo`. — [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 154-164; [DurosExpansion.java](C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/DurosExpansion.java) líneas 38-45
- `%preguntones_duros%` es el saldo Vault, no el premio de una misión. — [DurosExpansion.java](C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/DurosExpansion.java) líneas 38-39
- Web, diccionario: «También salen de las misiones del survival y de venderle a otro jugador.» Sin catálogo. — [index.html](C:/Users/Ale/Documents/preguntones-m/web/index.html) línea 122
- Web, portada: «Los duros salen de jugar y de venderle algo a otro jugador.» Línea 98. El End «no abre hasta el día 14» y «hacen falta 12 blazes cazados en ese ciclo». Línea 93.
- Web, entrar: «Entras con 25 duros. La primera parcela cuesta 25. Las siguientes salen de jugar en el survival.» Línea 97. Línea 106 repite 48 horas, día 14 y 12 blazes. — [entrar.html](C:/Users/Ale/Documents/preguntones-m/web/entrar.html)
- Web, normas: el End no abre hasta el día 14 y los blazes de un ciclo anterior no cuentan. — [normas.html](C:/Users/Ale/Documents/preguntones-m/web/normas.html) línea 40
- Discord, normas (`mensajes.json` clave `texto`, línea 12): «Los duros salen de jugar en el survival y de venderle a otro jugador. Entras con 25.» También 48 horas, día 14 y 12 blazes. No dice la palabra «misiones».
- Discord, guía (clave `guia`, línea 14): «El resto sale de las misiones del survival y de venderle a otro jugador.» No lista misiones ni duros de misión. La XO sí lleva cifra: «Cada ronda son 4 duros.» — [mensajes.json](C:/Users/Ale/Documents/preguntones-m/discord/mensajes.json)
- El panel (`MenuPantalla`) nombra tienda, bandera y banco con precios de duros. No tiene una fila de misiones. — [MenuPantalla.java](C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/MenuPantalla.java) líneas 63-65
- La tienda de bloques enseña el saldo en el lore de una pepita: «<saldo> duros». No enseña premios de misión. — [Tienda.java](C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 405-409
- El prefijo del rango es `&dCazador &r`, peso 15, padre `default`. El chat de Essentials muestra `{PREFIX}{USERNAME}`. — [cazador.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/LuckPerms/yaml-storage/groups/cazador.yml) líneas 1-7; [EssentialsChat config.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/EssentialsChat/config.yml) línea 5
- TAB ordena grupos `admin,mod,cazador,vecino,default`. — [TAB config.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/TAB/config.yml) líneas 26-27

Sistemas que suenan a misión y no lo son:

- Karma, al comerciar: `§aHas comerciado con un aldeano. §f+2 de karma.` Al dormir: `§aHas dormido. §f+2 de karma.` Al pescar: `§aHas pescado. §f+1 de karma.` Topes diarios: 3 tratos, 1 dormir, 4 pescas, 8 monstruos (el monstruo no manda frase). — [KarmaPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/karma/src/com/preguntones/karma/KarmaPlugin.java) líneas 413-456 y 549-561; [karma/config.yml](C:/Users/Ale/Documents/preguntones-m/tools/karma/config.yml) líneas 8-14
- La XO paga `4 × personas en la pista` al ganador y escribe `§aGanas la XO. §f+<duros> duros.` — [Bote.java](C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/Bote.java) líneas 1-12; [XoPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/XoPlugin.java) líneas 761-771
- Los 25 duros iniciales son `starting-balance: 25` de Essentials, no una misión. — [Essentials config.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/config.yml) líneas 9-10

### Inferences

- Suma aritmética de los importes ya escritos, si una persona completa todos los tramos, entra a las dos dimensiones y cobra el dragón: madera 18, piedra 32, caza 30, comida 12, pesca 16, camino 24, cama 8, aldeano 10, hierro 24, diamante 32, encantar 12, blaze 28, escombro 20, enderman 20, Nether 16, End 18, dragón 20. Total 340 duros por persona y ciclo. En un grupo, cada miembro conectado cobra sus 20; no se reparten.
- El jugador no puede leer esa tabla dentro del juego. Ve actividades sueltas en `/ciclo` y en el libro, y la cifra solo después de cobrar, más el «20 duros» fijo de la página del dragón.
- La misión `blaze` (2 muertes, 14 duros, dos veces) y la puerta (12 muertes, 0 duros) leen la misma estadística con bases distintas. Completar la misión no abre el End, y la fracción de la puerta no descuenta los tramos ya pagados.

### Gaps

- No hay un nombre de dificultad por misión en código ni en textos. Inventar fácil/media/difícil sería diseño, y este encargo no lo hace.
- No se comprobó en un cliente qué bloques de la 26.1.2 quedan fuera de las listas (por ejemplo tallos que el array no nombra). El hecho usable es la lista exacta de materiales y entidades de `troncos`, `piedra`, `monstruos`, `comida` y `mena`.

## ¿Cómo se detecta el avance y qué se ejecuta al completar?

### Takeaway

No hay un evento de Bukkit ni un listener de romper bloque. Cada 5 segundos, si Vault está cargado y el jugador está en `survival`, `survival_nether` o `fin`, el plugin resta una foto de estadísticas y, al cruzar una meta, ingresa duros con Vault y escribe un chat. El avance a medias no llama a nada y no se guarda.

### Cited Findings

- El temporizador se registra en `onEnable`: `runTaskTimer(this, this::revisarMisiones, 100L, 100L)`. 100 ticks son 5 segundos, también para la primera pasada. — [MundosPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) línea 105
- Si `economia` es null, `revisarMisiones` sale sin mirar a nadie (líneas 662-664). La economía se toma del servicio Vault `Economy` (líneas 655-659).
- El jugador entra en el sondeo solo si `getWorld().getName()` es `survival`, `survival_nether` o `fin` (líneas 667-669). El barrio se llama `world` en otros puntos del mismo archivo (líneas 403 y 282).
- La primera vez que alguien aparece en ese sondeo, `bases` guarda `foto(jugador)` y el delta de ese tick es 0, porque `ahora` se toma justo después (líneas 671-675). `bases` es un `Map` en memoria (línea 77). `guardarEstado` no lo escribe (líneas 595-611).
- El progreso que se pasa a `pagar` es `ahora[i] - base[i]` (línea 675). Las estadísticas de Bukkit son del jugador, no del mundo.
- `pagar`: si `progreso < meta`, return. Si no, `merecidas = min(veces, progreso / meta)` con división entera. Si `merecidas <= hechas`, return. Si no, `nuevas = merecidas - hechas` y `depositPlayer(jugador, duros * nuevas)`. Si Vault no responde `transactionSuccess`, no se apunta el hecho y no hay mensaje; el siguiente sondeo lo reintenta. Si entra, se guarda `hechos[clave][uuid] = merecidas`, se llama `guardarEstado()` y se manda un solo chat con el producto, no un chat por tramo (líneas 766-783).
- `pagarUnaVez` exige `hechos[clave][uuid] == 0`, ingresa la cantidad fija y deja el contador en 1 (líneas 786-797).
- `hechos` se carga y se guarda en `plugins/Mundos/estado.yml` como `hechos.<clave>.<uuid>: <entero>` (líneas 622-652). El archivo bajado hoy no trae esa sección.
- `reiniciarCiclo` vacía `bases`, `hechos`, `baseBlaze` y `cazador`, así que los topes vuelven a cero en el ciclo siguiente (líneas 304-309). También teletransporta a la plaza y avisa: «§eEste mundo se ha renovado. Tus parcelas siguen en el barrio.» (líneas 294-297).
- El dragón no pasa por `pagar`. Da el permiso de LuckPerms aunque Vault falle o sea null. El chat `+20 duros` solo sale si el depósito tiene éxito (líneas 449-451). Un segundo dragón no paga mientras `cazador != null` (líneas 438-440). El grupo (`liderDe`, `equipoDe`) no se escribe en el yaml.
- La puerta ancla la base de blazes en `anclarCaza` al entrar y al arrancar, para quien esté conectado, y la guarda en `caza-blaze.<uuid>` (líneas 498-506 y 608-610). `frenoDragon` cancela el `END_PORTAL` si falta fecha o blazes (líneas 390-396).
- No hay `sendTitle`, sonido, boss bar, action bar ni avance de Minecraft en Mundos. Las boss bars del repo son de viaje («Yendo a…») y de la XO. — [ViajesPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/ViajesPlugin.java) línea 318; [XoPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/XoPlugin.java) línea 127
- No hay tests bajo `tools/mundos/`. El directorio de fuente solo tiene `MundosPlugin.java`, `Pregen.java`, `config.yml` y `plugin.yml`.

### Inferences

- Un reinicio del plugin o del servidor borra el avance a medias (la base vive en RAM) y conserva los tramos ya cobrados (están en `hechos`). Para el siguiente tramo hay que volver a juntar la meta entera después del nuevo ancla.
- Como el delta usa estadísticas globales y el filtro es «en qué mundo estás cuando toca el sondeo», picar en el barrio antes del primer ancla no cuenta. Si el ancla ya existe, lo picado fuera y vuelto a medir en el siguiente sondeo dentro de survival, Nether o End entra en el delta, porque la estadística no distingue el mundo.
- Si en un solo sondeo el progreso cruza dos metas, el jugador ve una línea y un ingreso `duros × nuevas`, no dos líneas.
- No hay un gancho público al que otro plugin pueda enganchar «misión completada» o «avance parcial». El único efecto externo es el depósito de Vault y, en el dragón, el comando de LuckPerms.

### Gaps

- No se miró el código de Vault/Essentials para ver el mensaje propio de `/balance` o del depósito. El plugin de misiones, por su parte, solo añade el `sendMessage` citado.
- No se siguió cada teletransporte de Viajes. El pago del End se dispara por estar en el mundo `fin` durante el sondeo. El bloqueo encontrado es el del portal (`alPortal`). `ViajesPlugin.java` línea 1255 trata `fin` como mundo válido para la cama; no se leyó si ese viaje comprueba los 12 blazes.

## ¿Dónde tendría que verse «recibes N duros», antes y después?

### Takeaway

Después de completar, la frase con la cifra ya existe y es un chat verde. Antes de completar no hay menú, lore, boss bar ni fila de marcador que enseñe el premio de cada misión. El único premio escrito por adelantado es «20 duros» en la página del dragón del libro. El marcador enseña el saldo total, no el premio.

### Cited Findings

Sitios donde el jugador ya ve la cifra al cobrar (después):

- Chat de la misión de estadística, línea 783 de MundosPlugin: `§aHas talado madera. +6 duros.` (o el múltiplo si entran varios tramos a la vez). Igual para las otras 13, con su texto y su producto.
- Chat de dimensión, línea 797: `§aHas entrado al Nether. +16 duros.` y `§aHas entrado al End. +18 duros.`
- Chat del dragón, línea 451: `§aHabéis cazado al dragón. +20 duros.` Solo a cada miembro conectado, y solo si el depósito entra.
- El marcador pasa a mostrar el saldo nuevo en `Duros: %preguntones_duros%` porque el depósito es Vault. Esa línea no dice cuánto acaba de entrar. — [TAB config.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/TAB/config.yml) línea 20; [DurosExpansion.java](C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/DurosExpansion.java) líneas 38-39
- Si luego abre la tienda de bloques, la pepita repite el saldo: «<n> duros». — [Tienda.java](C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 405-409

Sitios donde se habla del premio antes de cobrar, y qué cifra hay hoy:

- Libro, página «El dragón»: «20 duros y Cazador» está escrito antes de matarlo. — [Cuaderno.java](C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/Cuaderno.java) línea 327
- Libro, página «Los duros»: dice que te los dan al talar y picar, sin números. Líneas 192-194.
- `/ciclo` línea 114: nombra actividades y «los duros», sin números. La fracción de blazes (líneas 127, 522, 529) es progreso de la puerta, y la puerta no paga.
- Marcador: saldo y estado del ciclo (`abre <fecha>`, fecha de borrado, o `dragón vivo`). Sin fila de misión. — [TAB config.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/TAB/config.yml) líneas 18-22; [textoCiclo](C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 154-164
- Web `index.html` líneas 98 y 122, `entrar.html` línea 97, Discord `guia` y `texto`: dicen que las misiones o el survival dan duros, sin el importe de cada una.
- No hay inventario de misiones, lore de objeto de misión, boss bar de misión ni action bar de misión en el fuente de Mundos.

Dónde una línea nueva de «recibes N duros» tendría que colocarse para que se vea antes y después, con el código que hay hoy:

- Después, el único sitio que ya construye la frase del premio es el `sendMessage` de `pagar` (783), `pagarUnaVez` (797) y `alMorirDragon` (451). Esas tres cadenas interpolan el entero. El broadcast del dragón (463-464) no lleva el +20.
- Antes, para las 14 misiones y para Nether/End, no hay un widget que ya reserve esa frase. Los textos previos que habría que ampliar, si se quiere la cifra sin inventar una pantalla, son la página «Los duros» del libro (Cuaderno 190-204), la línea 114 de `/ciclo`, el párrafo del diccionario web (index.html 122), la frase de `entrar.html` 97, y la pregunta «¿De dónde salen los duros?» de `discord/mensajes.json` línea 14. La página del dragón ya tiene el 20.
- El marcador puede seguir enseñando el saldo. Para enseñar el premio antes, haría falta una línea nueva: hoy `Survival:` es la fecha del ciclo, no una misión.
- DiscordSRV tiene el comentario «El chat del juego va a #chat y los avisos a #anuncios. La consola no se publica.» y `DiscordChatChannelMinecraftToDiscord: true`. — [DiscordSRV config.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/DiscordSRV/config.yml) líneas 1 y 18. El pago usa `Player.sendMessage`, no un mensaje de chat de jugador, y Mundos no envía un webhook.

### Inferences

- Cambiar `mision.duros` cambia solo el chat de después y el saldo del marcador. No cambia el libro, la web ni Discord, porque esos textos no leen el array.
- Una UI de misiones que enseñe el premio antes de completar sería código nuevo. No hay un menú a medio hacer en Mundos, Viajes ni Parcelas.
- El chat de karma (`+N de karma.`) puede salir en la misma acción que el de duros (pescar, dormir, comerciar). Son dos frases. La de duros lleva la palabra «duros»; la de karma lleva «de karma».

### Gaps

- No se leyó el fuente de DiscordSRV. Con la config y con el uso de `sendMessage`, no queda demostrado que el chat verde llegue a #chat. Tampoco hay en Mundos una llamada que lo publique en #anuncios.
- No se abrió el juego para ver el marcador en un cliente. El texto sale de `TAB/config.yml` y de los placeholders.

## ¿Hay misiones ya atadas a dinero, objetos, permisos, parcelas o la tienda?

### Takeaway

Sí al dinero: las 14 misiones, Nether, End y el dragón ingresan duros por Vault. El dragón, además, añade el grupo LuckPerms `cazador`. Ninguna misión entrega objetos, una parcela ni un producto de la tienda. La tienda gasta duros; no los da por misiones.

### Cited Findings

- Dinero: `economia.depositPlayer` en las líneas 450 (20), 777 (`mision.duros * nuevas`) y 791 (16 o 18). La moneda que ve el jugador se llama duros en el chat, el libro, el marcador y la web. Essentials arranca la cuenta en 25 (`starting-balance: 25`), aparte de las misiones. — [Essentials config.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/config.yml) línea 9
- Permiso: solo el dragón ejecuta `lp user <nombre> parent add cazador` (línea 449). El grupo tiene el prefijo `Cazador` y hereda `default`. No da ítems en ese yaml. — [cazador.yml](C:/Users/Ale/Documents/preguntones-m/servidor/plugins/LuckPerms/yaml-storage/groups/cazador.yml)
- Objetos: `pagar` y `pagarUnaVez` no crean `ItemStack`. El libro de Viajes se entrega por otro camino y no es el premio de una misión. — [Cuaderno.java](C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 49-84
- Parcelas: el mensaje de renovación dice «Tus parcelas siguen en el barrio» (línea 297). Comprar parcela es otro plugin y se paga con duros; no es una recompensa de misión. El libro fija la primera en 25 y la sexta en 275, tope 6. — [Cuaderno.java](C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 134-140
- Tienda: `tienda/paquetes.json` rechaza un paquete de duros con el motivo «El euro no fabrica la moneda del servidor.» La web dice que la tienda es aspecto (color, estela, frase) y que con euros no se compran duros ni terreno. — [paquetes.json](C:/Users/Ale/Documents/preguntones-m/tienda/paquetes.json) línea 31; [index.html](C:/Users/Ale/Documents/preguntones-m/web/index.html) líneas 98-103 y 122
- La tienda de bloques del juego (`/puesto`) cobra duros por bloques y enseña el saldo. No tiene una sección de misiones. — [Tienda.java](C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 65 y 405-409; [Cuaderno.java](C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/Cuaderno.java) línea 200
- La XO también ingresa duros por Vault, con otro texto y otro disparador (ganar la ronda). No está en `misiones()`. — [XoPlugin.java](C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/XoPlugin.java) líneas 766-771

### Inferences

- Atar duros a «completar la misión» ya ocurre. Lo que no existe es enseñar ese importe antes de completar, salvo el 20 del dragón en el libro.
- Un premio de objeto, parcela o permiso nuevo no tiene un campo en `Mision`. Hoy el constructor solo admite clave, meta, veces, duros y texto.

### Gaps

- No se abrió `tienda/paquetes.json` entero ni el catálogo de precios de Parcelas para listar la tienda. Para este encargo basta con que ningún premio de `MundosPlugin` llama a la tienda ni crea una parcela.
- El informe de diseño habla de una tarea semanal y de un voto con recompensa. No están en el plugin de misiones. No se buscó un segundo plugin que las hubiera implementado con otro nombre más allá del listado de jars y de las búsquedas `misión`, `mision`, `quest`, `encargo` y `tarea` en Java, YAML, JSON, HTML y Markdown, dejando fuera regiones, logs y librerías.
