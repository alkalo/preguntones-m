# Ingresos de duros en Preguntones

Cifras leídas del código y de la copia local del servidor el 13 de noviembre de 2026. No se ha cambiado código, configuración ni mundo. Los informes anteriores no se usan como cifra: donde chocan con el código, manda el código.

La moneda la guarda EssentialsX 2.22.0 y la expone Vault. No hay otro plugin de economía en `servidor/plugins`. No hay Jobs, Votifier, VotingPlugin ni un plugin de quests. Los jar presentes son AuthMe, Censura, Chunky, CoreProtect, DiscordSRV, EssentialsX y sus módulos Chat, Protect y Spawn, GriefPrevention, GrimAC, Impulso, Karma, LibertyBans, LuckPerms, Mundos, packetevents, Parcelas, PlaceholderAPI, Recuperar, Reinicio, SkinsRestorer, Susto, TAB, Tebex, Vault, Viajes, Voces y Xo.

## Qué plugins y clases ingresan duros

### Takeaway

Solo tres clases crean duros para quien juega: `MundosPlugin` (misiones, Nether, End y dragón), `XoPlugin` (la ronda del sábado) y Essentials (`starting-balance: 25`, más `/eco` para el admin). Parcelas mueve duros que ya existen, o los saca de la caja del barrio. El banco no paga interés a nadie.

### Cited Findings

