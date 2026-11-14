# Mercado de duros entre jugadores

Fecha del código: 13 de noviembre de 2026. No se ha cambiado código. Los números salen de `tools/parcelas` y, para el tamaño de un ingreso, de las misiones en `tools/mundos`. No hay `config.yml` de parcelas en el repositorio: los precios de parcela usan el defecto de `getInt` salvo que el servidor tenga otro yaml. El cofre no lee ese yaml.

La tienda oficial guarda el precio como el argumento de la fila multiplicado por `ESCALA = 4`. Aquí solo se usa ese producto para comparar con el cofre. La fórmula del catálogo y la lista de misiones no se rehacen.

## How does a chest shop get its price, what are PRECIO_MIN, PRECIO_MAX, COMPRA_MAX, and the tax, and who receives the remainder?

### Takeaway

El dueño escribe un entero de 1 a 500 duros por unidad. Una compra no puede pasar de 2.000 duros. El comprador paga el total, el dueño recibe el total menos el 10 % entero, y ese 10 % entra en la caja del barrio. Si el dueño está en mora, su parte paga antes la deuda.

### Cited Findings

- `PRECIO_MIN = 1`, `PRECIO_MAX = 500`, `COMPRA_MAX = 2000`, `IMPUESTO = 10`. — [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 5-8
- El impuesto es `total * 10 / 100` si el total es positivo, y 0 si no. La parte del dueño es `total - impuesto(total)`. Con total 4 la tasa es 0. Con total 40 la tasa es 4 y el dueño recibe 36. — [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 15-23; [Pruebas.java](tools/parcelas/src/com/preguntones/parcelas/Pruebas.java) líneas 98-99
- `leerPrecio` admite solo dígitos, como mucho 3 caracteres, y rechaza lo que queda fuera de 1..500. El diálogo dice ese mismo rango, limita el campo a 3 caracteres y guarda el entero en el hueco. Un hueco recién metido queda a precio 0 hasta ese diálogo. El comprador no puede llevarse un hueco con precio menor que 1. — [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 46-69; [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 1012-1016, 1281-1379, 864; [Pruebas.java](tools/parcelas/src/com/preguntones/parcelas/Pruebas.java) líneas 107-116
- Las pruebas fijan el tope de unidades: `tope(64, 4) = 64`, `tope(64, 100) = 20`, `tope(10, 500) = 4`. La cuenta es `min(stock, COMPRA_MAX / precio)`, división entera. — [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 35-43; [Pruebas.java](tools/parcelas/src/com/preguntones/parcelas/Pruebas.java) líneas 104-105
- En la compra, `total = unidades * precio`. Si las unidades pasan el tope, se rechaza. `cobrar` retira el total del comprador. Luego `tasa = Cuentas.impuesto(total)` y `paraDueno = total - tasa`. `banco.ingresar` abona `paraDueno` al dueño, también si está desconectado. Si ese abono falla, se restaura el género y se devuelve el total. Si sale bien, `banco.sumarCaja(tasa)` suma la tasa a la caja. El comprador ve el descuento del total y la frase de que el barrio se queda la tasa. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 919-984; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 654-679; [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 98-131
- `sumarCaja` solo incrementa el contador `caja`. No ingresa duros a un jugador. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 125-130
- Si el dueño está en mora, `ingresar` aplica antes el cobro a la deuda. Del importe, el interés baja la deuda y no entra en la caja. El capital sí entra en la caja (`caja += aCapital`). Lo que sobre después de la deuda se deposita con `abonar`. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 98-122 y 495-504; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 673-678
- El comprador en mora no puede comprar. El dueño no puede comprarse su propio género. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 924-929
- El 10 % del cofre es la constante `Cuentas.IMPUESTO`. No es la clave `precios.impuesto-porcentaje`. — [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) línea 5; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) línea 97

### Inferences

- Con precio 500 el tope es 4 unidades y el total es 2.000. Con un precio que no divide a 2.000, el total máximo es `(2000 / precio) * precio`, hasta `precio - 1` duros por debajo de 2.000. Una compra no supera 2.000.
- En una venta a un dueño sin mora, el saldo de los jugadores baja exactamente en la tasa. Esa tasa queda en la caja, no destruida del todo: la caja puede volver a salir cuando el banco recompra una parcela. El interés cobrado a un dueño en mora sí sale de la circulación sin entrar en la caja.
- Una venta de 1 a 9 duros paga tasa 0. El dueño recibe el total entero.

### Gaps

- No se ha abierto el yaml de parcelas del servidor en marcha. Si ese archivo cambia otras claves, el cofre no se entera: sus cuatro constantes están en código.
- `vigilar` también recorta subidas de saldo mientras la mora sigue. No se ha seguido si el mismo tick de la venta vuelve a cobrar lo que `ingresar` acaba de depositar. El reparto de esa venta, en el método `comprar`, es el de `ingresar` y después `sumarCaja`.

## What can be sold, what is forbidden, how many shops per plot, and what happens to stock if the shop is removed?

### Takeaway

Se puede vender casi cualquier objeto, 18 huecos por cofre y 4 cofres por parcela. Están prohibidos el propio cofre de venta y diez bloques de mando o de estructura. El género no se destruye: o se niega el retiro hasta vaciarlo, o vuelve al dueño.

### Cited Findings

- Cada puesto tiene 18 huecos. El letrero y la compra recorren `0 .. HUECOS - 1`. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 77 y 1603-1614
- `prohibido` rechaza el ítem del cofre de venta y estos materiales: `COMMAND_BLOCK`, `CHAIN_COMMAND_BLOCK`, `REPEATING_COMMAND_BLOCK`, `COMMAND_BLOCK_MINECART`, `BARRIER`, `DEBUG_STICK`, `STRUCTURE_BLOCK`, `STRUCTURE_VOID`, `JIGSAW`, `LIGHT`. El mensaje es «Eso no se vende.» No hay otra lista. Encantados, huevos, shulkers, libros y herramientas no están en esa lista. Al guardar, el hueco se escribe como `ItemStack` y el precio aparte. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 325-348, 757-758, 829-831, 993-995, 1688-1689 y 1725-1731
- La receta son cuatro lingotes de oro, un cofre y un cartel (`GCG` / `GSG`). No cuesta duros. Al colocarlo, el bloque pasa a ser un barril en la parcela del jugador, en el mundo `world`, fuera del spawn, del pasillo, de una parcela libre, de una parcela en venta, de un aval, y solo si en esa parcela hay menos de cuatro cofres. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 123-124 y 283-289; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 715-745
- El tope de cuatro está escrito como literal en `motivoVenta` (`cuantos >= 4`). `Venta.MAX_POR_PARCELA = 4` no se usa en ningún otro sitio. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 743-744; [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) línea 78
- Agacharse abre el menú del dueño solo si el UUID es el del puesto. Cualquier otro clic abre la compra. Meter y sacar género exige ser el dueño. Un amigo de la parcela no supera ese UUID. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 973-976; [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 207-226 y 750-751
- Tolvas y otros trasiegos de inventario se cancelan. Pistones y cambio de bloque del barril se cancelan. Las explosiones del barrio se quitan de la lista de bloques rotos. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 654-689; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 1143-1149
- Si el dueño rompe el barril y queda género, el evento se cancela y el cofre sigue: «Vacía el cofre antes de recogerlo.» Si está vacío, se le devuelve el cofre de venta. Si lo rompe otra persona, también se cancela: «Este cofre no es tuyo.» — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 182-204; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 951-954
- Si al cargar el chunk el bloque ya no es un barril, `recoger` copia cada hueco a la bolsa `recuperar` del dueño, borra el puesto y, si está conectado, intenta dárselo. Lo que no cabe se queda en la bolsa. Al entrar, y con `/tienda recuperar`, se vuelve a intentar. El texto de ayuda dice que ese comando devuelve el género si el cofre ya no está. — [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 119-124, 640-648, 1452-1499 y 1530-1538
- Cuando el banco se queda la parcela por impago, `recogerParcela` llama a `recoger` en cada cofre de esa parcela. El género va a la misma bolsa del dueño anterior. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 475-477; [Venta.java](tools/parcelas/src/com/preguntones/parcelas/Venta.java) líneas 170-179
- No se puede poner la parcela en venta, ofrecerla ni venderla al banco mientras quede un cofre. El mensaje es «Quita el cofre de venta antes.» Como además el retiro manual exige el cofre vacío, el género tiene que haber vuelto al inventario del dueño antes de esas ventas. El comprador de la parcela no recibe el stock. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 703-711, 1918-1925, 2013-2020 y 2176-2178

### Inferences

- El mercado de objetos no está limitado a los bloques de la tienda. Diamante, comida, herramientas, obsidiana normal y netherite caben en el cofre porque no están en `prohibido`.
- El género vive en la memoria y en `ventas.yml`, no en el inventario vanilla del barril. Por eso una rotura no lo tira al suelo: o se impide, o se agenda la devolución al dueño.

### Gaps

- No se ha comprobado, con un shulker lleno, que el serializador YAML de Bukkit conserve todo el NBT. El código guarda el `ItemStack` y no lo recorta.
- No se ha seguido qué pasa con un cofre ya colocado si la parcela pasa a aval sin llegar a mora. Colocar uno nuevo sí está prohibido.

## How do parcel offers and /parcela vender move duros, and does that duplicate or fight the chest-shop tax?

### Takeaway

Oferta y `/parcela vender` cobran el mismo 10 % de configuración, por defecto 10, sobre el precio entero de la parcela. La tasa va a la misma caja que la del cofre, pero no se suma a la del cofre: una parcela con cofre no se puede vender, y una parcela en venta no admite un cofre nuevo.

### Cited Findings

- Al arrancar, `precios.minimo` vale 20, `precios.maximo` vale 2.000, `precios.libre` vale 25 y `precios.impuesto-porcentaje` vale 10, si el yaml no dice otra cosa. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 93-97
- `/parcela ofrecer <jugador> <precio>` exige al otro conectado y un precio entre `precioMin` y `precioMax`. Guarda una sola oferta por UUID del destinatario: la nueva pisa la anterior. `/parcela vender <precio>` publica `parcela.precio` en ese mismo rango. El panel usa los mismos topes en `ponerEnVenta` y `hacerOferta`. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 388-422, 486-511, 1918-1932 y 2013-2042
- Aceptar la oferta y `/parcela comprar` hacen el mismo movimiento. `cobrar` retira el precio al comprador. `tasa = precio * impuesto / 100`. `paraDueno = precio - tasa`. `ingresar` paga `paraDueno` al dueño anterior. Si falla, se deposita el precio entero de vuelta al comprador. Si sale bien, `sumarCaja(tasa)`. `parcela.pagado` queda en el precio de esa venta. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 426-482 y 524-580
- Con el defecto del 10 %, una parcela a 20 deja tasa 2 y dueño 18. Una a 2.000 deja tasa 200 y dueño 1.800. El mínimo de 20 no cae en la franja de tasa 0, que en el cofre sí existe por debajo de 10.
- El panel enseña el mismo reparto: el vendedor recibe `precio - tasa` y el barrio se queda la tasa. — [PanelParcelas.java](tools/parcelas/src/com/preguntones/parcelas/PanelParcelas.java) líneas 108-109 y 296-297
- `comprar` avisa de la tasa en el chat. `aceptar` dice cuánto recibe el anterior y no menciona la tasa, pero también llama a `sumarCaja`. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 463 y 478-482 y 566 y 580
- `comprar` rechaza si el comprador ya tiene el tope de parcelas (`max-por-jugador`, defecto 6). `aceptar` no hace esa comprobación en el método leído. Las dos vías sí bloquean mora, aval y una casa que pasaría de 8 aldeanos. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 96, 437-450 y 538-552
- Reclamar una parcela libre con la pala retira `precioDeNuevas` y no llama a `sumarCaja`. Ese precio es 25, 55, 95, 145, 205 y 275 según las que ya se tengan: empieza en `precioLibre` y suma `30 + 10 * i` por cada una poseída. Las seis suman 800. Esos duros salen del jugador y no entran en la caja. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 347-363 y 634-640
- La venta al banco es otra vía. Paga `recompra`: la quinta parte entera de lo pagado, y al menos 1 si hubo pago. Sale de la caja. También exige quitar antes el cofre. No aplica el 10 % de la reventa entre jugadores. — [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 9 y 72-77; [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 2171-2203; [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 137-148
- El dueño en mora de una reventa entre jugadores pasa por el mismo `ingresar`: interés fuera de la caja, capital a la caja, resto al saldo. — [Banco.java](tools/parcelas/src/com/preguntones/parcelas/Banco.java) líneas 98-122 y 495-504

### Inferences

- Las dos tasas no se apilan sobre un mismo hecho. Comparten la caja y el porcentaje de hoy, y se excluyen por el estado de la parcela.
- Pueden divergir. Cambiar `precios.impuesto-porcentaje` mueve la parcela y deja el cofre en 10. Hoy los dos caminos dan el mismo 10 % porque el defecto y la constante coinciden.
- Una reventa a 2.000 mueve 2,5 veces los 800 de las seis parcelas nuevas. La quinta parte de esos 2.000, 400, es lo que el banco tendría que desembolsar después si esa venta quedó como `pagado`.

### Gaps

- No se ha leído el `config.yml` del proceso de GamzyHost. El rango 20..2.000 y el 10 % son los defectos del código, no una captura del disco del servidor.
- No se ha buscado si algún otro comando publica `parcela.precio` fuera de `vender` y `ponerEnVenta`.

## Can a player undercut the official shop, or do the caps make player trade pointless or a money printer?

### Takeaway

El jugador puede vender por debajo de la tienda cuando el lote oficial sale a más de 1 duro por unidad. No puede bajar de la tienda en los lotes que salen a menos de 1. Ni el suelo ni el techo fabrican duros: la tienda solo cobra, y el cofre solo transfiere. El techo de 500 sí deja un cartel muy por encima de la tienda, y el tope de 2.000 deja que un clic mueva más que todas las misiones repetibles juntas.

### Cited Findings

- La tienda cobra el precio guardado en la ficha y retira esos duros. No hay un camino que compre objetos al jugador. La bandera son 40 duros, fuera del catálogo. La ficha de aldeano son 40. — [Tienda.java](tools/parcelas/src/com/preguntones/parcelas/Tienda.java) líneas 82-92 y 133-156; [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 12-13; [Aldeanos.java](tools/parcelas/src/com/preguntones/parcelas/Aldeanos.java) línea 42
- `add` guarda `precio * 4`. El comentario del archivo fija dos resultados: 32 troncos de roble son 96 duros; sus tablones salen a 24. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 8-9, 15 y 610-611
- Esos resultados y otras filas, ya multiplicadas por 4, comparadas con el suelo de 1 y el techo de 500:

| Objeto | Lote | Duros del lote | Por unidad | Frente al cofre (1..500) |
| --- | ---: | ---: | ---: | --- |
| Tronco de roble | 32 | 96 | 3 | 1 y 2 quedan por debajo. 500 es 166 veces la unidad |
| Tablones de roble | 32 | 24 | 0,75 | 1 ya es más caro que la unidad oficial |
| Antorchas | 32 | 24 | 0,75 | igual que los tablones |
| Tierra | 32 | 32 | 1 | se empata a 1; no se puede bajar |
| Camino de tierra | 32 | 24 | 0,75 | 1 queda por encima |
| Arena | 32 | 48 | 1,5 | 1 queda por debajo |
| Roca | 32 | 64 | 2 | 1 queda por debajo |
| Lana | 16 | 64 | 4 | 1, 2 y 3 quedan por debajo |
| Obsidiana llorosa | 16 | 112 | 7 | 1..6 quedan por debajo. 500 es unas 71 veces |
| Observador | 2 | 96 | 48 | se puede vender más barato. 500 es unas 10 veces |
| Tolva | 1 | 80 | 80 | se puede vender más barato |
| Puerta de hierro | 1 | 96 | 96 | se puede vender más barato |
| Cabeza de dragón | 1 | 192 | 192 | se puede vender más barato. 500 es 2,6 veces |

- Fuentes de esas filas: tronco y tablones en `madera` con argumento 24 y `paso(24 / 4) = 6`, más el comentario de las líneas 8-9. Antorchas, tierra, camino, arena, roca, lana, obsidiana llorosa, observador, tolva, puerta y cabeza están en las llamadas citadas, y el precio de tienda es el argumento por 4. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) líneas 72, 100, 123, 177, 281-293, 332, 434, 446, 449, 530-544 y 578-611
- Entre las filas de cantidad 1, el argumento más alto del archivo es 48, la cabeza de dragón. Su precio guardado es 192. No aparece `DIAMOND`, `WHEAT`, `NETHERITE`, `ANCIENT_DEBRIS`, `ELYTRA` ni `BEACON`. `OBSIDIAN` solo aparece como `CRYING_OBSIDIAN`. Esos ausentes solo tienen precio de jugador, de 1 a 500. — [Catalogo.java](tools/parcelas/src/com/preguntones/parcelas/Catalogo.java) línea 434
- Comprar en la tienda y revender en el cofre no crea saldo. La tienda ya destruyó el precio del lote. El cofre mueve duros del segundo jugador al vendedor y aparta la tasa. Revender por debajo de la unidad oficial, después de haber pagado esa unidad, deja menos duros de los que costó el lote. Revender por encima solo ingresa duros si alguien paga el cofre teniendo la tienda abierta al mismo objeto.
- Una misión puede ingresar, en una sola llamada, `duros * veces`. Lo más alto de esa tabla es 32: piedra, 8 duros por hasta 4 veces, y diamante, 16 duros por hasta 2 veces. El escombro paga 20 una vez. Sumando `duros * veces` de las 14 misiones repetibles salen 286. Nether una vez son 16 y End una vez son 18. — [MundosPlugin.java](tools/mundos/src/com/preguntones/mundos/MundosPlugin.java) líneas 730-753 y 818-835
- La sexta parcela vacía cuesta 275. Las seis cuestan 800. Una compra de cofre puede mover 2.000, que es más que esas dos cifras y unas siete veces los 286 de las misiones repetibles. — [ParcelasPlugin.java](tools/parcelas/src/com/preguntones/parcelas/ParcelasPlugin.java) líneas 634-640; [Cuentas.java](tools/parcelas/src/com/preguntones/parcelas/Cuentas.java) líneas 6-8

### Inferences

- El suelo de 1 deja útil el cofre para el tronco (3), la roca (2), la lana (4), la arena (1,5) y la obsidiana llorosa (7). Lo deja inútil como descuento para tablones, antorchas y camino de tierra, porque la tienda ya los reparte por debajo de 1 duro la unidad. La tierra solo se empata.
- El techo de 500 no obliga a nadie a pagar de más mientras el objeto siga en la tienda. Tampoco impide el cartel caro. Para lo que no está en la tienda, 500 y el tope de 2.000 son el mercado entero: cuatro unidades a 500 duros mueven 2.000 en un clic, sin que la tienda ofrezca un precio rival.
- No es una impresora. No hay recompra administrativa de objetos. El riesgo del techo actual es un clic demasiado grande y un precio por unidad sin ancla, no la creación de saldo.

### Gaps

- No se ha calculado la unidad de cada fila del catálogo. La tabla es una muestra, más el máximo de cantidad 1 encontrado en el archivo. Puede haber una unidad suelta, en un lote de varias piezas, por encima de 192; no cambia que 500 queda por encima de las filas citadas.
- «Más barato que la tienda» compara duro por objeto. La tienda vende el lote cerrado. Un jugador puede vender una sola tolva a 79 frente a 80, y también puede no tener stock. Esa diferencia de cantidad no está medida en partidas reales.

## What chest-shop and parcel-trade limits keep renewable goods cheaper than the official shop, hard blocks convenient at the official shop, and a single sale inside what the economy can absorb?

### Takeaway

Conviene dejar el suelo en 1 y el 10 %, bajar el precio por unidad de 500 a 48, la compra de 2.000 a 96 y la reventa de parcela de 2.000 a 275. Así el tronco se puede vender a 1 o 2, la tolva y la cabeza de dragón siguen más cómodas en la tienda, y un clic de cofre no mueve más duros que un lote oficial de 32 troncos.

### Cited Findings

- Valores que hoy aplica el código, con el defecto de parcela porque no hay yaml en el repo:

| Límite | Dónde | Valor actual |
| --- | --- | ---: |
| Precio por unidad del cofre | `PRECIO_MIN` .. `PRECIO_MAX` | 1 .. 500 |
| Duros de una compra de cofre | `COMPRA_MAX` | 2.000 |
| Tasa del cofre | `IMPUESTO` | 10 % entero, constante |
| Reventa y oferta de parcela | `precios.minimo` .. `precios.maximo` | 20 .. 2.000 |
| Tasa de esa reventa | `precios.impuesto-porcentaje` | 10 % entero, yaml |
| Parcela vacía más cara | `precioDeNuevas` con 5 ya poseídas | 275 |
| Escalera de las seis | suma de 25, 55, 95, 145, 205 y 275 | 800 |
| Mayor ingreso de una llamada de misión | piedra 8×4 y diamante 16×2 | 32 |
| Lote de 32 troncos de roble | catálogo | 96 |
| Tolva, puerta de hierro, cabeza de dragón | catálogo, una unidad | 80, 96 y 192 |

- Esas cifras están en las citas de las secciones anteriores. La propuesta de abajo no cambia la fórmula del catálogo ni las misiones.

### Inferences

Propuesta, no es el código de hoy:

| Límite | Actual | Propuesto | Por qué |
| --- | ---: | ---: | --- |
| `PRECIO_MIN` | 1 | 1 | El duro no tiene decimales. A 1, el tronco (3), la roca (2), la lana (4) y la arena (1,5) sí pueden salir más baratos que la tienda. Tablones y antorchas, a 0,75 la unidad, siguen más baratos en la tienda. Subir el suelo a 2 impediría abaratar la roca, que sale a 2 |
| `PRECIO_MAX` | 500 | 48 | 48 es la unidad oficial del observador (96 / 2). Empata ese bloque y queda por debajo de la tolva (80), la puerta de hierro (96) y la cabeza de dragón (192): quien quiera el precio publicado y el stock infinito sigue en la tienda. Quien tenga el bloque puede dejarlo más barato, hasta 48. 48 también es una vez y media el mayor ingreso de una misión (32), no 166 veces un tronco |
| `COMPRA_MAX` | 2.000 | 96 | 96 es lo que la tienda cobra por 32 troncos. Un clic no mueve más que ese lote renovable. A 1 duro la unidad caben 96 objetos en la cuenta, y el hueco real de un stack se queda en 64, así que una venta de trigo a 1 duro mueve 64. A 48 duros la unidad el tope es 2 y el total es 96. 96 es un tercio de la sexta parcela (275) y muy por debajo de los 800 de las seis. 2.000 es 2,5 veces esos 800 y unas siete veces los 286 de las misiones repetibles |
| Mínimo de parcela | 20 | 20 | Ya está por debajo de la primera parcela vacía (25). A 20 la tasa es 2 y el vendedor recibe 18, menos de lo que costó reclamarla. No es una impresora de parcelas |
| Máximo de parcela | 2.000 | 275 | 275 es el precio de la sexta parcela vacía, lo más caro que el propio servidor cobra por un solar vacío. Una reventa no pide más que eso. A 275 la tasa del 10 % es 27 y el vendedor recibe 248. La recompra posterior del banco, un quinto, sería 55, la misma cifra que si esa sexta se hubiera reclamado vacía y no se revendiera a 2.000 (un quinto de 2.000 son 400, que sí vacía la caja) |
| Tasa del cofre y de la parcela | 10 y 10, por vías distintas | 10 en las dos, leídas de la misma clave | No hace falta subirla. El problema no es el porcentaje, es que el cofre ignora `precios.impuesto-porcentaje`. Mientras las dos valgan 10, no se pisan: el cofre y la parcela en venta no conviven. Si el yaml cambia y el cofre no, una vía se queda atrás |

- A 48 duros la unidad, un tronco se puede anunciar por encima de los 3 de la tienda. Eso no obliga a comprarlo: la tienda sigue vendiendo el lote a 96. El suelo de 1 es lo que mantiene el descuento. El techo de 48 es lo que impide que un objeto ausente de la tienda, o una cabeza de dragón, valga 500 en un solo hueco.
- La tasa de las ventas de cofre por debajo de 10 duros sigue en 0 con la división actual. Con el tope de 96 eso es una venta pequeña, menor que una misión de piedra. No hace falta cambiar la fórmula para cerrar el clic grande.

### Gaps

- El tope propuesto es por clic. Dieciocho huecos seguidos, a 96 duros, moverían 1.728. El código no tiene un tope diario. Esta nota no propone uno: la pregunta era la venta suelta.
- 48 duros deja el escombro, cuyo aviso de misión es 20, con un precio de jugador por encima del aviso y muy por debajo del techo viejo de 500. No hay precio oficial de escombro, diamante o comida con el que afinar más. Si más adelante la tienda vendiera esos objetos, el 48 habría que volver a medirlo contra esa unidad.
- No se ha simulado una sesión con saldos reales. La escala usa los ingresos y los precios que el código ya cobra, no una partida jugada.
