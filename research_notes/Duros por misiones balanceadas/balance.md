# Balance de duros por misiones

Lectura del 13 de noviembre de 2026. No se ha modificado código, config, web ni Discord. No se ha hecho pull ni push, no se han leído regiones ni `conexion.json`. Las tres notas de esta carpeta se toman como inventario; donde esta nota cita un pago, un precio o una frase, se ha vuelto a abrir el archivo y el número coincide. La propuesta nueva está marcada como tal. No se inventan misiones: los ids son los diecisiete pagos de Mundos más la puerta de blazes, que paga 0.

## Qué cifras y qué restricciones mandan

### Takeaway

Hoy el jugador ya cobra duros por las 14 misiones de estadística, por entrar al Nether y al End y por el dragón, pero solo ve el importe al cobrarlo, salvo el «20 duros» del dragón en el libro. La entrada de 25 paga exactamente la primera parcela. El desajuste no es la primera parcela: es que picar piedra (32) o cazar (30) en un ciclo paga más que matar al dragón (20), y que la XO puede pagar seis veces `4 × personas`.

### Cited Findings

- Saldo inicial 25, suelo 0, techo 2000, símbolo ` duro` detrás. El comentario del archivo dice que hay una sola moneda. — [config.yml de Essentials](servidor/plugins/Essentials/config.yml) líneas 1-13
- Las 14 misiones están en el constructor `Mision(clave, meta, veces, duros, texto)`. Por tramo: madera 6, piedra 8, caza 10, comida 6, pesca 8, camino 8, cama 8, aldeano 10, hierro 12, diamante 16, encantar 12, blaze 14, escombro 20, enderman 10. Metas y topes de tramo no se repiten aquí: son los de [misiones.md](research_notes/Duros%20por%20misiones%20balanceadas/misiones.md). — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 686-702
- Nether 16 y End 18 se pagan una vez por ciclo, con el jugador dentro de `survival_nether` o `fin`. El dragón paga 20 a cada miembro conectado del grupo y el chat escribe «+20 duros». El broadcast no lleva el 20. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 434-451 y 677-682
- `pagar` no hace nada si `progreso < meta`. El cobro es `duros * nuevas`, con `nuevas` tramos enteros. Si Vault no acepta el ingreso, no se marca el hecho. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 766-797
- La foto de progreso (`bases`) vive en memoria. `guardarEstado` escribe `hechos` y `caza-blaze`, no `bases`. Un reinicio conserva los tramos ya cobrados y olvida el avance a medias. La puerta de 12 blazes sí guarda su base y no paga. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 77, 307, 595-652 y 671-675
- Suma de un ciclo completo, ya en la nota de obtención y rehecha con estos constructores: 14 misiones 286, más Nether 16 y End 18, son 320; más el dragón 20, son 340. — [obtencion-actual.md](research_notes/Duros%20por%20misiones%20balanceadas/obtencion-actual.md); [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 686-702
- `/ciclo` nombra actividades y no dice ningún duro: «Los duros salen de jugar allí: madera, piedra, caza, explorar, hierro, diamante, Nether y el dragón.» — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) línea 114
- La XO paga `4 * max(personas, 1)`, fijado al empezar la ronda. Hay seis huecos: 21:35, 21:45, 21:55, 22:05, 22:15 y 22:25. Quien gana las seis con la pista llena cobra `24 × personas` ese sábado (con dos personas, 48; con cinco, 120). — [Bote.java](tools/xo/src/com/preguntones/xo/Bote.java) líneas 3-12; [Cita.java](tools/xo/src/com/preguntones/xo/Cita.java) líneas 26-46 y 87-89; [Pruebas.java](tools/xo/src/com/preguntones/xo/Pruebas.java) líneas 21-30
- Primera parcela libre 25. La siguiente suma `30 + 10 * i` por cada parcela ya poseída: 25, 55, 95, 145, 205, 275. Las seis suman 800. El duro del reclamo no entra en la caja. — [config.yml de Parcelas](servidor/plugins/Parcelas/config.yml) líneas 9-14; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 553-559
- El clic más barato de `/puesto` son 16 duros y ese dinero se destruye. La bandera son 40. El impuesto de venta entre jugadores es el 10 % hacia abajo y va a la caja. El interés del banco se destruye. El máximo de una parcela en venta y el de una compra del cofre son 2000, el mismo techo que el saldo. — [tienda-parcelas.md](research_notes/Duros%20por%20misiones%20balanceadas/tienda-parcelas.md); [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java)
- Restricciones que siguen siendo el intento del proyecto: una moneda llamada duros; el comercio entre jugadores importa; no hay recompra administrativa de un recurso infinito; el euro no fabrica duros ni poder; el voto, si llega, queda por debajo de una tarde de juego; el recién llegado paga la primera parcela con la paga de entrada; el interés no vuelve a la caja; el barrio se queda el 10 % de la venta entre jugadores. — [Diseño mercado servidor Preguntones.md](reports/Diseño%20mercado%20servidor%20Preguntones.md) líneas 19-23; [economia-del-barrio.md](reports/economia-del-barrio.md) líneas 25-31 y 97-101
- No hay plugin de quests, Jobs ni Votifier. Voto y «tarea corta de la semana» no tienen importe. — [misiones.md](research_notes/Duros%20por%20misiones%20balanceadas/misiones.md); [Diseño mercado servidor Preguntones.md](reports/Diseño%20mercado%20servidor%20Preguntones.md) línea 23