- `MundosPlugin.depositPlayer` está en tres sitios: 20 al cazar el dragón, `mision.duros * nuevas` al cumplir una misión, y 16 o 18 al entrar por primera vez al Nether o al End. — [MundosPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)
- `XoPlugin.premiar` deposita `Bote.premio(bote)` al ganador de la ronda. `Bote` no retira duros a nadie: el premio se fabrica. — [XoPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/XoPlugin.java); [Bote.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/Bote.java)
- Essentials arranca la cuenta en 25, con suelo 0 y techo 2000. El símbolo es ` duro`, detrás del número. El kit `inicio` da pan, pico de madera, pala de oro y 16 antorchas: ningún duro. — [config.yml de Essentials](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/config.yml); [kits.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/kits.yml)
- `ParcelasPlugin.abonar` es el único depósito genérico del barrio. Lo usan el banco (préstamo y recompra) y, a través de `Banco.ingresar`, el vendedor de una parcela o de un cofre. Esos duros salen del comprador o de la caja, no de la nada. `devolver` y los `depositPlayer` de los fallos devuelven un cobro que no se quedó. — [ParcelasPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java); [Banco.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Banco.java); [Venta.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Venta.java)
- `KarmaPlugin` solo retira. El `depositPlayer` de esa clase deshace un cobro que no cuadró. La cantidad configurada es `cruel-duros: 20`. — [KarmaPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/karma/src/com/preguntones/karma/KarmaPlugin.java); [config.yml de Karma](file:///C:/Users/Ale/Documents/preguntones-m/tools/karma/config.yml)
- Viajes, Recuperar, Reinicio, Susto, Impulso, Censura y Voces no llaman a la economía. El `PlayerJoinEvent` de Viajes entrega el cuaderno y un aviso de Escape. El de Xo solo revisa el modo de juego. El de Mundos ancla la caza de blazes y, si tocaba, da el rango Cazador: no da los 20 duros. — [ViajesPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/ViajesPlugin.java); [XoPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/XoPlugin.java); [MundosPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)
- Tebex, en la nota local del paquete Vecino, añade el grupo de LuckPerms y dice que no vende duros. La clave de la tienda no está puesta. DiscordSRV no tiene una alerta que ejecute un premio. AuthMe no configura un premio en dinero. — [comandos.txt de Tebex](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Tebex/comandos.txt)
- El grupo `admin` tiene el permiso `*`, así que incluye `essentials.eco.give`, `set`, `take` y `reset`, y también `essentials.sell`. `mod` tiene esos cuatro nodos `eco` en false. `default` tiene `essentials.balance` y `essentials.pay`, y no tiene `essentials.sell` ni `essentials.worth`. `staff`, `cazador` y `vecino` heredan `default` en eso. `staff` sí tiene `parcelas.admin`. — [admin.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/LuckPerms/yaml-storage/groups/admin.yml); [mod.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/LuckPerms/yaml-storage/groups/mod.yml); [default.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/LuckPerms/yaml-storage/groups/default.yml); [staff.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/LuckPerms/yaml-storage/groups/staff.yml)

### Inferences

- El grifo de duros nuevos, para quien no es admin, es Essentials al crear la cuenta, Mundos mientras juega en el survival, y la XO cuando gana una ronda.
- Todo lo demás o cambia de manos o vuelve atrás un cobro. No es un ingreso nuevo.

### Gaps

- No se ha abierto el jar de EssentialsX. No está verificado si `starting-balance` se aplica solo al crear la ficha, ni si un depósito que pasaría de 2000 falla entero o entra hasta el tope. El archivo de configuración no define una tasa de `/pay`.
- La copia local de `servidor/` no se ha vuelto a bajar del panel en esta pasada. Las cifras de `estado.yml`, `banco.yml` y los yml de permisos son las que están en el disco hoy.

## Cuánto, a quién, si se repite, y en qué línea

### Takeaway

Cada fuente de abajo dice el duro exacto, quién lo recibe y si vuelve a pagarse. Las devoluciones y los traspasos van al final, aparte, porque no fabrican saldo.

### Cited Findings

**Al entrar.** 25 duros, una vez, por la clave `starting-balance: 25`. Ningún plugin Java deposita 25 en el join. El cuaderno dice «Traes 25 duros. Justo la primera.» y el texto de Discord dice «Entras con 25.» — [config.yml de Essentials](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/config.yml); [Cuaderno.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/viajes/src/com/preguntones/viajes/Cuaderno.java); [mensajes.json](file:///C:/Users/Ale/Documents/preguntones-m/discord/mensajes.json)

**Misiones de Mundos.** Cada 5 segundos (tarea cada 100 ticks) mira a quien está en `survival`, `survival_nether` o `fin`. El progreso es la estadística de ahora menos una foto en memoria. La foto no se guarda: un reinicio del servidor la vuelve a tomar y el avance no cobrado se pierde. Lo ya cobrado sí se guarda, en `hechos`, y no se vuelve a pagar hasta que el ciclo borra ese mapa. Si el depósito falla, no marca la misión y lo reintenta. En un mismo tick puede pagar varios tramos de golpe (`duros * nuevas`), sin pasar de `veces`. — [MundosPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)

Tabla de `misiones()`, líneas 738-754. El máximo es `veces * duros` por persona y por ciclo. El trabajo es `meta * veces`.

- Madera: 6 duros cada 16 troncos, 3 veces, máximo 18. Cuentan roble, abeto, abedul, jungla, acacia, roble oscuro, manglar, cerezo y los tallos carmesí y distorsionado. Trabajo: 48 troncos.
- Piedra: 8 cada 32, 4 veces, máximo 32. Cuentan piedra, roca, pizarra y pizarra rocosa. Trabajo: 128 bloques.
- Caza: 10 cada 4 monstruos, 3 veces, máximo 30. Zombi, esqueleto, araña, araña de cueva, creeper, ahogado, husk, stray, bruja, saqueador, vindicador y phantom. Trabajo: 12 muertes.
- Comida: 6 cada 3, 2 veces, máximo 12. Vaca, cerdo, gallina, oveja y conejo. Trabajo: 6 muertes.
- Pesca: 8 cada 6 peces, 2 veces, máximo 16. Trabajo: 12 peces.
- Camino: 8 cada 40.000 cm andados o esprintados (400 bloques), 3 veces, máximo 24. Trabajo: 1.200 bloques.
- Cama: 8 por dormir una vez. Una sola vez.
- Aldeano: 10 por un comercio. Una sola vez.
- Hierro: 12 cada 8 menas de hierro o de pizarra, 2 veces, máximo 24. Trabajo: 16 menas.
- Diamante: 16 cada 2 menas, 2 veces, máximo 32. Trabajo: 4 menas.
- Encantar: 12 por un encantamiento. Una sola vez.
- Blaze: 14 cada 2 blazes, 2 veces, máximo 28. Trabajo: 4 blazes. Es independiente de los 12 blazes de la puerta.
- Escombro: 20 por un escombro ancestral. Una sola vez.
- Enderman: 10 cada 3, 2 veces, máximo 20. Trabajo: 6 endermen.

Suma de esos topes: 286 duros por persona y ciclo.

**Nether y End, una vez por ciclo.** 16 al estar en `survival_nether`. 18 al estar en `fin`. Misma lista `hechos`, así que se borran con el ciclo. No piden matar nada: basta estar en ese mundo cuando corre la revisión. — [MundosPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)

**Dragón.** 20 duros a cada miembro del grupo que esté conectado y tenga la puerta abierta. El grupo cabe a 4, así que una caza fabrica 20, 40, 60 u 80 según cuántos estén dentro en ese momento. Quien está en el grupo pero desconectado recibe el rango Cazador al volver, y no los 20. Si el depósito falla, la caza igual queda gastada: no hay reintento. La segunda muerte del dragón no paga mientras `cazador` siga puesto. — [MundosPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java); [Grupo.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/Grupo.java)

**Cuándo vuelve a pagarse el ciclo.** `reiniciarCiclo` borra `hechos`, la foto de las misiones y al cazador. Eso ocurre 48 horas después del dragón (`aviso-horas` por defecto 48; el yml de Mundos no lo cambia) o con `/ciclo reset`, permiso `mundos.admin`, que por defecto es de op. El día 14 solo ata el primer ciclo: `Ciclo.pideDias` es verdad si el ciclo es 1 o menos. La puerta pide además 12 blazes de ese ciclo. El ciclo 2 pide bastión y ciudad ancestral; desde el 3 se turnan. — [MundosPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java); [config.yml de Mundos](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/config.yml); [Ciclo.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/Ciclo.java)

**Estado local del ciclo.** `estado.yml` tiene `ultimo-reset: 1794438148850` (11 de noviembre de 2026, 23:02 UTC, medianoche del 12 en Madrid), `cazador` vacío y no trae la clave `ciclo`. El plugin, si falta la clave, carga el ciclo 1. Catorce días después el End se abre el 25 de noviembre a las 23:02 UTC (00:02 del 26 en Madrid). El archivo no tiene `hechos`: en esta copia nadie tiene una misión marcada como cobrada. — [estado.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Mundos/estado.yml); [MundosPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)

**XO.** 4 duros por persona que estaba viva al empezar la primera frase de la ronda, todos para quien queda. Mínimo del cálculo: 4 si el bote llega a 0 o 1, pero la ronda no arranca con menos de 2 personas en la entrada («Hacen falta dos personas. Esta ronda se salta.»). Con 2 personas el ganador recibe 8; con 4, 16; con N, 4N. Nadie paga entrada. El bote se fija en la primera frase y no se actualiza si alguien entra después. Si todos fallan, no hay ganador y no hay depósito. Una ronda puede tener varias frases de 15 segundos; el dinero se paga una vez, al quedar uno. — [Bote.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/Bote.java); [XoPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/XoPlugin.java); [Pruebas.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/Pruebas.java)

**Calendario de la XO.** Sábado, hora de Madrid, de 21:30 a 22:30. Huecos a las 21:35, 21:45, 21:55, 22:05, 22:15 y 22:25: seis rondas. `/xo empezar` (permiso `xo.admin`, por defecto op) abre otra hora, con huecos en los minutos 5, 15, 25, 35, 45 y 55: otras seis rondas. El texto de Discord dice «Quien queda se lleva 4 duros» y «Cada ronda son 4 duros». El código paga 4 por cabeza, no 4 fijos. — [Cita.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/Cita.java); [plugin.yml de Xo](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/plugin.yml); [mensajes.json](file:///C:/Users/Ale/Documents/preguntones-m/discord/mensajes.json)

**Admin.** `/eco give`, `set`, `take` y `reset` llegan con el comodín de `admin`. El techo de saldo es 2000. No hay un comando propio `/duros dar`. `mod` no puede. `/ciclo reset` no da duros: borra el tope de las misiones y deja cobrarlas otra vez. `/xo empezar` abre otra tanda de premios fabricados. — [admin.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/LuckPerms/yaml-storage/groups/admin.yml); [config.yml de Essentials](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/config.yml)

**`/sell` y worth.yml.** Los carteles `buy` y `sell` están apagados. El comentario del config dice que uno fabrica objetos y el otro fabrica duros. `worth.yml` sigue siendo el ejemplo de serie (tronco a 2, pico de diamante a 650, y el resto de la lista de ejemplo). Quien tiene `essentials.sell` puede vender a esos precios. En los grupos, ese nodo solo entra por el `*` de `admin`. Un jugador del barrio no lo tiene. — [config.yml de Essentials](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/config.yml); [worth.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/worth.yml)

**Traspasos, no fabricación.**

- Parcela entre jugadores: el comprador paga el precio entero (entre 20 y 2000 en el config del servidor). El vendedor recibe el precio menos el 10 % entero. El 10 % entra en la caja. Si el ingreso al vendedor falla, al comprador se le devuelve todo. Lo mismo en la oferta directa y en la compra del cartel. — [ParcelasPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java); [config.yml de Parcelas](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Parcelas/config.yml); [Cuentas.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Cuentas.java)
- Cofre de venta: igual, 10 % a la caja. El precio por unidad lo pone el jugador, de 1 a 500. Una compra no pasa de 2000 duros. — [Venta.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Venta.java); [Cuentas.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Cuentas.java)
- `/pay`: permitido a `default`. El config local no pone una tasa. Es un traspaso.
- Cartel `trade`: permitido crear y usar a `default`. El dueño cobra lo que haya escrito en el cartel. No hay un precio fijo en el código propio.
- Préstamo: la caja suelta el capital pedido, como máximo lo que haya en la caja, y el jugador queda debiendo capital más interés. El interés de `Cuentas.interes` se suma a lo que devuelve. No hay una cuenta de ahorro ni un interés que el banco pague. Condonar (`/banco condonar`, op o `parcelas.admin`) perdona la deuda: no deposita un importe nuevo; el capital ya entregado se queda. La copia local de la caja marca 84 duros, con un préstamo ya fuera. — [Banco.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Banco.java); [Cuentas.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Cuentas.java); [banco.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Parcelas/banco.yml)
- Recompra: la caja paga la quinta parte, hacia abajo, de lo que costó la parcela, y al menos 1 si hubo pago. Una primera parcela de 25 devuelve 5. Sale de la caja. Si no llega, la venta no se hace. — [Cuentas.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Cuentas.java); [ParcelasPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java)
- Devoluciones que no son ingreso: `devolver`, el reintegro si `cobrar` no cuadra, el reintegro si el vendedor no cobra, y el reintegro de Karma si la retirada cruel no cuadra. Abandonar la parcela no devuelve duros.

**Votos, entrada y kits.** No hay plugin de votos. El join no da duros aparte del saldo inicial de Essentials. El kit no da duros.

### Inferences

- El tope de duros fabricados por jugar, por persona y por ciclo, es 286 de misiones + 16 del Nether + 18 del End + 20 del dragón = 340, si está conectado en la caza y entra a los dos mundos. Un grupo de 4 fabrica 80 en el dragón, no 20 a repartir.
- Esta semana el End y el dragón no están en ese tope: el ciclo local es el 1 y la puerta de días no abre hasta el 26 de noviembre. El tope jugable hasta entonces es 286 + 16 = 302, más la XO del sábado.
- La XO del sábado, con la sala llena de verdad, es el único premio de jugador que crece sin un tope escrito. El único freno de código es el techo de 2000 del saldo, y hace falta otra persona para que la ronda exista.

### Gaps

- No se ha comprobado en el jar qué hace Vault/Essentials cuando el depósito cruzaría 2000. Los llamadores de misiones y de la XO, si `transactionSuccess()` es falso, no dan el dinero por cobrado. El dragón sí da la caza por hecha aunque el depósito falle.
- `worth.yml` no se ha recorrido entero. La cita del pico a 650 basta para ver que es la lista de ejemplo, no una tienda del barrio.

## Cuánto puede sacar un jugador nuevo y uno que ya juega

### Takeaway

En el ciclo que hay ahora, una persona que haga todo lo que la puerta deja abierto saca 302 duros de misiones y Nether, más 25 al entrar, más lo que gane la XO del sábado. El End y el dragón no pagan hasta el 26 de noviembre. Las misiones no se cobran dos veces en la misma semana: el mundo no se borra hasta 48 horas después del dragón.

### Cited Findings

- Primera parcela: `precios.libre` es 25 en el config del servidor y en el defecto del código. La siguiente suma 30, y luego 10 más por cada parcela que ya tenga: con una ya comprada, la segunda cuesta 55. Tope 6 parcelas. — [config.yml de Parcelas](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Parcelas/config.yml); [ParcelasPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java)
- Tienda, precios ya con la escala de 4. El comentario de `Catalogo` dice que 32 troncos de roble son 96 duros. Antorchas: 32 por 24. Roca: 32 por 64. Faroles: 4 por 64. Mesa de trabajo: 1 por 16. La ficha de aldeano y la bandera de viaje están fuera del catálogo, a 40 cada una. — [Catalogo.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Catalogo.java); [Aldeanos.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Aldeanos.java); [Tienda.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Tienda.java)
- Reconstrucción de `Catalogo.filas()` con la misma escala, `paso` y los helper de madera, obra y colores: 891 filas y 58.136 duros si se compra una vez cada fila. La fila más cara sale a 216 (escalera de cuarzo liso o de púrpur: el precio de receta 36, sobre 16 bloques, pasa a 54 y la escala lo deja en 216). La más barata del catálogo sale a 16. Una fila cuyo material no exista en el servidor se salta y no entra en la tienda. — [Catalogo.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Catalogo.java)
- Tope de una sentada, esta semana, si la persona hace cada acción que el código paga y el End sigue cerrado: 25 de entrada + 286 de misiones + 16 del Nether = 327. El End (18) y el dragón (20) no entran hasta el 26 de noviembre, y el dragón solo si además lleva 12 blazes de este ciclo. Una sentada no puede cobrar la misma misión dos veces.
- Tarde corta, solo lo que no pide fortaleza, aldea, mesa de encantamientos ni mena de diamante: madera 18, piedra 32, caza 30, comida 12, camino 24, cama 8, hierro 24. Suma 148, más los 25 de entrada = 173. Con la pesca (16) son 189 contando la entrada.
- Tarde larga de superficie, sumando pesca 16, aldeano 10, encantar 12, diamante 32 y enderman 20: 238 de misiones + 25 = 263, todavía sin Nether.
- Nether en la misma semana, si pica un escombro y mata 4 blazes: +16 +28 +20 = +64. Con la superficie completa: 302 + 25 = 327.
- Semana del 13 al 19 de noviembre: el mismo tope de misiones, porque el ciclo no se reinicia. La XO del sábado 14 añade, solo a quien gana cada ronda, 4 por persona que hubiera en la pista. Seis rondas. Con 2 personas y el mismo ganador en todas: 48. Con 4 personas: 96. Quien no gana saca 0 de la XO. Una persona sola no cobra: la ronda se salta.
- A partir del 26 de noviembre, si cazan el dragón y están los cuatro conectados, cada uno suma 18 del End + 20 del dragón sobre los 302. El mundo se borra 48 horas después y las misiones vuelven a cero. Desde el ciclo 2 ya no hay espera de 14 días: el tope de 340 puede repetirse cada vez que maten al dragón y pasen esas 48 horas, si cumplen blazes y la excursión de ese ciclo (bastión, ciudad, o las dos en el ciclo 2).
- Quien ya cobró las misiones en este ciclo saca 0 de Mundos hasta el borrado. En la copia local de `estado.yml` no hay `hechos`, así que ese «ya cobrado» todavía no está escrito.
- Un admin puede poner el saldo en cualquier punto hasta 2000, las veces que quiera, y puede reabrir las misiones con `/ciclo reset` o abrir otra hora de XO. Eso no es lo que el código deja hacer a un jugador.

### Inferences

- Jugador nuevo, una tarde de superficie sin Nether: 173 duros con la entrada, o 263 si además pesca, comercia, encanta, pica 4 diamantes y mata 6 endermen. Con eso paga la parcela de 25 y le queda tienda, pero la tarde corta (173) no llega a parcela + 32 antorchas (24) + 32 troncos (96) + 32 roca (64) = 209.
- La misma persona, la semana entera, sin ser admin y sin que otro le pague: como mucho 327 más la XO. Con un compañero y ganando las seis rondas, 375. Con cuatro en la pista y ganando las seis, 423. Sigue lejos de las 58.136 del catálogo y por debajo del techo de 2000.
- Jugador que ya liquidó las misiones: en lo que queda de ciclo solo fabrica duros si gana la XO. Lo que cobre por una parcela o un cofre es dinero de otro jugador. El 10 % de esa venta no se destruye: pasa a la caja.

### Gaps

- «Tarde corta» y «tarde larga» parten el trabajo según lo que pide cada misión (una cama frente a un escombro). El código no mide horas. Quien pique el escombro en la primera hora cobra los 20 igual.
- No se han leído los saldos de `Essentials/userdata`. No hay una cifra de lo que la gente tiene ahora mismo.

## Qué pagos se saltan el circuito de jugar y comprar

### Takeaway

El circuito que el propio servidor describe es entrar, jugar el survival, y venderle a otro. Tres pagos lo saltan: los 25 de entrada, que son la parcela entera; la XO, que fabrica 4 duros por cabeza sin cobrar a nadie y sin tope de sala; y la mano del admin (`/eco` y, si vende, `worth.yml`). Las misiones sí tienen tope. El banco no regala interés.

### Cited Findings

- Discord: «Los duros salen de jugar en el survival y de venderle a otro jugador. Entras con 25.» La primera parcela cuesta 25. Quien acaba de entrar puede poner la pala de oro sin haber jugado el survival, y se queda en 0 para la tienda. — [mensajes.json](file:///C:/Users/Ale/Documents/preguntones-m/discord/mensajes.json); [config.yml de Parcelas](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Parcelas/config.yml)
- La XO anuncia 4 duros y el código multiplica por la gente de la pista, seis veces por sábado, sin retirada previa. No hay un máximo de jugadores en la arena. Una sala de 10 personas paga 40 por ronda y 240 en la hora al mismo ganador; una de 20 paga 480. El techo de saldo, 2000, es el único tope. — [Bote.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/Bote.java); [Cita.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/xo/src/com/preguntones/xo/Cita.java); [config.yml de Essentials](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/config.yml)
- `/eco give` y `/eco set` crean o reescriben saldo hasta 2000, repetibles, solo con el comodín de admin. `worth.yml` deja al mismo admin fabricar duros vendiendo ítems a precios de ejemplo, entre ellos un pico de diamante a 650, más que una tarde de misiones. — [admin.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/LuckPerms/yaml-storage/groups/admin.yml); [worth.yml](file:///C:/Users/Ale/Documents/preguntones-m/servidor/plugins/Essentials/worth.yml)
- `/ciclo reset` borra `hechos` y permite volver a cobrar los 302 o los 340. Es de op. — [MundosPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)
- Condonar una deuda deja en el bolsillo el capital que salió de la caja. No es un depósito nuevo. Lo puede hacer op o `parcelas.admin` (`staff` lo tiene). El tamaño lo marca lo que hubiera en la caja, no una constante. — [Banco.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Banco.java)
- Lo que no se salta el circuito: cada misión tiene `veces`. Nether, End y dragón son 16, 18 y 20, por debajo de una pila de troncos (96). El interés del banco se cobra al deudor; no se paga a quien guarda duros. Vender parcela o cofre mueve saldo y aparca el 10 % en la caja. Las devoluciones deshacen un cobro. No hay voto ni premio de entrada.

### Inferences

- Las misiones, tal como están, no compran el catálogo ni en un día ni en un ciclo. El desajuste gordo no es el 6 de la madera: es el regalo de 25, que coincide con la parcela, y la XO, que crece con la sala.
- A partir del ciclo 2 el tope de misiones puede repetirse cada 48 horas si cazan el dragón seguido. Sigue siendo unos cientos de duros, no decenas de miles. No es un grifo sin tapa; la tapa es el ciclo.

### Gaps

- No hay un registro en el repo de si alguien ha usado `/eco give` o `/sell`. El permiso existe; el uso no está en el código.

## Qué cifras de ingreso cuadran con la parcela y con unas pocas pilas

### Takeaway

La tarde que mina, caza, camina, duerme y pesca debería dejar 210 duros, que son la primera parcela (25) más 32 antorchas (24), 32 troncos de roble (96) y 32 de roca (64). Hoy esa tarde, sin contar los 25 de entrada, deja 164. La propuesta de abajo no toca los precios de la tienda ni de las parcelas: solo los ingresos.

### Cited Findings

- Cesta usada como vara, precios actuales, sin cambiarlos. Parcela libre: 25. La segunda, por el mismo bucle del código, 55: la cesta no debe pagarla. Antorchas 32 por 24, troncos de roble 32 por 96, roca 32 por 64. Suma de la cesta: 25 + 24 + 96 + 64 = 209. Ficha de aldeano y bandera: 40 cada una, aparte de la cesta. Una fila cara del catálogo: 216. Una compra de cada fila que el fuente pide: 58.136. — [ParcelasPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java); [Catalogo.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/parcelas/src/com/preguntones/parcelas/Catalogo.java)
- Tarde actual de madera, piedra, caza, comida, camino, cama, hierro y pesca: 18 + 32 + 30 + 12 + 24 + 8 + 24 + 16 = 164, más 25 de entrada = 189. Falta para la cesta de 209 si no se cuentan los 25, y sobra la parcela entera antes de jugar si se cuentan. — [MundosPlugin.java](file:///C:/Users/Ale/Documents/preguntones-m/tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)

Números propuestos, solo de ingreso. Los `veces` y las `meta` se quedan. Cambia el duro por tramo, el saldo inicial y el premio de la XO.

- Saldo al entrar: 0. Hoy 25. Así la parcela se juega, no se trae puesta.
- Madera: 8 por tramo (máximo 24). Hoy 6 (máximo 18).
- Piedra: 10 por tramo (máximo 40). Hoy 8 (máximo 32).
- Caza: 12 por tramo (máximo 36). Hoy 10 (máximo 30).
- Comida: 8 por tramo (máximo 16). Hoy 6 (máximo 12).
- Pesca: 10 por tramo (máximo 20). Hoy 8 (máximo 16).
- Camino: 10 por tramo (máximo 30). Hoy 8 (máximo 24).
- Cama: 12, una vez. Hoy 8.
- Hierro: 16 por tramo (máximo 32). Hoy 12 (máximo 24).
- Esa tarde suma 210. Paga la cesta de 209 y deja 1 duro. No llega a la segunda parcela (55) ni a una fila de 216.
- Aldeano: 12, una vez. Hoy 10.
- Encantar: 16, una vez. Hoy 12.
- Diamante: 12 por tramo (máximo 24). Hoy 16 (máximo 32). Baja, porque 4 menas no deben valer más que la tarde de piedra.
- Enderman: 8 por tramo (máximo 16). Hoy 10 (máximo 20).
- Entrar al Nether: 12, una vez. Hoy 16.
- Blaze: 12 por tramo (máximo 24). Hoy 14 (máximo 28).
- Escombro: 16, una vez. Hoy 20.
- Entrar al End: 16, una vez. Hoy 18.
- Dragón: 20 a cada compañero conectado con la puerta abierta, como ahora. Un grupo de 4 fabrica 80, menos que una pila de troncos (96). El rango sigue siendo el premio de quien no está en línea; no hace falta subir los 20.
- XO: 8 duros fijos al ganador de la ronda, seis rondas como ahora. Hoy 4 por persona de la pista. Quien gane las seis se lleva 48, una propina de sábado, no otra tarde. Deja de crecer con la sala. Sigue haciendo falta dos personas para que haya ronda.
- Día que liquide superficie, Nether y escombro: 210 + 12 + 16 + 24 + 16 + 12 + 24 + 16 = 330. Con el End y el dragón, cuando la puerta exista: 366. Con las seis XO ganadas: 414 en esa semana. 414 es el 0,7 % de las 58.136 del catálogo. Da para la cesta y para una ficha de aldeano (40) o la bandera (40), no para las 891 filas y no para repetir la cesta muchas veces el mismo día.
- Un `/eco give` de admin, si se usa, no debería pasar de 210 de una vez: el tamaño de la tarde, no el techo de 2000. `/sell` contra `worth.yml` no es un ingreso de jugador; el grupo `default` ya no lo tiene, y conviene que siga así.
- Interés pagado por el banco a quien ahorra: 0, que es lo que hay. El préstamo no entra en esta tabla: es deuda contra la caja.

### Inferences

- Con estos números, la persona nueva que hace la tarde fácil compra la parcela y tres pilas normales, y se queda sin la segunda parcela y sin la fila cara. Quien se queda el día entero y abre el Nether suma unos 330, todavía una fracción del catálogo.
- El premio plano de 8 alinea el código con la frase pública de «4 duros» solo a medias: la frase dice 4 y la propuesta dice 8, a propósito, para que una hora ganada entera sea una pila de antorchas (24) y algo más, no una parcela. Si se prefiere obedecer el texto al pie de la letra, el plano sería 4 y la hora entera serían 24. La pieza que importa es que deje de ser 4 por cabeza.

### Gaps

- 58.136 es la suma de las filas que pide el fuente, calculada con las mismas funciones de precio, sin arrancar el servidor. Cada material que `Material.valueOf` rechace resta esa fila. No cambia el orden: sigue siendo decenas de miles, no unos cientos.
- No se propone cifra para el impuesto, el interés del préstamo ni la curva de parcelas. Esos precios se han usado solo como vara.
