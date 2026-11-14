# Obtención actual de duros

Inventario del código, de la configuración local del servidor y de los dos informes de diseño, leído el 13 de noviembre de 2026. No se ha hecho pull ni push. No se han leído regiones del mundo ni `conexion.json`. La copia local de `servidor/plugins` sí está en el repo y es la que se cita. No hay `plugins/Essentials/userdata/*.yml`, ni `plugins/Parcelas/banco.yml`, ni `plugins/Mundos/estado.yml` en esa copia: los saldos vivos y la caja actual no están en el checkout.

Los plugins instalados que tocan la moneda son EssentialsX 2.22.0, Vault, Parcelas, Mundos y Xo. En `servidor/plugins` no hay jar de Jobs, Votifier, VotingPlugin ni de un plugin de quests. AuthMe está instalado; en sus yml locales no aparece `money`, `balance`, `reward` ni `economy`. Tebex está instalado; sus comandos de entrega no dan duros.

## Dónde está la economía de duros

### Takeaway

El saldo del jugador lo guarda EssentialsX y lo expone Vault. Quien crea duros nuevos hacia el jugador es Essentials (alta de 25), Mundos (misiones, Nether, End y dragón) y Xo (bote de la ronda). Parcelas mueve duros entre jugadores, la caja del barrio y los sumideros, y no tiene un comando de conceder saldo.

### Cited Findings