### Inferences

- Tratar el array de `misiones()` como la lista cerrada. Nether, End y dragón son otros tres literales, no filas del array. La puerta de blazes es un objetivo sin campo `duros`.
- El techo de 2000 no se puede bajar mientras una parcela se pueda vender a 2000: el comprador no podría pagarla con el saldo.

### Gaps

- No se ha abierto el jar de EssentialsX 2.22.0. Sigue sin verse si un `depositPlayer` que cruzaría 2000 falla entero o ingresa hasta el tope. El código de Mundos, si `transactionSuccess()` es falso, no marca la misión. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 777-779
- No hay `userdata` de Essentials en esta copia. No se sabe cuántos duros tiene nadie en el servidor en marcha.

## Qué hay que cambiar y qué se queda

### Takeaway

Se bajan los pagos de las misiones de recurso común y de mineral, se deja la XO en 2 duros una sola vez por sábado, y se enseña el premio antes de cobrar. No se tocan los 25 de entrada, la escalera de parcelas, el 16 de `/puesto`, la bandera de 40, el 10 %, el interés destruido ni el techo de 2000. Nether 16, End 18, escombro 20 y dragón 20 se quedan: son los hitos de una vez que ya están escritos o que marcan el techo.

### Cited Findings

- La primera parcela y la entrada son el mismo 25. El informe pide que el recién llegado compre ese terreno con la paga de entrada, no con el ingreso de un veterano. — [Essentials config.yml](servidor/plugins/Essentials/config.yml) línea 9; [Parcelas config.yml](servidor/plugins/Parcelas/config.yml) línea 10; [Diseño mercado servidor Preguntones.md](reports/Diseño%20mercado%20servidor%20Preguntones.md) línea 23
- Piedra acaba en 32 duros por ciclo y caza en 30. El dragón paga 20. Esos dos repetibles ya superan el hito de una vez más alto. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 689-690 y 450
- Una ronda de la XO no es el único pago de la noche: el reloj abre otra en cada hueco de diez minutos, seis en total. — [Cita.java](tools/xo/src/com/preguntones/xo/Cita.java) líneas 87-89
- El jugador no tiene una pantalla de misiones. El premio previo existe solo en la página del dragón: «Hasta 4. Al caer, 20 duros y Cazador.» — [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 319-327
- JoinGG, el 9 de septiembre de 2026, dice que subir precios para frenar la inflación castiga al recién llegado y no al veterano, y que el arreglo es un sumidero, no una tarifa más alta. — [JoinGG](https://www.joingg.co/blog/server-economy-design)

### Inferences

- El grifo que hay que cerrar es el de las misiones comunes y el de la XO. Subir la parcela 4 o el clic de 16 haría lo que JoinGG desaconseja y rompería el 25 = 25.
- El avance a medias se muestra; no se paga. Pagar por tronco o por piedra sería un grifo de un recurso infinito, el caso que el propio informe y JoinGG rechazan en la recompra administrativa.
- Los tres cambios que van primero, porque hoy son los que más se salen del hito de 20: la piedra (de 8 a 3 por tramo), la caza (de 10 a 4 por tramo) y la XO (de `4 × personas` en cada una de las seis rondas a 2 duros, una vez por persona y sábado).

### Gaps

- No hay una medición, en este repo ni en una fuente pública citada, de cuántos minutos tarda un jugador nuevo de Preguntones en completar un tramo. «Una tarde» se estima abajo solo como suma de tramos, y esa estimación es una propuesta, no un dato de juego.

## Qué ritmo piden las economías públicas

### Takeaway

El dinero que entra por hora y solo sale en compras de una vez acaba inflando la moneda. El premio repetible tiene que quedar por debajo del hito de progresión, el recién llegado tiene que poder comprar algo útil en la primera sesión, y no se arregla subiendo toda la tienda. Preguntones ya cumple la mitad: `/puesto` destruye duros y no recompra; le falta que el repetible no gane al dragón.

### Cited Findings

- JoinGG, 9 de septiembre de 2026: los ingresos suelen ser por jugador y por hora, y los gastos suelen ser compras de una vez. Cuando el jugador ya tiene lo que quería, deja de gastar y sigue cobrando. El ejemplo «a las seis semanas, 400 del nuevo frente a 11 millones del veterano» es una ilustración suya, no una medición. — [JoinGG](https://www.joingg.co/blog/server-economy-design)
- El mismo artículo, misma fecha: un sumidero es lo que destruye la moneda. Comprarle a otro jugador no es un sumidero. Comprar en la tienda de administración sí lo es. La renta que escala con lo que posees es el sumidero más fuerte; si no se paga, el terreno queda en suspenso, no se borra. Los cosméticos sin techo y los consumos que no sean obligatorios son los otros dos. — [JoinGG](https://www.joingg.co/blog/server-economy-design)
- El mismo artículo, misma fecha: lo que rompe la economía es la tienda de administración que compra un recurso infinito a precio fijo, el voto más rentable que jugar, la paga por estar conectado, la diaria que se acumula offline y el euro que fabrica moneda. No se corrige la inflación subiendo precios. Hay que dejar una franja barata que el recién llegado pague en la primera sesión. El precio se calcula con el ingreso real por hora, no a ojo. — [JoinGG](https://www.joingg.co/blog/server-economy-design)
- EnduranceNodes, revisado el 6 de septiembre de 2026: Vault no es la economía, es el puente. Vender al servidor un objeto a precio fijo y sin tope es una impresora. El comercio entre jugadores no necesita un grifo; una tasa pequeña convierte ese movimiento en sumidero. — [EnduranceNodes](https://endurancenodes.com/knowledge-base/plugins/economy-setup)
- EssentialsX guarda el saldo y lo registra en Vault. El defecto público de `starting-balance` es 0 y el de `max-money` es 10 billones. Preguntones los tiene en 25 y 2000. Cambiar `starting-balance` después no reescribe a quien ya tiene ficha. La guía que lo explica está fechada el 15 de octubre de 2026. — [RE:NODE](https://renode.space/blog/essentialsx-config-yml-guide); [config de serie en GitHub, rama 2.x](https://github.com/EssentialsX/Essentials/blob/2.x/Essentials/src/main/resources/config.yml); [config local](servidor/plugins/Essentials/config.yml) líneas 9-11
- El wiki de Essentials describe las mismas claves y no muestra fecha de publicación en la página leída. — [Essentials Economy](https://wiki.mc-ess.net/wiki/Essentials_Economy)
- Mundos no usa una API de quests. Ingresa con `Economy.depositPlayer` de Vault, el mismo servicio que Parcelas y la XO. No hay jar de Quests en `servidor/plugins`. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 655-659 y 777; [misiones.md](research_notes/Duros%20por%20misiones%20balanceadas/misiones.md)

### Inferences

- La regla que se aplica a Preguntones, juntando JoinGG (9 de septiembre de 2026) con el tope de tramos que ya tiene Mundos: el total repetible de un ciclo, en una sola misión de recurso común, se queda por debajo del primer hito de una vez (Nether, 16 duros, cifra actual). La XO, que es el otro repetible, se queda por debajo de un tramo. El dragón, el escombro, el Nether y el End no se suben: subirlos sería fabricar más moneda para que el repetible no los adelante.
- No se añade renta por parcela. JoinGG la recomienda el 9 de septiembre de 2026, y el informe de Preguntones ya rechazó el castigo de borrar el terreno a quien se ausenta. El sumidero que sí encaja es el que ya existe: reclamar destruye duros, `/puesto` destruye duros y el interés del préstamo se destruye.
- El 10 % de la venta entre jugadores no es ese sumidero. EnduranceNodes (6 de septiembre de 2026) y JoinGG coinciden en que el dinero que pasa a otro jugador sigue en el mundo. En Preguntones ese 10 % entra en la caja y puede volver como préstamo. Se mantiene porque el informe del barrio lo fija, no porque drene la moneda.

### Gaps

- Ninguna de las dos fuentes da una fracción del tipo «el diario debe ser el 10 % de la misión». La regla de esta nota (el repetible por debajo del hito de una vez) es la lectura aplicada a los números de Preguntones, no una cifra que JoinGG publique.
- No se ha encontrado, con fecha, una guía de la API de Quests de este servidor, porque ese plugin no está instalado. La API real es Vault más el array de Mundos.

## La tabla propuesta

### Takeaway

Se cobra solo el tramo entero, nunca el avance a medias. Un ciclo completo baja de 340 a 220 duros. Con eso, la primera parcela sigue saliendo de los 25 de entrada; la segunda pide el capítulo temprano; la tercera pide el Nether y los dos tramos de blazes; la sexta no llega hasta el cuarto ciclo. La XO paga 2, una vez, y eso es menos que el tramo más bajo (piedra, 3).

### Cited Findings

Cifras que se quedan, todas actuales, ninguna es propuesta:

- Entrada 25 y parcela 1 = 25. — [Essentials config.yml](servidor/plugins/Essentials/config.yml) línea 9; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 553-559
- Parcelas 2 a 6: 55, 95, 145, 205, 275. Suma de las seis: 800. — misma fórmula, `25 + (30 + 10*i)` acumulado
- `/puesto` mínimo 16. Bandera 40. Techo de saldo 2000. Impuesto 10 %. — [tienda-parcelas.md](research_notes/Duros%20por%20misiones%20balanceadas/tienda-parcelas.md)
- Nether 16, End 18, escombro 20, dragón 20. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 450, 678-681 y 700
- La puerta de 12 blazes sigue en 0. No es una misión de la tabla. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 508-529, según [misiones.md](research_notes/Duros%20por%20misiones%20balanceadas/misiones.md)

Propuesta nueva de `duros` por tramo. La meta y las veces no cambian. «Máximo» es `veces × duros` dentro de un ciclo.

| Id | Tramo ahora | Máximo ahora | Tramo propuesto | Máximo propuesto | De dónde sale el nuevo |
| --- | ---: | ---: | ---: | ---: | --- |
| madera | 6 | 18 | 4 | 12 | Propuesta. 4 × 3, por debajo del Nether actual (16) |
| piedra | 8 | 32 | 3 | 12 | Propuesta. 3 × 4. Es el tramo más bajo, a propósito |
| caza | 10 | 30 | 4 | 12 | Propuesta. 4 × 3 |
| comida | 6 | 12 | 4 | 8 | Propuesta. 4 × 2 |
| pesca | 8 | 16 | 4 | 8 | Propuesta. 4 × 2 |
| camino | 8 | 24 | 4 | 12 | Propuesta. 4 × 3 |
| cama | 8 | 8 | 6 | 6 | Propuesta. Una vez. Dormir no iguala a un capítulo de piedra |
| aldeano | 10 | 10 | 8 | 8 | Propuesta. Una vez |
| hierro | 12 | 24 | 8 | 16 | Propuesta. 8 × 2, igual al Nether actual, por debajo del dragón actual (20) |
| diamante | 16 | 32 | 8 | 16 | Propuesta. 8 × 2. Hoy 32 supera al dragón |
| encantar | 12 | 12 | 8 | 8 | Propuesta. Una vez |
| blaze | 14 | 28 | 8 | 16 | Propuesta. 8 × 2. La puerta de 12 blazes sigue en 0 |
| escombro | 20 | 20 | 20 | 20 | Se mantiene |
| enderman | 10 | 20 | 6 | 12 | Propuesta. 6 × 2. Se puede completar en el overworld |
| nether | 16 | 16 | 16 | 16 | Se mantiene |
| end | 18 | 18 | 18 | 18 | Se mantiene |
| dragón | 20 | 20 | 20 | 20 | Se mantiene. El «20 duros» del libro sigue siendo verdad |

Suma propuesta de un ciclo, si se hace todo y se está conectado al morir el dragón: 12+12+12+8+8+12+6+8 + 16+16+8 + 16+20+16 + 12+18+20 = 220. El 340 actual menos 220 son 120 duros menos por persona y ciclo. Esa resta es aritmética de esta tabla, no una constante del código.

La XO, propuesta nueva: 2 duros, una vez por persona y sábado, en la primera ronda que gane. Las otras rondas siguen coronando ganador y no acuñan. 2 es menor que 3, el tramo de piedra. Hoy el mínimo de quien gana las seis rondas con dos personas es 48, que ya supera al dragón (20). — cifra actual en [Bote.java](tools/xo/src/com/preguntones/xo/Bote.java) y [Cita.java](tools/xo/src/com/preguntones/xo/Cita.java)

No se paga el avance a medias. El código ya exige un tramo entero. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 766-772

Voto y tarea semanal: no existen en el repo, así que no entran en la tabla. Si más adelante hay voto, la propuesta es 2 duros una vez por ciclo, no 2 cada día: 14 días × 2 = 28, y 28 supera al dragón actual (20).

### Inferences

Aritmética con los precios actuales, gastando en parcelas en cuanto se puede y sin comprar en `/puesto`. El saldo de entrada se va en la parcela 1 y el contador de misiones empieza en 0.

- Parcela 1 (25): cero misiones. Los 25 de entrada. No cambia.
- Un clic de `/puesto` (16): la madera entera (12) más un tramo de comida (4). 12+4 = 16. Un solo tramo de madera (4) no llega.
- Parcela 2 (55): madera, piedra, caza, camino y pesca enteras. 12+12+12+12+8 = 56. Sobran 1. Hacen falta cinco misiones tempranas, no un rato de pico.
- Al cerrar lo temprano (78) y haber pagado la parcela 2, quedan 23. La bandera vale 40, así que no caben las dos. Hierro entero son 16: 23+16 = 39, a un duro de la bandera. Hierro más un tramo de 4 duros (madera, caza, comida, pesca o camino) = 43, y la bandera cabe.
- Parcela 3 (95): esos 23, más hierro 16, diamante 16 y encantar 8 (suman 40; van 63), más el Nether 16 (van 79), más los dos tramos de blaze (8+8). 79+16 = 95. El saldo queda en 0. Si los endermen (12) se cazaron antes, el Nether solo ya abre la parcela 3: 23+12+40+16 = 91, y el primer tramo de blaze (8) pasa de 95.
- Al morir el dragón en el primer ciclo, con las tres parcelas ya pagadas y sin haber tocado la tienda, quedan 70. Sale de escombro 20 + enderman 12 + End 18 + dragón 20, más los 0 que dejó la parcela 3. 70 es menor que la parcela 4 (145).
- Parcela 4 (145): hace falta otro capítulo temprano. 70 + 78 = 148. Se paga y quedan 3.
- Parcela 5 (205): el resto de ese segundo ciclo son 220 − 78 = 142. 3+142 = 145, que no llega a 205. Al terminar el segundo ciclo hay cuatro parcelas y 145 duros. El tercer ciclo entero suma 220: 145+220 = 365. La quinta cuesta 205 y quedan 160. La sexta cuesta 275, así que 160 no llega.
- Parcela 6 (275): el cuarto ciclo temprano. 160+78 = 238, todavía corto; hierro y diamante de ese ciclo son 16+16 = 32; 238+32 = 270, a 5 de la sexta; el encantar (8) la paga. Quedan 3 duros.

En resumen, con precios actuales y pagos nuevos: la parcela 2 es del primer capítulo temprano, la 3 del primer Nether, la 4 del segundo capítulo temprano, la 5 del tercer ciclo y la 6 del cuarto. Un ciclo repetible de 220 no paga las cinco parcelas que faltan tras la entrada (775). 775 / 220 = 3,52 ciclos, coherente con el recorrido de arriba porque el primer ciclo también se gasta en la 2 y la 3.

Lo que un recién llegado puede comprar, y lo que no:

- Al entrar, antes de cualquier misión: la parcela 1, o un clic de 16 y le sobran 9, no las dos cosas. 25 − 16 = 9, y 9 no es una parcela.
- Tras el capítulo temprano (78) y con la parcela 1 ya pagada: la parcela 2 y un clic de 16 (55+16 = 71, sobran 7), o la bandera (40) y no la parcela 2. No la parcela 3.
- Tras hierro, diamante, encantar, Nether y blazes: la parcela 3. Todavía no la 4.
- El veterano que repite el ciclo cobra 220, no 340. Nueve ciclos sin gastar son 1980, por debajo de 2000; el décimo chocaría con el techo. 9 × 220 = 1980. Eso son unos 126 días si el ciclo sigue en 14, no las seis semanas a once millones del ejemplo de JoinGG.

El enderman se puede hacer en el overworld. Sus 12 duros no adelantan la parcela 2 (siguen haciendo falta 55) y solo adelantan la 3 hasta la puerta del Nether, no hasta el primer día.

### Gaps

- El recorrido supone que no se compra en `/puesto` ni se paga la bandera por el camino. Quien se compra la bandera (40) retrasa una parcela: es el trade-off, y no está medido con jugadores reales.
- No se ha jugado una sesión para ver si «madera + un tramo de comida» cabe en la primera hora. La cuenta es de duros, no de minutos.

## Qué archivos y qué frases hay que tocar

### Takeaway

El premio de cada misión tiene que salir de una sola tabla y pintarse en `/ciclo`, en la página «Los duros» del libro, en el diccionario de la web y en la guía de Discord. El chat de cobro ya interpola el entero: al cambiar el constructor, la frase de después cambia sola. Los «4 duros» de la XO están copiados en el juego, la web y Discord, y quedarían mintiendo. El «20 duros» del dragón no se toca.

### Cited Findings

Frases que ya enseñan un duro y hay que cambiar porque la XO pasa a 2, una vez por sábado. Propuesta de texto, no está en el código: «2 duros, una vez por sábado. Las otras rondas no dan duros.»

- Libro, índice de la XO: «La frase, dónde ponerse y los 4 duros.» — [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) línea 345
- Libro, página de la ronda: «El último: 4 duros.» — misma clase, línea 355
- Anuncio y dos chats de apertura: «4 duros por persona, se los lleva quien gana.» — [XoPlugin.java](tools/xo/src/com/preguntones/xo/XoPlugin.java) líneas 43, 510 y 514
- Discord, anuncio del sábado: «Quien queda se lleva 4 duros.» — [mensajes.json](discord/mensajes.json) clave `anuncios.cita_sabado`
- Discord, guía: «Cada ronda son 4 duros.» — [mensajes.json](discord/mensajes.json) clave `guia`
- Web, portada y diccionario: «Cada ronda son 4 duros.» — [index.html](web/index.html) líneas 72 y 130
- Web, normas: «Quien queda se lleva 4 duros y entra en la siguiente.» — [normas.html](web/normas.html) línea 83
- La prueba de Java exige «el premio no es 4 duros por persona». — [Pruebas.java](tools/xo/src/com/preguntones/xo/Pruebas.java) línea 61
- El comentario y la constante `POR_PERSONA = 4`. — [Bote.java](tools/xo/src/com/preguntones/xo/Bote.java) líneas 3-6

Frases que hablan de misiones o de «jugar en el survival» y no dicen el premio. Hay que añadir el listado, o decir que `/ciclo` lo enseña y que el ciclo completo son 220 duros (propuesta). El 220 no está escrito en ningún sitio hoy.

- `/ciclo`, línea sin cifras. Pasa a una línea por id: nombre, progreso `hecho/meta`, tramo `cobrados/veces` y «N duros». Nether, End y dragón entran en esa lista con 16, 18 y 20. La puerta se escribe «12 blazes, 0 duros» para que no se confunda con la misión `blaze`. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 113-127
- Libro, «Los duros»: «Te los dan solos en el survival: talar, picar y más. y al vender a otro. El euro no los crea.» No cabe la tabla entera en el estilo de líneas cortas del libro; la página tiene que mandar a `/ciclo` y puede dar el suelo y el techo (3 el tramo de piedra, 20 el dragón, 220 el ciclo). — [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) líneas 190-204
- Web, diccionario, entrada de duros: «También salen de las misiones del survival y de venderle a otro jugador.» Aquí sí cabe la tabla de diecisiete filas. — [index.html](web/index.html) línea 122
- Web, entrar: «Las siguientes salen de jugar en el survival.» — [entrar.html](web/entrar.html) línea 96
- Discord, guía, pregunta «¿De dónde salen los duros?»: «El resto sale de las misiones del survival y de venderle a otro jugador.» Misma tabla, en corto. — [mensajes.json](discord/mensajes.json) clave `guia`
- Discord, normas: «Los duros salen de jugar en el survival y de venderle a otro jugador. Entras con 25.» No dice «misiones». Si la guía lleva la tabla, esta frase puede quedar como está, porque el 25 no cambia. — misma archivo, clave `normas.texto`

Frases que ya llevan el importe de después y no hay que reescribir a mano si el entero sale de la variable. Sí hay que recompilar Mundos:

- Chat de estadística: `§a` + texto + ` +` + cantidad + ` duros.` — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) línea 783
- «Has entrado al Nether. +16 duros.» y «Has entrado al End. +18 duros.» Esos 16 y 18 se quedan. — líneas 678-681 y 797
- «Habéis cazado al dragón. +20 duros.» El 20 está escrito en el `depositPlayer` y en el mensaje. Se queda. — línea 450-451
- Libro del dragón: «20 duros y Cazador.» Se queda. — [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) línea 327

Frases que no hay que cambiar, porque el número sigue siendo verdad:

- «Entras con 25», «Traes 25 duros», «La primera parcela libre de Preguntones cuesta 25 duros», «Al entrar en Preguntones recibes 25 duros». — [entrar.html](web/entrar.html) línea 96; [index.html](web/index.html) línea 122; [preguntas.yml](tools/xo/preguntas.yml) líneas 328-331; [mensajes.json](discord/mensajes.json) clave `guia`
- La escalera del libro: «1ª  25    2ª  55 / 3ª  95    4ª 145 / 5ª 205    6ª 275». — [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) línea 138
- El marcador `Duros: %preguntones_duros%` es el saldo, no el premio. No es una superficie de misión. — [config.yml de TAB](servidor/plugins/TAB/config.yml), citado en [misiones.md](research_notes/Duros%20por%20misiones%20balanceadas/misiones.md)

Lo que no es un premio de misión y no se reescribe para esta tabla:

- Karma: «+2 de karma» al comerciar o dormir, «+1 de karma» al pescar. Sale a la vez que el chat de duros y usa otra palabra. — [misiones.md](research_notes/Duros%20por%20misiones%20balanceadas/misiones.md)
- Los jars `servidor/plugins/Mundos.jar` y `servidor/plugins/Xo.jar` son el build. Se sustituyen al compilar; no se parchean a mano. `tools/xo/build/preguntas.yml` es salida de build, no la fuente.

Para que la fracción de `/ciclo` no mienta tras un reinicio, la propuesta de interfaz es guardar `bases` en `estado.yml` igual que ya se guarda `caza-blaze`. Eso no paga el tramo a medias. Hoy `guardarEstado` no escribe `bases`. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 595-652

`hechos` guarda cuántos tramos se cobraron, no cuántos duros. Bajar el premio no devuelve lo ya ingresado ni obliga a migrar saldos. El tramo ya apuntado no se vuelve a pagar.

### Inferences

- Una sola tabla en `misiones()` más tres literales (Nether, End, dragón) tiene que ser la fuente. El libro, la web y Discord no leen ese array: si se actualiza solo el Java, el «4 duros» viejo sigue en público.
- El marcador no necesita una fila por misión. Diecisiete premios no caben ahí, y el saldo ya tiene su línea.

### Gaps

- No se ha comprobado en un cliente si el `sendMessage` del cobro llega a Discord por DiscordSRV. La config lo deja abierto y Mundos no manda un webhook. — citado como hueco en [misiones.md](research_notes/Duros%20por%20misiones%20balanceadas/misiones.md)
- El libro dice «Cinco segundos» en la ronda y la web dice «5 segundos»; el código y Discord dicen 15. — [Cuaderno.java](tools/viajes/src/com/preguntones/viajes/Cuaderno.java) línea 353; [index.html](web/index.html) líneas 72 y 130; [XoPlugin.java](tools/xo/src/com/preguntones/xo/XoPlugin.java) línea 41. No es un precio. Quien edite esas frases para el 2 de la XO no debería copiar el 5.

## Dónde la tabla choca con los dos informes

### Takeaway

La tabla respeta el 25 de entrada, el 10 %, el interés destruido, la moneda única y el rechazo a la recompra y al euro. Choca con el informe de mercado en dos sitios: ese texto no quería duros en la cita del sábado, y quería ganar sobre todo vendiendo a otro jugador, con una tarea semanal que aquí no se añade. No choca con los precios del informe del barrio, que son los que ya cobra el código.

### Cited Findings

- El informe de mercado dice que el premio del sábado es un título o un aspecto, no una inyección de duros. El código paga `4 × personas` hasta seis veces. La propuesta deja 2 duros una vez. Sigue siendo una inyección, más pequeña que cualquier tramo. — [Diseño mercado servidor Preguntones.md](reports/Diseño%20mercado%20servidor%20Preguntones.md) línea 13; [Bote.java](tools/xo/src/com/preguntones/xo/Bote.java) líneas 3-12
- El mismo informe dice que se gana vendiendo en un cartel, con la tarea corta de la semana y, más adelante, con un voto por debajo de una tarde de juego. No describe la tabla de Mundos. La tarea semanal no está en el código. Esta propuesta no le pone un importe: las misiones ya son ese trabajo, y un segundo pago repetiría el grifo. — [Diseño mercado servidor Preguntones.md](reports/Diseño%20mercado%20servidor%20Preguntones.md) línea 23; [misiones.md](research_notes/Duros%20por%20misiones%20balanceadas/misiones.md)
- El 5 % y las 48 horas de ese informe son la política de subasta de Towny Reborn, no una tarifa de Preguntones. La tabla no los usa. El impuesto que se mantiene es el 10 % del código y del informe del barrio. — [Diseño mercado servidor Preguntones.md](reports/Diseño%20mercado%20servidor%20Preguntones.md) línea 23; [economia-del-barrio.md](reports/economia-del-barrio.md) líneas 25-26
- El informe del barrio dice que, si el banco se queda la parcela, el siguiente la reclama al precio normal y ese precio entra en la caja. El código cobra y no ingresa en la caja. La tabla no cambia ese cobro: dejarlo destruido es lo que hace de la parcela un sumidero. Pasarlo a la caja cumpliría el informe y devolvería el duro al circuito del préstamo. — [economia-del-barrio.md](reports/economia-del-barrio.md) línea 112; [obtencion-actual.md](research_notes/Duros%20por%20misiones%20balanceadas/obtencion-actual.md)
- El mismo informe dice que un `/pay` con la deuda vencida salda la deuda. El código cancela `/pay`. La tabla no lo toca. — [economia-del-barrio.md](reports/economia-del-barrio.md) líneas 111 y 149
- JoinGG, 9 de septiembre de 2026, pide una renta periódica desde el primer día. El informe de Preguntones suelta ese castigo: ampliar cuesta duros y el terreno abandonado se puede recuperar. La tabla no añade renta. — [JoinGG](https://www.joingg.co/blog/server-economy-design); [Diseño mercado servidor Preguntones.md](reports/Diseño%20mercado%20servidor%20Preguntones.md) línea 23
- La meta de normas de la web dice que «la cita del sábado no da duros» y el párrafo de la misma página dice que quien queda se lleva 4. Ya se contradicen hoy. Con la propuesta, la meta sigue siendo falsa mientras la XO pague 2, y el párrafo de los 4 también. — [normas.html](web/normas.html) líneas 7 y 83

### Inferences

- El choque que hay que aceptar por escrito es el de la XO: el informe quería 0 duros y el encargo pide que el repetible sea menor que un tramo, no que desaparezca. 2 cumple el encargo y deja el informe a dos duros de distancia. Ponerla a 0 cumpliría el informe y vaciaría las frases que hoy prometen un premio en duros.
- No hace falta reconciliar el 5 % de Towny con el 10 % del barrio. Son sistemas distintos, y el que cobra es el 10 %.

### Gaps

- El informe de mercado no da el número de «una tarde de juego». La comparación «2 es menos que una tarde» usa la suma propuesta de un tramo de madera, piedra, caza, comida y camino más la cama: 4+3+4+4+4+6 = 25. Es una propuesta de lectura, no una medición del informe ni del servidor.
