# Recortar el ciclo a 220 duros

Completar una misión ya ingresa duros: lo que hay que cambiar es el importe de lo repetible y el momento en que el jugador lo lee. Un ciclo entero, con esa persona conectada al morir el dragón, baja de **340 a 220 duros**. Se recorta el cuarto argumento de las catorce misiones de estadística, salvo el escombro, y la XO pasa de `4 × personas` en cada una de sus seis rondas a **2 duros, una vez por persona y sábado**. Nether (**16**), End (**18**), escombro (**20**) y dragón (**20**) se quedan, igual que los **25** de entrada, la escalera **25, 55, 95, 145, 205, 275**, el clic de **16** en `/puesto`, la bandera de **40**, el **10 %** y el techo de **2000**. Con esa tabla la primera parcela sigue saliendo de la paga de entrada; la segunda cabe en cinco misiones tempranas (**56**); la tercera pide el capítulo de mineral, el Nether y los blazes de pago; la sexta no se abre hasta el cuarto ciclo. El premio se escribe antes de cobrarlo en `/ciclo`, en la página «Los duros» del libro, en el diccionario de la web y en la guía de Discord; el chat verde de después ya interpola el entero y cambia solo al recompilar. El informe de mercado quería cero duros el sábado: esta tabla deja **2**, por debajo del tramo más bajo, y esa distancia se acepta.

## Un ciclo entero baja de 340 a 220 duros

Hoy el array `misiones()` de Mundos es la lista cerrada de las catorce misiones de estadística, y Nether, End y dragón son otros tres literales. La puerta de **12** blazes no tiene campo de duros y sigue en **0**. No hay plugin de quests, ni Jobs, ni Votifier, ni una tarea semanal con importe: el catálogo real es ese Java. El sondeo corre cada **100** ticks (**5** segundos), solo con el jugador en `survival`, `survival_nether` o `fin`. Paga `duros × tramos enteros` nuevos desde la última foto; si el progreso no llega a la meta, no ingresa nada. Si Vault rechaza el depósito, no marca el hecho y el siguiente sondeo lo reintenta. Las metas, las veces y los textos del constructor no cambian. Solo cambia el cuarto argumento, `duros`.