- EssentialsX 2.22 registra la moneda y Vault la expone. `starting-balance: 25`, `min-money: 0`, `max-money: 2000`, `currency-symbol: ' duro'` con el símbolo detrás. El comentario del archivo dice que los carteles `buy` y `sell` no se activan porque uno fabrica objetos y el otro fabrica duros; `enabledSigns` solo contiene `trade`. — [config.yml de Essentials](servidor/plugins/Essentials/config.yml) líneas 1-22
- El kit `inicio` da 8 panes, 1 pico de madera, 1 pala de oro y 16 antorchas. No incluye dinero. Lo da el bloque `newbies.kit: inicio`. — [kits.yml](servidor/plugins/Essentials/kits.yml) líneas 6-11; [config.yml de Essentials](servidor/plugins/Essentials/config.yml) líneas 24-28
- Parcelas declara `depend: [Vault]` y `softdepend: [Essentials, PlaceholderAPI, Karma]`. Lee el saldo con `Economy.getBalance` y lo trunca con `Math.floor`. Ingresa con `depositPlayer` y cobra con `withdrawPlayer`. — [plugin.yml de Parcelas](tools/parcelas/plugin.yml) líneas 5-6; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 93-99 y 562-619
- Mundos y Xo hacen `softdepend` / `depend` de Vault y depositan con la misma API. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 655-660 y 777; [XoPlugin.java](tools/xo/src/com/preguntones/xo/XoPlugin.java) líneas 755-772; [plugin.yml de Xo](tools/xo/plugin.yml) líneas 6-7
- La caja del barrio y los préstamos van a `plugins/Parcelas/banco.yml` (`caja`, `caja-lista`, `prestamos`, `avisos`). El progreso de misiones va a `plugins/Mundos/estado.yml`, clave `hechos`. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 57-59 y 667-680; [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 98 y 647-650
- Los precios de parcela que usa el servidor están en `servidor/plugins/Parcelas/config.yml`: libre 25, mínimo de venta 20, máximo 2000, 6 por jugador, impuesto 10 %. El código usa esos valores como defecto si falta la clave. No hay `config.yml` dentro de `tools/parcelas/`; los defectos están en el Java. — [config.yml de Parcelas](servidor/plugins/Parcelas/config.yml) líneas 9-14; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 86-90
- La tienda web (`tienda/paquetes.json`) entrega LuckPerms (`parent add vecino`, color, estela, mensaje de entrada). Una entrada de la lista de rechazados dice que el euro no fabrica duros. No hay comando `eco`. — [paquetes.json](tienda/paquetes.json) líneas 31 y 78-80
- No hay carpeta `web/` con economía. El marcador `%preguntones_duros%` lo publica Parcelas si PlaceholderAPI está presente, y el scoreboard de TAB lo enseña. — [DurosExpansion.java](tools/parcelas/src/com/preguntones/parcelas/DurosExpansion.java) líneas 14-16 y 38-39; [config.yml de TAB](servidor/plugins/TAB/config.yml) líneas 10-23
- `worth.yml` de Essentials es la plantilla de serie (comentario en inglés y `log: 2.0`). Ningún grupo salvo el comodín de `admin` tiene un permiso de venta a la administración. — [worth.yml](servidor/plugins/Essentials/worth.yml) líneas 1-8; [admin.yml](servidor/plugins/LuckPerms/yaml-storage/groups/admin.yml) líneas 4-5

### Inferences

- Un pase de balance puede tratar Vault como la única cuenta del jugador. Los YAML de Parcelas y Mundos guardan caja, deudas y «ya cobrado», no el saldo.
- El catálogo de `/puesto` y la bandera son sumideros (quitan duros y dan objetos). Están en el mismo plugin que el mercado, pero no son una fuente.

### Gaps

- No se ha abierto el jar de EssentialsX. No está verificado en este repo si un `depositPlayer` que cruzaría `max-money: 2000` falla entero o ingresa solo hasta el tope. Los llamadores, si `transactionSuccess()` es falso, no marcan la misión como pagada ni dan por cobrada una venta.
- La carpeta `userdata` de Essentials está vacía en esta copia. No hay un ejemplo real de la clave YAML del saldo. Karma sí espera el archivo `plugins/Essentials/userdata/<uuid>.yml` para leer `timestamps.logout`. — [KarmaPlugin.java](tools/karma/src/com/preguntones/karma/KarmaPlugin.java) líneas 610-616
- No está en el checkout el `banco.yml` ni el `estado.yml` del servidor en marcha. La caja puede no ser ya 150.

## De qué formas sube hoy el saldo

### Takeaway

El dinero nuevo que entra al jugador, con cifra en código o config, es: 25 al crear la cuenta, las misiones del survival (tope 320 duros por persona y ciclo, más 20 si está conectado al matar al dragón) y la XO (4 duros por persona que estaba en la pista al empezar la ronda). El resto de subidas son traspasos (venta, `/pay`, cartel `trade`, préstamo) o devoluciones. Voto, jobs, quests, paga por tiempo, interés de ahorro y la tarea semanal del informe de mercado no tienen importe ni implementación.

### Cited Findings

#### Alta, una vez

- `starting-balance: 25`. Los textos de juego dicen lo mismo: «Entras con 25», «Traes 25 duros» y la pregunta de la XO «Al entrar en Preguntones recibes 25 duros» marcada verdadera. Ningún plugin Java deposita 25 en `PlayerJoinEvent`. — [config.yml de Essentials](servidor/plugins/Essentials/config.yml) línea 9; [mensajes.json](discord/mensajes.json) línea 14; [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) línea 96; [preguntas.yml](tools/xo/preguntas.yml) líneas 330-331
- El kit de novato no suma duros. — [kits.yml](servidor/plugins/Essentials/kits.yml) líneas 6-11

#### Misiones del survival (dinero nuevo)

Se revisan cada 100 ticks (5 segundos), solo con el jugador conectado y en `survival`, `survival_nether` o `fin`. El pago es `duros * nuevas`, donde `nuevas` son las tandas completadas desde la última vez, con tope `veces`. La base se fotografía la primera vez que se le ve en esos mundos en el ciclo; el delta usa estadísticas de toda la vida, así que lo minado en el barrio entre dos revisiones sí entra si la foto ya existía, y lo anterior a la primera foto no se paga. Al renovar el ciclo se borran `bases` y `hechos`, y se puede volver a cobrar. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 105, 307-308 y 662-798

Constructor: `Mision(clave, meta, veces, duros, texto)`. Pago por tanda y máximo por persona y ciclo (`veces * duros`):

| Clave | Qué cuenta | Meta por tanda | Tandas | Duros por tanda | Máximo |
| --- | --- | --- | --- | --- | --- |
| madera | troncos de roble, abeto, abedul, selva, acacia, roble oscuro, manglar, cerezo, tallos carmesí y distorsionado | 16 | 3 | 6 | 18 |
| piedra | stone, cobblestone, deepslate, cobbled deepslate | 32 | 4 | 8 | 32 |
| caza | zombie, esqueleto, araña, araña de cueva, creeper, drowned, husk, stray, bruja, pillager, vindicator, phantom | 4 | 3 | 10 | 30 |
| comida | vaca, cerdo, gallina, oveja, conejo | 3 | 2 | 6 | 12 |
| pesca | `FISH_CAUGHT` | 6 | 2 | 8 | 16 |
| camino | `WALK_ONE_CM` + `SPRINT_ONE_CM` (40.000 cm = 400 bloques) | 40000 | 3 | 8 | 24 |
| cama | `SLEEP_IN_BED` | 1 | 1 | 8 | 8 |
| aldeano | `TRADED_WITH_VILLAGER` | 1 | 1 | 10 | 10 |
| hierro | iron ore y deepslate iron ore | 8 | 2 | 12 | 24 |
| diamante | diamond ore y deepslate diamond ore | 2 | 2 | 16 | 32 |
| encantar | `ITEM_ENCHANTED` | 1 | 1 | 12 | 12 |
| blaze | kills de blaze | 2 | 2 | 14 | 28 |
| escombro | ancient debris | 1 | 1 | 20 | 20 |
| enderman | kills de enderman | 3 | 2 | 10 | 20 |

Suma de esa tabla: 286. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 686-746

Además, una sola vez por ciclo y persona, si el chequeo les pilla dentro:

- Nether (`survival_nether`): 16. Texto: «Has entrado al Nether. +16 duros.»
- End (mundo `fin`): 18. Texto: «Has entrado al End. +18 duros.»

286 + 16 + 18 = 320 duros de misiones y dimensiones por persona y ciclo, si completa todas las tandas y visita los dos mundos. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 677-682 y 786-797

El dragón suma otros 20 duros a cada miembro del grupo que esté conectado en el momento de la muerte. El grupo admite como máximo 4. Si el dragón ya tiene cazador en el ciclo, no se vuelve a pagar. Quien está desconectado no cobra. El mensaje es «Habéis cazado al dragón. +20 duros.» También recibe el grupo LuckPerms `cazador`, que no cambia ingresos. El End no abre hasta `dias-reset: 14` y hacen falta `dragon-blazes: 12` de ese ciclo. El mundo se borra `aviso-horas` después, defecto 48, y ahí se reinician los hechos. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 89-91, 434-451 y 852; [config.yml de Mundos](tools/mundos/config.yml) líneas 1-5; [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 325-327

Tope de una persona que lo hace todo y está online al matar al dragón: 320 + 20 = 340 duros nuevos de Mundos en ese ciclo, más los 25 de alta si es su primer ciclo de cuenta. El 340 no es una constante del código; es la suma de las cifras de arriba.

`/ciclo` no paga. Dice: «Los duros salen de jugar allí: madera, piedra, caza, explorar, hierro, diamante, Nether y el dragón.» — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 113-114

#### XO (dinero nuevo)

- `Bote.POR_PERSONA = 4`. `premio(personas) = 4 * max(personas, 1)`. — [Bote.java](tools/xo/src/com/preguntones/xo/Bote.java) líneas 3-12
- El multiplicador se fija una vez, al empezar la primera pregunta de la ronda: `bote = max(vivos.size(), 1)`. No se recalcula si alguien cae después. Cada frase dura 15 segundos. Una sesión abierta a mano dura `SESION_MS = 60 * 60 * 1000` ms. El sábado la sesión la abre el propio plugin; el premio por ronda no depende del reloj. — [XoPlugin.java](tools/xo/src/com/preguntones/xo/XoPlugin.java) líneas 40-41, 143 y 631-633
- Cobra solo el último que sigue en `vivos`. Si no queda nadie, el anuncio es «Esta ronda no tiene ganador» y no hay depósito. El mensaje al ganador es «Ganas la XO. +N duros.» El dinero no se descuenta a los demás. — [XoPlugin.java](tools/xo/src/com/preguntones/xo/XoPlugin.java) líneas 688-772
- Con una persona el premio es 4. Con cinco en la pista al empezar, el ganador recibe 20. El cuaderno y Discord dicen «4 duros» o «cada ronda son 4 duros», sin el multiplicador. — [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 345 y 355; [mensajes.json](discord/mensajes.json) línea 14

#### Traspasos que suben el saldo de quien cobra

- Venta de parcela (`/parcela comprar` y aceptar una oferta). Precio entre 20 y 2000. Impuesto `precio * impuesto / 100` con `impuesto` del config, hoy 10, división entera. El vendedor recibe el resto. Ejemplos: 4 duros de precio dejan impuesto 0; 40 dejan 4 al barrio y 36 al vendedor. El impuesto entra en la caja. Si el abono al vendedor falla, se devuelve el precio entero al comprador. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 385-410 y 478-499; [config.yml de Parcelas](servidor/plugins/Parcelas/config.yml) líneas 11-14; [Pruebas.java](tools/parcelas/src/com/preguntones/parcelas/Pruebas.java) línea 52
- Cofre de venta. Total = unidades × precio unitario. Precio unitario de 1 a 500. El tope de una compra es 2000 duros (`COMPRA_MAX`); la cantidad máxima es `min(stock, 2000 / precio)`. Impuesto fijo `Cuentas.IMPUESTO = 10` (no lee el config): `total * 10 / 100`. El dueño, aunque esté desconectado, recibe `total - impuesto` por `banco.ingresar`. El impuesto va a la caja. Si no cabe en el inventario o el abono falla, se devuelve el total y no se parte el género. — [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 5-22 y 33-41; [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 896-928 y 1258-1262
- `/pay` está permitido al grupo `default` (`essentials.pay`). El cuaderno dice que se escribe el nombre y cuántos duros. Esta config de Essentials no tiene una tasa de `/pay`. El bloqueo de Parcelas solo cancela el texto `/pay` y `/pagar`, y solo con la deuda ya en mora. — [default.yml](servidor/plugins/LuckPerms/yaml-storage/groups/default.yml) línea 11; [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) línea 198; [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 559-573
- Cartel `trade` de Essentials, permiso `essentials.signs.create.trade` y `essentials.signs.use.trade` en `default`. El comentario de config dice que el dueño carga el cartel con lo que lleva en la mano y cobra los duros. No hay un precio fijo ni un impuesto en ese archivo. — [config.yml de Essentials](servidor/plugins/Essentials/config.yml) líneas 19-22; [default.yml](servidor/plugins/LuckPerms/yaml-storage/groups/default.yml) líneas 13-14
- Préstamo. La caja nace en 150 la primera vez (`CAJA_INICIAL`), o si el archivo existe sin `caja-lista`. Prestar resta ese capital de la caja y lo abona al jugador: no imprime duros. Un solo préstamo. Cantidad de 1 hasta la caja. Plazos: 1 día al 10 %, 3 días al 20 %, 7 días al 30 %. Interés = `max(1, (capital * porcentaje + 99) / 100)` si capital y porcentaje son positivos. Pruebas: 25 al 10 % = 3 (se deben 28); 100 al 30 % = 30; 1 al 10 % = 1. Pagar antes cuesta el total ya aceptado, de una vez. Al cobrar, el interés se destruye y solo el capital vuelve a la caja. Con capital ≤ 50 aparece el aval «Mis duros»; las parcelas propias que no estén en venta ni avaladas salen también. Si no hay ningún botón de aval, no se concede. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 38, 98-130, 247-301, 343-401 y 475-485; [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 24-30; [Pruebas.java](tools/parcelas/src/com/preguntones/parcelas/Pruebas.java) línea 55; [economia-del-barrio.md](reports/economia-del-barrio.md) líneas 29 y 97-101

Si el jugador está en mora, `ingresar` aplica el cobro a la deuda antes de dejarle el resto. Las misiones y la XO depositan directo en Vault; un reloj cada 40 ticks (2 segundos) quita el aumento de saldo mientras `mora` es verdad. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 63-65, 98-122 y 487-508

#### Devoluciones (restauran un cobro, no crean una fuente)

- Bandera y artículos de `/puesto`: si no cabe el objeto, `devolver` el precio cobrado. La bandera cuesta 40, cifra fija, fuera del catálogo. — [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 77-87 y 135-145; [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 11-12
- Cofre y parcela: devolución del total si el pago al vendedor falla o el inventario no admite el género. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 904-925; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 391-393 y 485-487
- Si `withdrawPlayer` dice éxito pero el saldo no ha bajado al menos el precio menos 0,5, se reingresa el precio y el cobro se da por fallido. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 573-585 y 604-619

#### Cifras que un pase de balance necesita y que no crean saldo

- Reclamar parcela libre destruye duros: no entran en la caja. Precio con `llevas` parcelas ya suyas, `libre = 25`: se suma `30 + 10 * i` para `i` de 0 a `llevas - 1`. Resultado, y el cuaderno lo copia: 1ª 25, 2ª 55, 3ª 95, 4ª 145, 5ª 205, 6ª 275. Suma de las seis: 800. Tope `max-por-jugador: 6`. `topeDe` devuelve ese máximo; no lee `clan.parcelas-por-persona` (está en el config y Clanes no lo usa; Clanes solo lee `clan.max-personas`, defecto 8). Abandonar no devuelve duros. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 267-302, 503-515, 553-559 y 725-727; [config.yml de Parcelas](servidor/plugins/Parcelas/config.yml) líneas 9-17; [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 126 y 138; [Clanes.java](tools/parcelas/src/com/preguntones/parcelas/Clanes.java) línea 25
- `/puesto` multiplica por `ESCALA = 4` el argumento que llega a `add`. `paso` redondea hacia arriba a par, mínimo 2, y eso ocurre antes de la escala. El comentario del catálogo, ya con la escala, dice: 32 troncos de roble son 96 duros (base 24 × 4); sus tablones salen a 24; la madera de corteza sale a 128. Bases de tronco antes de escalar: roble, abeto, abedul, selva, acacia y roble oscuro 24; manglar y cerezo 28; roble pálido 32; bambú 80; carmesí y distorsionado 36. Las luces llaman a `add` directo: 32 antorchas llevan argumento 6 y quedan en 24 duros. La lista completa sale de `Catalogo.java`. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 7-15, 71-118, 530-556 y 578-611
- `min-money: 0` y `max-money: 2000`. El saldo que enseña Parcelas es el entero por debajo. — [config.yml de Essentials](servidor/plugins/Essentials/config.yml) líneas 10-11; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 566-570
- Al ejecutar el aval de parcela, `soltarAval` la deja libre y no ingresa nada en la caja. El siguiente reclamante paga el precio de pala normal, y ese cobro se destruye. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 455-461 y 704-714
- `/banco condonar`, para op o `parcelas.admin`, borra la deuda. El mensaje dice que la caja no recupera ese préstamo. No deposita duros. `staff` tiene `parcelas.admin`. `mod` lo tiene en false. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 68-75 y 403-430; [staff.yml](servidor/plugins/LuckPerms/yaml-storage/groups/staff.yml) línea 30; [mod.yml](servidor/plugins/LuckPerms/yaml-storage/groups/mod.yml) líneas 166-167

#### Fuentes pedidas que no están

- Voto: no hay plugin ni importe. Discord no premia estar en el servidor de voz ni un boost: `recompensa_por_estar_en_discord: false`, `recompensa_por_boost: false`. — [estructura.json](discord/estructura.json) líneas 39-40
- Jobs, quests, paga por tiempo conectado, interés a favor del ahorrador: no hay plugin ni fórmula en el código buscado.
- Tebex no da duros. El grupo `vecino` (VIP) solo cambia nombre, estela y mensaje. — [vecino.yml](servidor/plugins/LuckPerms/yaml-storage/groups/vecino.yml) líneas 1-16; [paquetes.json](tienda/paquetes.json) líneas 31 y 37

Quien puede crear saldo a mano: el grupo `admin` tiene el permiso `*`, así que incluye `essentials.eco.give`, `set`, `take` y `reset`. El grupo `mod` los tiene explícitamente a false. `default`, `staff`, `cazador` y `vecino` no los listan; heredan `balance` y `pay` de `default`. No hay un comando propio del estilo `/duros dar`. — [admin.yml](servidor/plugins/LuckPerms/yaml-storage/groups/admin.yml) líneas 4-7; [mod.yml](servidor/plugins/LuckPerms/yaml-storage/groups/mod.yml) líneas 20-27; [default.yml](servidor/plugins/LuckPerms/yaml-storage/groups/default.yml) líneas 5-11

### Inferences

- La impresora de duros hacia los jugadores, aparte del admin con `*` y de los 25 iniciales, es Mundos más la XO. El banco y las ventas redistribuyen.
- Comprar las seis parcelas en orden cuesta 800. La alta cubre justo la primera (25) y deja el saldo a 0 si no ha entrado otra fuente. El máximo de misiones de un ciclo (340 con dragón) no llega a las otras cinco (775).
- El impuesto del cofre está clavado en 10 aunque alguien cambie `impuesto-porcentaje`. El de la parcela sí sigue el config. Hoy los dos valen 10.

### Gaps

- No hay un contador en el código de cuántas rondas de XO caben en la hora. El premio por ronda sí está fijado; el premio de una noche depende de cuánta gente sigue en pie y de cuántas rondas da tiempo.
- No se ha leído el jar, así que la fórmula interna del cartel `[Trade]` (formato de líneas, si admite decimales, si cobra al dueño offline) no está en este repo. Solo está el comentario de config y el permiso.
- `ParcelasPlugin.contar` escribe «Tienes N duros», pero no hay ninguna llamada a ese método en el Java del repo. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 740-754
- El comportamiento exacto de Essentials al chocar con 2000 no está en el código del proyecto.

## Comandos, permisos y mensajes

### Takeaway

Consultar el saldo: `/balance` (permiso `essentials.balance`), el scoreboard `Duros: %preguntones_duros%`, el botón del banco y el objeto «Tus duros» de la tienda. Pasar duros: `/pay`. Concederlos a mano: solo quien tenga el comodín de `admin`, por los nodos `essentials.eco.*`. La mora bloquea `/pay` y `/pagar`.

### Cited Findings

- `default` (y quien lo hereda: staff, mod, admin, cazador, vecino): `essentials.balance`, `essentials.pay`, `essentials.kit`, `essentials.kits.inicio`, `essentials.signs.create.trade`, `essentials.signs.use.trade`, `parcelas.usar`. — [default.yml](servidor/plugins/LuckPerms/yaml-storage/groups/default.yml) líneas 5-16; [cazador.yml](servidor/plugins/LuckPerms/yaml-storage/groups/cazador.yml) líneas 3-4
- `admin`: `*` y padre `default`. — [admin.yml](servidor/plugins/LuckPerms/yaml-storage/groups/admin.yml) líneas 4-7
- `mod`: `essentials.eco.give`, `reset`, `set` y `take` en false. También `parcelas.admin` y `xo.admin` en false. — [mod.yml](servidor/plugins/LuckPerms/yaml-storage/groups/mod.yml) líneas 20-27, 166-170
- Comandos de Parcelas, permiso `parcelas.usar` por defecto: `/parcela`, `/puesto`, `/tienda recuperar`, `/banco`, `/bandera` (40 duros), `/clan`. `parcelas.admin` es de op y no da dinero; sirve para el spawn y para `/banco condonar <nombre>`. — [plugin.yml de Parcelas](tools/parcelas/plugin.yml) líneas 8-39; [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 68-75
- `/parcela` sin argumentos no muestra el saldo. Informa de la parcela, el spawn o el camino. El precio de la siguiente parcela sale al pisar una libre o al reclamar. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 178-180 y 210-257
- Mundos: `/ciclo` y `/grupo` con `mundos.usar` (defecto true). `/ciclo reset` exige `mundos.admin` (defecto op) y no paga. — [plugin.yml de Mundos](tools/mundos/plugin.yml) líneas 7-20
- Xo: `/xo empezar` y `/xo parar`, permiso `xo.admin` (defecto op). Abrir sesión no da duros; los da ganar la ronda. — [plugin.yml de Xo](tools/xo/plugin.yml) líneas 8-15
- Mensajes de cobro de misión: «§a» + texto + « +» + cantidad + « duros.» Nether, End y dragón como arriba. XO: «Ganas la XO. +N duros.» o «Has ganado la XO, pero no he podido darte los duros.» — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 451, 783 y 797; [XoPlugin.java](tools/xo/src/com/preguntones/xo/XoPlugin.java) líneas 768-771
- Banco, diálogo: «Tienes N duros. La caja del barrio tiene M.» Botones Pedir y Pagar. Plazos escritos como «1 día · T duros» con el interés en la ayuda. Al conceder: «El banco te deja C duros.» En mora, al usar `/pay`: «Debes al banco. No puedes pasar duros hasta saldar.» — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 136-143, 263-268, 380 y 570-572
- Tienda: lore «Tus duros» / «N duros». Falta de saldo: «Te faltan duros. Cuesta P. Tienes N.» — [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 79 y 408-409
- Scoreboard siempre visible, título Preguntones, línea `Duros: %preguntones_duros%`. El comando de TAB para ocultarlo es `/sb`. El placeholder de parcela no es el saldo: devuelve el precio de la siguiente parcela o el texto `llena`. — [config.yml de TAB](servidor/plugins/TAB/config.yml) líneas 11-23; [DurosExpansion.java](tools/parcelas/src/com/preguntones/parcelas/DurosExpansion.java) líneas 38-43; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 729-737
- El panel tiene botón Banco, ayuda «Pedir duros. Se devuelven con intereses.» — [MenuPantalla.java](tools/viajes/src/com/preguntones/viajes/MenuPantalla.java) línea 65

### Inferences

- Un jugador sin rangos de pago ve el saldo en el scoreboard y lo mueve con `/pay`, la pala, el cofre y el cartel `trade`. No tiene un comando para fabricar duros.
- `staff` puede condonar una deuda y no puede usar `/eco give` por ningún nodo listado.

### Gaps

- Los alias de Essentials (`/bal`, `/money`, `/eco`) no están redefinidos en el `config.yml` local, que acaba en `disabled-commands` sin incluir los de economía. Que el jar de la 2.22.0 registre esos alias no se ha comprobado abriendo el jar.
- El bloqueo de mora compara el comando ya escrito. No aparece en el código un alias distinto de `/pay` y `/pagar`.

## Cómo se guarda el saldo

### Takeaway

El saldo del jugador es el de EssentialsX 2.22.0, leído y escrito por Vault. No hay tabla propia ni archivo de saldos en Parcelas. La caja y las deudas sí son un YAML de Parcelas; el «ya te pagué esta misión» es un YAML de Mundos.

### Cited Findings

- Cabecera del config: «La moneda la registra este plugin y Vault la expone. Un duro. No hay otra moneda.» Enteros de juego encima, con suelo 0 y techo 2000. Símbolo ` duro` como sufijo. — [config.yml de Essentials](servidor/plugins/Essentials/config.yml) líneas 1-13
- Jars presentes: `EssentialsX-2.22.0.jar`, `Vault.jar`. Parcelas no arranca si Vault no tiene economía. — listado de `servidor/plugins`; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 93-98
- Lectura: `(int) Math.floor(economia.getBalance(jugador))`. Escritura de juego: `depositPlayer` / `withdrawPlayer` con importes enteros pasados como double. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 566-619
- Caja y préstamos: `plugins/<datos de Parcelas>/banco.yml`. Campos de préstamo: `capital`, `interes`, `restante`, `vence`, `mora`, `parcela`. Si no existe el archivo, caja = 150 y se guarda. Si existe y `caja-lista` es falso, también se pone 150. Si `caja-lista` es verdadero, se respeta el entero guardado, aunque sea 0. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 38, 57-59 y 628-670
- Hechos de misión: `plugins/<datos de Mundos>/estado.yml`, ruta `hechos.<clave>.<uuid> = veces`. Se borra entero al renovar el ciclo. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 98, 307-308 y 622-650
- `parcelas.yml` guarda dueños y precios de parcela, no saldos. En la copia local están `config.yml` y `parcelas.yml`, y no `banco.yml`. — [config.yml de Parcelas](servidor/plugins/Parcelas/config.yml)
- Karma lee `Essentials/userdata/<uuid>.yml` solo para `timestamps.logout`. En esta copia no hay ningún yml de userdata, así que no se ha visto la clave del dinero. — [KarmaPlugin.java](tools/karma/src/com/preguntones/karma/KarmaPlugin.java) líneas 610-616

### Inferences

- El techo de 2000 es de cuenta, no de cada pago. Una venta, una misión o un `/pay` que no quepa bajo ese techo depende de si Essentials acepta el depósito.
- Reiniciar el servidor no repone la caja: el 150 solo entra si falta el archivo o falta la bandera `caja-lista`.

### Gaps

- Nombre exacto del campo de saldo dentro de `userdata/<uuid>.yml`: no observado en este checkout.
- Caja y préstamos del servidor en marcha: archivo ausente aquí. No se puede citar el saldo actual de la caja.
- No hay base de datos de economía (ni SQLite ni MySQL) en los plugins revisados.

## Qué dicen los informes y no está en el código

### Takeaway

`reports/economia-del-barrio.md` describe el cofre, el banco, el 10 %, la caja de 150 y los tres plazos, y eso está implementado con las mismas cifras. Dos flujos de ese informe no coinciden con el código: el precio de la parcela ejecutada no entra en la caja, y un `/pay` con la deuda en mora se cancela en vez de saldarla. `reports/Diseño mercado servidor Preguntones.md` deja fuera del juego, y sin importe propio, el voto, la tarea corta de la semana, los trabajos y la subasta; además ese informe dice que la cita del sábado no inyecta duros, y la XO implementada sí paga.

### Cited Findings

- Informe del barrio, ya en código con el mismo número: caja inicial 150; impuesto 10 % hacia abajo (4 duros → 0, 40 → 4 y el dueño 36); tope de compra 2000; plazos 10 % / 20 % / 30 %; interés hacia arriba y mínimo 1; ejemplo 25 al 10 % = 3, se deben 28; aval en duros hasta 50 de capital; un solo préstamo; el interés no vuelve a la caja. — [economia-del-barrio.md](reports/economia-del-barrio.md) líneas 25-31, 60 y 97-111; código en [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) y [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) como en la sección anterior
- El mismo informe dice que, si el banco se queda la parcela, «el siguiente la reclama con la pala, al precio normal, y ese precio entra en la caja». En el código el reclamante paga y el dinero no se ingresa en la caja. — [economia-del-barrio.md](reports/economia-del-barrio.md) línea 112; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 287-302 y 704-714
- El informe dice que, mientras queda deuda tras el vencimiento, lo que entre (venta, XO, alguien que intente pagarte) salda antes de entrar al saldo. La venta pasa por `ingresar` y sí salda antes. La XO entra al saldo y el reloj de mora la retira después. `/pay` y `/pagar` se cancelan: ese pago no llega a la deuda. — [economia-del-barrio.md](reports/economia-del-barrio.md) líneas 111 y 149; [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 98-122, 487-508 y 570-572
- Informe de mercado: se ganaría con un cartel de Essentials, «la tarea corta de la semana» y, cuando haya ficha en una lista, un voto «por debajo de una tarde de juego». No da la cifra de la tarea ni la del voto. El cartel `trade` sí está activado. La tarea semanal y el voto no están en plugins ni en código. El informe dice que VotifierPlus y VotingPlugin entran con la primera ficha, no antes. — [Diseño mercado servidor Preguntones.md](reports/Diseño mercado servidor Preguntones.md) líneas 23, 27 y 48
- El mismo informe aparta para más adelante el plugin de trabajos y la casa de subastas. Atribuye a Towny Reborn, no a un config de Preguntones, el impuesto del 5 % y la caducidad a las 48 horas. Esos números no están en el código del servidor. — [Diseño mercado servidor Preguntones.md](reports/Diseño mercado servidor Preguntones.md) línea 23
- El informe rechaza la paga por tiempo online, la recompra administrativa y que el euro fabrique moneda. En el código no hay paga por tiempo. Los carteles `buy` y `sell` están apagados. Tebex no da duros. `worth.yml` sigue siendo la plantilla de serie y no hay permiso de `/sell` en los grupos de jugador. — [Diseño mercado servidor Preguntones.md](reports/Diseño mercado servidor Preguntones.md) líneas 21-25; [config.yml de Essentials](servidor/plugins/Essentials/config.yml) líneas 19-22
- El informe dice que la cita del sábado premia con un título o un aspecto, no con duros. La XO implementada paga `4 * personas` al ganador de la ronda. — [Diseño mercado servidor Preguntones.md](reports/Diseño mercado servidor Preguntones.md) línea 13; [Bote.java](tools/xo/src/com/preguntones/xo/Bote.java) líneas 3-12
- El informe de mercado no fija los 25 de alta, ni la tabla de misiones, ni el 20 del dragón, ni la escalera 25/55/95/145/205/275. Esas cifras están en config, en Mundos y en el cuaderno del juego.

### Inferences

- Para un pase de balance, las cifras vigentes son las del código y de `servidor/plugins`, no las del informe de mercado. El 5 % y las 48 horas de ese informe son una cita ajena, no una tarifa del servidor.
- El informe del barrio se puede usar como descripción del cofre y del banco, con las dos excepciones de la parcela ejecutada y del `/pay` en mora.

### Gaps

- El informe de mercado no deja un número que sustituir para el voto ni para la tarea semanal: dice «pequeña» y «por debajo de una tarde de juego», sin duros. No hay forma de citar un importe que el código haya omitido, porque el informe tampoco lo fija.
- No se ha comparado este checkout con el panel de GamzyHost. Si el servidor en marcha tiene otro `config.yml` de Essentials o de Parcelas, las cifras de esta nota son las de la copia local.