El desajuste que cierra esta tabla es interno al ciclo. Picar piedra paga hoy **32** duros por ciclo y cazar monstruos **30**, los dos por encima del dragón (**20**). El diamante repite el mismo **32**. La regla que se aplica, leída sobre estos números y no como una fracción que publique nadie, es esta: el máximo de una misión de recurso común se queda por debajo del primer hito de una vez, el Nether a **16**; el hierro, el diamante y los blazes de pago pueden igualar esos **16** y se quedan por debajo del dragón; la XO, que es el otro repetible, se queda por debajo del tramo más bajo, que pasa a ser la piedra a **3**. Subir el dragón, el escombro, el Nether o el End para que el pico no los adelante fabricaría más moneda. JoinGG, el 9 de septiembre de 2026, describe el fallo de fondo: el ingreso suele ser por jugador y por hora, y el gasto suele ser una compra de una vez; cuando el jugador ya tiene lo que quería, deja de gastar y sigue cobrando. Su escena de «a las seis semanas, 400 del recién llegado frente a 11 millones del veterano» es una ilustración suya, no una medición de Preguntones ([JoinGG](https://www.joingg.co/blog/server-economy-design)).

| Id | Qué cuenta | Meta | Tramos | Tramo ahora | Máximo ahora | Tramo nuevo | Máximo nuevo |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| madera | Troncos y tallos del array | 16 | 3 | 6 | 18 | 4 | 12 |
| piedra | Piedra, roca, deepslate y deepslate rocosa | 32 | 4 | 8 | 32 | 3 | 12 |
| caza | Doce monstruos del overworld | 4 | 3 | 10 | 30 | 4 | 12 |
| comida | Vaca, cerdo, gallina, oveja, conejo | 3 | 2 | 6 | 12 | 4 | 8 |
| pesca | `FISH_CAUGHT` | 6 | 2 | 8 | 16 | 4 | 8 |
| camino | Andar y correr, 40.000 cm | 40000 | 3 | 8 | 24 | 4 | 12 |
| cama | Dormir | 1 | 1 | 8 | 8 | 6 | 6 |
| aldeano | Un trato | 1 | 1 | 10 | 10 | 8 | 8 |
| hierro | Mena y deepslate de hierro | 8 | 2 | 12 | 24 | 8 | 16 |
| diamante | Mena y deepslate de diamante | 2 | 2 | 16 | 32 | 8 | 16 |
| encantar | Un encantamiento | 1 | 1 | 12 | 12 | 8 | 8 |
| blaze | Muertes de blaze | 2 | 2 | 14 | 28 | 8 | 16 |
| escombro | Ancient debris | 1 | 1 | 20 | 20 | 20 | 20 |
| enderman | Muertes de enderman | 3 | 2 | 10 | 20 | 6 | 12 |
| nether | Estar en `survival_nether` | 1 | 1 | 16 | 16 | 16 | 16 |
| end | Estar en `fin` | 1 | 1 | 18 | 18 | 18 | 18 |
| dragón | Muerte del dragón, cada miembro conectado | 1 | 1 | 20 | 20 | 20 | 20 |

La suma de los máximos nuevos es **220**: 12+12+12+8+8+12+6+8, más 16+16+8, más 16+20+16, más 12+18+20. Los catorce de estadística bajan de **286** a **166**, y con Nether, End y dragón el ciclo baja de **340** a **220**, **120** duros menos por persona y ciclo. El **220** no está escrito en ningún sitio hoy; a partir de este cambio es la cifra pública del ciclo completo. La puerta de 12 blazes no entra en la suma. Sigue en **0**, lee la misma estadística de blaze con otra base (`caza-blaze`) y no se descuenta de la misión `blaze`. Completar los dos tramos de pago (4 blazes, **16** duros) no abre el End: el portal sigue pidiendo **12** blazes de este ciclo y **14** días ([MundosPlugin.java](../tools/mundos/src/com/preguntones/mundos/MundosPlugin.java)).

`hechos` guarda cuántos tramos se cobraron, no cuántos duros. Bajar el premio no devuelve lo ya ingresado y no obliga a migrar saldos. El tramo ya apuntado no se vuelve a pagar. El avance a medias sigue sin pagarse: un tronco o una piedra sueltos serían el grifo de un recurso infinito, el caso que JoinGG y el propio diseño rechazan en la recompra administrativa. Lo que sí cambia es la memoria de ese avance. Hoy `bases` vive en RAM y `guardarEstado` escribe `hechos` y `caza-blaze`, no la foto. Un reinicio conserva los tramos cobrados y olvida el progreso a medias, así que el siguiente tramo exige otra meta entera. Para que la fracción de `/ciclo` diga la verdad después de un reinicio, `bases.<uuid>` se guarda en `estado.yml` como lista de **14** enteros, en el orden de `foto()`. Si la lista falta o no mide 14, se hace una foto nueva y ese tick paga **0**, que es el comportamiento de la primera vez que el sondeo ve al jugador. `reiniciarCiclo` ya vacía el mapa y luego llama a `guardarEstado`: al escribir la lista ahí, el ciclo nuevo no hereda la foto vieja.

La XO es el otro grifo. `Bote.POR_PERSONA` vale **4** y `premio` multiplica por la gente que había en la pista al empezar la ronda. El reloj abre seis huecos: **21:35, 21:45, 21:55, 22:05, 22:15 y 22:25**. Quien gana las seis con la pista llena cobra `24 × personas` ese sábado: **48** con dos personas, **120** con cinco, y **120** ya es seis veces el dragón ([Bote.java](../tools/xo/src/com/preguntones/xo/Bote.java); [Cita.java](../tools/xo/src/com/preguntones/xo/Cita.java)). La regla nueva: **2 duros, una vez por persona y sábado de Madrid, en la primera ronda que gane**. Las otras rondas coronan ganador y no acuñan. **2** es menor que **3**. Una sesión abierta a mano en otro día de la semana tampoco acuña. El dato vive en `plugins/Xo/cita.yml`, clave `premio.<uuid>` igual a la fecha `aaaa-mm-dd` de ese sábado, leída en `cargarCita` y escrita dentro de `guardarCita`. Ese método reconstruye el archivo entero: si el mapa no entra en el mismo guardado que `jugada`, un reinicio de la cita borra quién ya cobró y la noche puede pagar otra vez. `premiar` ingresa **2** solo si esa fecha no coincide; si coincide, devuelve **0** sin llamar a `depositPlayer` y sin el chat de fallo. El broadcast que ya existe, «nombre gana la ronda.», es la frase de la ronda que no paga. No se escribe «+0 duros». `Pruebas` deja de exigir `premio(5) == 20` y `premio(1) == 4`: el premio de la noche es **2** con cualquier aforo, y la segunda victoria de esa persona es **0**.

El voto y la tarea corta de la semana no están en el repo, así que no entran en la tabla. Si más adelante hay voto, el importe de esta tabla es **2 duros una vez por ciclo**, no **2** cada día: catorce días por **2** son **28**, y **28** supera al dragón.

## La sexta parcela llega en el cuarto ciclo

La aritmética de abajo gasta en parcelas en cuanto el saldo llega, no compra en `/puesto` ni paga la bandera, y cobra el dragón estando conectado. Los **25** de entrada se van en la parcela 1 y el contador de misiones empieza en **0**. No hay en el repo, ni en una fuente pública citada, una medición de cuántos minutos tarda alguien nuevo en cerrar un tramo: la cuenta es de duros. Quien se compra la bandera (**40**) retrasa una parcela; ese retraso no está medido con jugadores reales.

| Hito | Lo que entra | Lo que paga | Saldo |
| --- | ---: | ---: | ---: |
| Alta y parcela 1 | 25 | 25 | 0 |
| Capítulo temprano | 78 | 55, la parcela 2 | 23 |
| Hierro, diamante y encantar | 40 | — | 63 |
| Nether | 16 | — | 79 |
| Dos tramos de blaze | 16 | 95, la parcela 3 | 0 |
| Escombro, enderman, End y dragón | 70 | — | 70 |
| Capítulo temprano del 2.º ciclo | 78 | 145, la parcela 4 | 3 |
| Resto de ese ciclo | 142 | — | 145 |
| 3.er ciclo entero | 220 | 205, la parcela 5 | 160 |
| Temprano, hierro, diamante y encantar del 4.º | 118 | 275, la parcela 6 | 3 |

El capítulo temprano es madera, piedra, caza, comida, pesca, camino, cama y aldeano: **12+12+12+8+8+12+6+8 = 78**. La parcela 2 no espera a ese cierre. Madera, piedra, caza, camino y pesca suman **56**, la parcela cuesta **55** y sobra **1**. Hacen falta esas cinco misiones, no un rato de pico. El resto del temprano (comida, cama y aldeano, **22**) es lo que, sumado a ese duro, deja los **23** de la fila.

La parcela 3 cuadra al duro con el camino largo: **23+16+16+8+16+16 = 95**. Si los endermen (**12**) se cobraron antes, el Nether deja el saldo en **91** y el primer tramo de blaze (**8**) es el que cruza los **95**. En los dos órdenes, con las tres parcelas ya pagadas y sin tienda, el dragón del primer ciclo deja **70**. **70** no llega a la parcela 4 (**145**).

El segundo ciclo temprano suma **148**, paga la cuarta y deja **3**. El resto de ese ciclo son **220 − 78 = 142**, así que al cerrarlo hay cuatro parcelas y **145** duros. El tercer ciclo entero lleva el saldo a **365**, la quinta cuesta **205** y quedan **160**. La sexta cuesta **275**. El temprano del cuarto ciclo deja **238**; hierro y diamante suman **32** y dejan **270**; el encantar (**8**) la paga y quedan **3**. Las cinco parcelas posteriores a la entrada cuestan **775**. **775 / 220 = 3,52** ciclos, que es el mismo recorrido: el primer ciclo también se gasta en la segunda y la tercera.

Lo que quien acaba de llegar puede comprar, y el primer sitio donde el saldo se queda corto:

Al entrar, antes de cualquier misión, los **25** compran la parcela 1. También compran un clic de **16** y dejan **9**, y **9** no es una parcela. Las dos cosas a la vez piden **41**. Con la parcela 1 ya pagada, ningún máximo temprano llega solo al clic de **16**: el más alto de ese capítulo es **12**. La madera entera (**12**) más un tramo de comida (**4**) suman **16**. Un tramo suelto de madera (**4**) no llega. El primer premio que, él solo, paga ese clic es el hierro entero (**16**) o el Nether (**16**).

Con el capítulo temprano cobrado (**78**) y la parcela 1 ya pagada, caben la parcela 2 y un clic de **16** (**71**, sobran **7**). Cabe la bandera (**40**) y no la parcela 2, porque **40+55 = 95**. No cabe la parcela 3. Si la parcela 2 ya está pagada y quedan **23**, el hierro entero deja **39**, a un duro de la bandera. El tramo siguiente de ese capítulo es el primero de diamante, **8** duros, y **39+8 = 47**: la bandera entra y la parcela 3 espera. Tras hierro, diamante, encantar, Nether y los dos tramos de blaze, la parcela 3 está pagada y la 4 no.

El enderman se puede cazar en el overworld. Sus **12** duros no adelantan la parcela 2, que sigue pidiendo **55**, y adelantan la 3 solo hasta la puerta del Nether. El veterano que repite el ciclo cobra **220**, no **340**. Nueve ciclos sin gastar son **1.980**, por debajo del techo; el décimo se encontraría con los **2.000**. Nueve ciclos de **14** días son **126** días. Sigue sin saberse, porque no se ha abierto el jar de EssentialsX 2.22.0, si un `depositPlayer` que cruzaría **2.000** falla entero o ingresa hasta el tope. Si falla entero, Mundos no marca el hecho. El techo no se baja: una parcela se puede vender a **2.000**, y el comprador tiene que poder tener ese saldo ([Essentials config.yml](../servidor/plugins/Essentials/config.yml)).

## La tienda y la escalera se quedan quietas

Los precios que un jugador pisa se quedan donde el juego ya los cobra. Moverlos para «acompañar» el recorte haría lo que JoinGG desaconseja el 9 de septiembre de 2026: subir tarifas castiga a quien acaba de llegar, y el arreglo de una moneda que entra por hora es un sumidero, con una franja barata que esa persona pueda pagar en la primera sesión ([JoinGG](https://www.joingg.co/blog/server-economy-design)). Aquí la franja barata ya es el **25** contra el **25**. EnduranceNodes, en la revisión del 6 de septiembre de 2026, separa el puente de la impresora: Vault no es la economía, y venderle al servidor un objeto a precio fijo y sin tope sí lo es. El comercio entre jugadores no necesita un grifo; una tasa pequeña puede convertir ese movimiento en sumidero ([EnduranceNodes](https://endurancenodes.com/knowledge-base/plugins/economy-setup)).

| Qué | Cifra que se queda | Dónde está |
| --- | --- | --- |
| Alta | 25, suelo 0, techo 2000, símbolo ` duro` detrás | `starting-balance`, `min-money`, `max-money` de Essentials |
| Parcela nueva | 25, 55, 95, 145, 205, 275. Las seis suman 800 | `precios.libre: 25` y el bucle `30 + 10 * i` |
| Reventa de parcela | De 20 a 2000. El 10 % entero, hacia abajo, entra en la caja | `precios.minimo`, `maximo`, `impuesto-porcentaje` |
| Tope | 6 parcelas por persona. El clan no lo sube | `max-por-jugador`. `clan.parcelas-por-persona` no lo lee nadie |
| `/puesto` | Catálogo entero. El clic más barato son 16; el más caro de un clic, 216 | `Catalogo.java`, `ESCALA = 4` |
| Bandera | 40 duros, hasta 5 colocadas | Cobro fijo, fuera del catálogo |
| Cofre de venta | Precio por unidad de 1 a 500, tope 2000 por compra, impuesto 10 | `Cuentas.IMPUESTO = 10`, no el yaml |
| Cartel `[Trade]` | El precio lo pone quien carga el cartel. Sin impuesto en la config | `enabledSigns: trade` |
| Banco | 1 día al 10 %, 3 al 20 %, 7 al 30 %. El interés se destruye. La caja nace una vez con 150 | `Banco.java` |
| Euro | El VIP de 4,99 € no fabrica duros | `tienda.html`, `paquetes.json` |

Reclamar una libre hace `withdrawPlayer` y no llama a `sumarCaja`: esos duros salen de la circulación. Abandonar no devuelve nada. `/puesto` y la bandera tampoco ingresan en ningún sitio; si el objeto no cabe, se devuelve el precio, que es una devolución y no una fuente. El interés del préstamo se resta primero al pagar y no entra en la caja; el capital sí vuelve. Esos tres —reclamo, tienda del servidor con la bandera, interés— son los sumideros que encogen la masa de duros. El **10 %** de una reventa o de un cofre no es ese sumidero: entra en la caja y puede volver como préstamo. Se mantiene porque es la tarifa que ya cobra el juego y la que fija el diseño del barrio, no porque drene la moneda. En el cofre la constante está clavada en el Java; en la parcela sí sigue el yaml. Hoy las dos valen **10**. Una venta de **4** deja tasa **0**; una de **40** deja **4** al barrio y **36** al dueño. Cambiar solo el yaml movería la parcela y dejaría el cofre igual. Esta pasada no cambia ninguna de las dos.

Tampoco se añade una renta por parcela. JoinGG la recomienda el 9 de septiembre de 2026 como el sumidero más fuerte. El diseño de Preguntones ya soltó el castigo de borrar el terreno a quien se ausenta: ampliar cuesta duros y el terreno abandonado se puede recuperar. El **10 %** y las **48** horas que ese mismo diseño cita son la política de subasta de Towny Reborn, no una tarifa de este servidor. El impuesto que se cobra es el **10 %** del código. GriefPrevention tiene en su config **8.0** duros por bloque de claim comprado y **0** al venderlos; el grupo `default` no lista ese permiso y no está demostrado que un jugador sin rango pueda gastarlo. Queda fuera de esta tabla. `worth.yml` sigue siendo la plantilla de serie y los carteles `buy` y `sell` siguen apagados: no hay recompra administrativa.

El cofre propio cuesta materiales (cuatro lingotes de oro, un cofre y un cartel), **0** duros. Poner la primera tienda de jugador es más barato en duros que cualquier clic de `/puesto`. El euro sigue sin fabricar duros, herramientas ni terreno.

## El importe se lee antes de cobrar

El jugador, hoy, ve el importe al cobrarlo. La excepción es el libro del dragón, que ya dice «20 duros» antes de matarlo. `/ciclo` nombra actividades y no dice ningún duro: «Los duros salen de jugar allí: madera, piedra, caza, explorar, hierro, diamante, Nether y el dragón.» El libro, la página «Los duros», dice que salen de talar y picar, sin cifras. La web y Discord dicen que las misiones dan duros y no dicen cuántos. El marcador `Duros: %preguntones_duros%` es el saldo de Vault, no el premio, y se queda así: diecisiete cifras no caben en esa línea.

La fuente de las cifras es una sola: el array más los tres literales. El libro, la web y Discord no leen ese array. Si se recompila solo el Java, el «4 duros» viejo sigue en público. El chat de después no hay que reescribirlo a mano. `pagar` manda `§a` + texto + ` +` + (`duros × tramos nuevos de ese sondeo`) + ` duros.` Si en un sondeo entran dos tramos de piedra, sale una línea, «Has picado piedra. +6 duros.», no dos. Estas son las líneas de un solo tramo, ya con el importe nuevo:

| Id | Chat al cobrar un tramo |
| --- | --- |
| madera | Has talado madera. +4 duros. |
| piedra | Has picado piedra. +3 duros. |
| caza | Has cazado monstruos. +4 duros. |
| comida | Has cazado para comer. +4 duros. |
| pesca | Has pescado. +4 duros. |
| camino | Has explorado. +4 duros. |
| cama | Has dormido en una cama. +6 duros. |
| aldeano | Has comerciado con un aldeano. +8 duros. |
| hierro | Has picado hierro. +8 duros. |
| diamante | Has picado diamante. +8 duros. |
| encantar | Has encantado. +8 duros. |
| blaze | Has cazado blazes. +8 duros. |
| escombro | Has picado escombros del Nether. +20 duros. |
| enderman | Has cazado endermen. +6 duros. |
| nether | Has entrado al Nether. +16 duros. |
| end | Has entrado al End. +18 duros. |
| dragón | Habéis cazado al dragón. +20 duros. |
| XO, primera del sábado | Ganas la XO. +2 duros. |

Nether, End y dragón siguen con el **16**, el **18** y el **20** escritos también en la llamada, no solo en el mensaje: como el número no cambia, la frase tampoco. El broadcast del dragón sigue sin el importe: «han cazado al dragón. El survival se borra el … El rango Cazador se queda.» El rango se concede aunque Vault falle; el «+20 duros» solo sale si el depósito entra. Al guardar ese éxito en `hechos.dragon` de esa persona, `/ciclo` puede decir `1/1` solo a quien cobró. Quien estaba desconectado no cobra, y el código de hoy no guarda un hecho por persona: sin esa clave la línea mentiría.

`/ciclo` sustituye la frase sin cifras por una línea por id, en el orden de la tabla, y conserva el resto (horas hasta el borrado, fecha del dragón, grupo de hasta 4). La forma es `§fMadera §e4/16 §f· §e1/3 §f· 4 duros.` La primera fracción es el resto del tramo en curso, no el total histórico: 20 troncos con un tramo ya cobrado se leen `4/16 · 1/3 · 4 duros`. Cuando no queda otro tramo, la primera fracción se queda en la meta. En el camino la meta visible es **400 bloques**; el código sigue comparando **40.000** centímetros. Nether, End y dragón usan `0/1` o `1/1` y su cifra. La puerta no se mezcla con la misión `blaze`: la línea que ya dice «Llevas X/12» pasa a «Llevas X/12 blazes. 0 duros.»

La página «Los duros» del libro no cabe la tabla: sus líneas son cortas y la página ya lleva el botón de `/pay` y el de `/puesto`. Se sustituye el primer bloque y se dejan el bloque de la web, los dos botones y el volver. El texto nuevo, con los saltos del libro, es:

```
En el survival,
/ciclo dice el
premio de cada una.

El tramo bajo son 3.
El dragón, 20.
El ciclo, 220.
Y al vender a otro.
El euro no los crea.
```

El diccionario de la web, en la ficha Duros, sí cabe la tabla. La frase de entrada y de la primera parcela se queda, y detrás entra esta:

```
Un ciclo completo son 220 duros. /ciclo enseña el avance. Cada cifra es un tramo: madera 4 (16 troncos, 3 veces), piedra 3 (32, 4 veces), caza 4 (4 monstruos, 3 veces), comida 4 (3 animales, 2 veces), pesca 4 (6 peces, 2 veces), camino 4 (400 bloques, 3 veces), cama 6 (una vez), aldeano 8 (una vez), hierro 8 (8 menas, 2 veces), diamante 8 (2 menas, 2 veces), encantar 8 (una vez), blazes 8 (2 blazes, 2 veces), escombro 20 (una vez), enderman 6 (3 endermen, 2 veces), Nether 16, End 18 y dragón 20. La puerta de 12 blazes da 0 duros. El resto sale de venderle a otro jugador.
```

La misma lista, con la misma palabra «tramo» y el **220**, sustituye en Discord la respuesta «¿De dónde salen los duros?» a partir de «El resto sale de las misiones del survival». `entrar.html` deja de decir solo «Las siguientes salen de jugar en el survival» y pasa a: «Entras con 25 duros. La primera parcela cuesta 25. Las siguientes salen de las misiones del survival: un ciclo completo son 220 duros, y /ciclo enseña cada una. Los duros no se compran. El tope es seis por persona, también dentro de un clan.» La portada, «Los duros salen de jugar y de venderle algo a otro jugador», se queda: la tabla está en el diccionario de la misma página. Las normas de Discord, «Los duros salen de jugar en el survival y de venderle a otro jugador. Entras con 25», también se quedan. El **25** sigue siendo verdad.

La XO es el sitio donde un texto viejo mentiría el mismo día del cambio. La frase que sustituye a «4 duros por persona» y a «Cada ronda son 4 duros» es, en todos los sitios: **«2 duros, una vez por sábado. Las otras rondas no dan duros.»** Al tocar esas frases hay un error ya escrito que no se copia: el libro dice «Cinco segundos», y la web dice «5 segundos», mientras `FRASE_SEGUNDOS` y Discord dicen **15**. «La primera ronda sale en cinco minutos» es otra cosa, el margen de `REUNIR_MS`, y se queda.

| Sitio | Texto que queda |
| --- | --- |
| Libro, pie de «La XO» | La frase, dónde ponerse y 2 duros, una vez por sábado. Si usas Discord, la voz es voz-sabado. |
| Libro, «En la ronda» | Quince segundos. Y, al cierre: «2 duros, una vez / por sábado.» |
| Aviso de `XoPlugin` | Tienes 15 segundos para colocarte. 2 duros, una vez por sábado. Las otras rondas no dan duros. |
| Chat al abrir, sábado y sesión manual | La primera ronda sale en cinco minutos. 2 duros, una vez por sábado. Las otras rondas no dan duros. |
| `index.html`, carta y ficha La XO | 15 segundos para colocarte. 2 duros, una vez por sábado. Las otras rondas no dan duros. |
| `normas.html`, meta description | Se quita «la cita del sábado no da duros» y queda «la cita del sábado da 2 duros, una vez». |
| `normas.html`, la XO | 15 segundos. «Quien gana la primera ronda de la noche se lleva 2 duros. Las otras rondas no dan duros, y el ganador entra en la siguiente.» |
| Discord, `anuncios.cita_sabado` | Quien gana la primera ronda de la noche se lleva 2 duros. Las otras rondas no dan duros. |
| Discord, `guia` | 2 duros, una vez por sábado. Las otras rondas no dan duros. |

Estas frases siguen siendo verdad y no se reescriben: «Traes 25 duros. Justo la primera.»; «La primera: 25.»; «1ª  25    2ª  55 / 3ª  95    4ª 145 / 5ª 205    6ª 275»; «Hasta 4. Al caer, 20 duros y Cazador.»; «Cuesta 40 duros.»; las preguntas de la XO «La primera parcela libre de Preguntones cuesta 25 duros» y «Al entrar en Preguntones recibes 25 duros». Karma puede salir en la misma acción que el chat de duros y usa otra palabra: «+2 de karma» al comerciar o al dormir, «+1 de karma» al pescar. No es un premio de esta tabla.

Los jars `Mundos.jar`, `Xo.jar` y `Viajes.jar` son el build. Se sustituyen al compilar; no se parchean a mano. El cuaderno vive en Viajes, así que el tercer jar entra en el mismo envío. La web y `discord/mensajes.json` se suben por su ruta. El mundo no entra en ese envío. Parcelas, Essentials, el catálogo y el banco no se recompilan para esta tabla.

Queda sin comprobar si el `sendMessage` verde llega a Discord. DiscordSRV tiene el chat del juego hacia el canal y Mundos no manda un webhook: el sitio seguro del premio es el chat del juego, `/ciclo`, el libro, la web y la guía.

## La XO de 2 duros deja atrás el informe de mercado

La tabla respeta lo que el juego ya cumple del diseño: una moneda, los duros; el euro no la fabrica; no hay recompra de un recurso infinito; el recién llegado paga la primera parcela con la paga de entrada; el interés no vuelve a la caja; el barrio se queda el **10 %** de la venta entre jugadores. Choca con el informe de mercado en el sábado. Ese texto fija el premio de la cita en un título o un aspecto. El código paga `4 × personas` hasta seis veces. Esta tabla deja **2 duros, una vez**. Sigue siendo una inyección, más pequeña que cualquier tramo. Ponerla a **0** cumpliría esa frase y vaciaría los textos que hoy prometen un premio en duros. La distancia que se acepta es de **2**.

El mismo informe gana los duros vendiendo en un cartel, con la tarea corta de la semana y, cuando haya ficha en una lista, con un voto por debajo de una tarde de juego. No describe la tabla de Mundos. La tarea semanal no está en el código, y esta pasada no le pone un importe: las misiones ya son ese trabajo, y un segundo pago repetiría el grifo. «Una tarde» no es un número en ese informe. La lectura que usa esta tabla, y es una lectura, no una medición del servidor, junta un tramo de madera, piedra, caza, comida y camino más la cama: **4+3+4+4+4+6 = 25**, el mismo duro de la entrada. La XO de **2** queda por debajo de esa lectura. El voto, si llega, queda en **2** por ciclo por la cuenta de los **28**.

Con el informe del barrio el choque no está en los precios. El cofre, el **10 %**, la caja de **150**, los plazos y el interés destruido coinciden con el código, y esta tabla no los mueve. Hay dos flujos que ya divergían y que la tabla deja como están. Si el banco se queda la parcela, el informe quiere que el siguiente la reclame al precio normal y que ese precio entre en la caja; el código cobra y no ingresa. Dejarlo destruido es lo que hace del reclamo un sumidero. Pasarlo a la caja cumpliría el informe y devolvería el duro al circuito del préstamo: no es parte de este cambio. El informe también quiere que un `/pay` con la deuda vencida salde la deuda; el código cancela `/pay` y `/pagar` mientras `mora` es verdad. La tabla no toca `Banco.java`.

La meta description de `normas.html` dice que la cita del sábado no da duros, y el párrafo de la misma página dice que quien queda se lleva **4**. Ya se contradicen hoy. Con esta tabla las dos frases quedan falsas hasta que se sustituyen por el **2**, una vez. La renta periódica que JoinGG pide desde el primer día no se añade.

## Conclusión

El agujero no era la primera parcela, que ya sale justa con la entrada, ni un plugin de quests que no está instalado. Era que dos misiones repetibles ganaban al dragón y que la XO podía acuñar seis veces en la misma noche. Cerrar esos dos grifos, enseñar el entero antes de cobrarlo y dejar quietos el terreno y la tienda es el cambio entero. Quien ya tiene un tramo en `hechos` se queda con el duro viejo y no vuelve a cobrar ese tramo; el techo de **2.000**, si Essentials rechaza el depósito que lo cruzaría, es el único comportamiento que esta tabla no puede fijar desde el código de Preguntones.
