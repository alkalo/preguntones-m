# Límites del diálogo antes de cargar el mundo

Hechos para un diseño posterior. El servidor de Preguntones es Paper 26.1.2. El javadoc citado de la API de diálogos es paper-api 26.3 (27 sep 2026). El código local es Recuperar 1.10. No se ha arrancado el cliente ni se ha abierto el jar de 26.1.2.

## Diferencia entre customClick y staticAction(openUrl), y qué hace openUrl con WAIT_FOR_RESPONSE

### Takeaway
Un botón tiene una sola acción. `customClick` es el único que avisa al servidor y entrega las casillas. `staticAction(ClickEvent.openUrl)` abre el navegador en el cliente y, además, ejecuta `after_action`. Con `WAIT_FOR_RESPONSE` esa URL sustituye el diálogo por la pantalla «Waiting for Response» y no cuenta como respuesta del servidor.

### Cited Findings
- `DialogAction` tiene tres formas: plantilla de comando, `CustomClickAction` y `StaticAction`. `customClick(Key, BinaryTagHolder)` crea un clic propio; cada `DialogInput.key()` se añade al NBT. `staticAction(ClickEvent)` crea una acción que ejecuta un click event fijo. — [DialogAction, paper-api 26.3](https://jd.papermc.io/paper/26.3/io/papermc/paper/registry/data/dialog/action/DialogAction.html)
- `WAIT_FOR_RESPONSE` sustituye el diálogo por una pantalla de «waiting for response». `CLOSE` cierra y vuelve a la pantalla anterior que no sea un diálogo. `NONE` deja la pantalla actual. — [DialogAfterAction, paper-api 26.3](https://jd.papermc.io/paper/26.3/io/papermc/paper/registry/data/dialog/DialogBase.DialogAfterAction.html)
- En el formato de Minecraft, `after_action` es una operación adicional después de los clics o envíos. El valor por defecto es `close`. `none` solo vale si `pause` es `false`. `wait_for_response` sustituye el diálogo por «Waiting for Response». — [Dialog, Minecraft Wiki](https://minecraft.wiki/w/Dialog)
- Desde la 1.21.6 Pre-Release 1, después de cada acción el diálogo evalúa `after_action`. Los click events del cuerpo se tratan como cualquier otra acción y también ejecutan `after_action`. `wait_for_response` existe para frenar varios envíos en una conexión lenta; el servidor debe acabar sustituyendo esa pantalla; a los 5 segundos se activa un botón Atrás que sigue como si el diálogo se hubiera cerrado y vuelve a la pantalla anterior que no sea un diálogo. — [Java Edition 1.21.6 Pre-Release 1](https://minecraft.wiki/w/Java_Edition_1.21.6_Pre-Release_1); el mismo texto está en el [changelog reproducido por Xyrios](https://xyrios.com/minecraft/tools/changelog/1.21.6-pre1)
- `open_url` abre la URL en el navegador del usuario. `custom` envía un evento al servidor y en un servidor vanilla no hace nada. Son tipos de acción distintos, cada uno en su objeto `action`. — [Dialog, Minecraft Wiki](https://minecraft.wiki/w/Dialog)
- El paquete que vuelve al servidor es `ServerboundCustomClickActionPacket`, y sale por `DialogConnectionAccess.sendCustomAction` cuando la acción es custom. En la fase de configuración el acceso de diálogo se niega a ejecutar un comando y escribe un aviso. La pantalla de espera es lo único de este sistema que cuenta ticks: enseña Atrás y luego lo habilita. El códec rechaza un diálogo que pausa el juego con un `after_action` que nunca despausa; esa comprobación corre al codificar, también en un diálogo enviado en línea durante la configuración. — [Dialogs, minecraftdocs.dev, contrastado con Minecraft 26.3](https://minecraftdocs.dev/systems/commands/dialogs)
- Paper muestra diálogos en la fase de configuración con `Audience#showDialog` y espera la respuesta dentro de `AsyncPlayerConnectionConfigureEvent`. El ejemplo que bloquea la entrada usa `customClick(Key, null)` y lee el clic en `PlayerCustomClickEvent` solo si la conexión es `PlayerConfigurationConnection`. El ejemplo de callback hace `audience instanceof Player` y sale si no lo es. — [Dialog API, PaperMC](https://docs.papermc.io/paper/dev/dialogs/)
- Los stubs locales declaran `customClick(Key, BinaryTagHolder)` y `staticAction(ClickEvent)`. El comentario dice que solo sirven para compilar y que en el servidor manda la clase de Paper. — [DialogAction.java](tools/recuperar/stubs/io/papermc/paper/registry/data/dialog/action/DialogAction.java)
- Puerta pone `afterAction(WAIT_FOR_RESPONSE)` y `canCloseWithEscape(false)` en todos sus diálogos. El botón de servidor usa `DialogAction.customClick(clave, null)`. El botón de enlace usa `DialogAction.staticAction(ClickEvent.openUrl(url))`. El código en dorado del cuerpo lleva `ClickEvent.copyToClipboard`. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 614-694
- `Builder.pause(boolean)` existe en paper-api 26.3 y se describe como pausa solo en un jugador. El stub local de `DialogBase.Builder` declara `afterAction`, `canCloseWithEscape`, `body` e `inputs`, y no declara `pause`. Puerta no llama a `pause`. — [DialogBase.Builder, paper-api 26.3](https://jd.papermc.io/paper/26.3/io/papermc/paper/registry/data/dialog/DialogBase.Builder.html); [DialogBase.java](tools/recuperar/stubs/io/papermc/paper/registry/data/dialog/DialogBase.java)

### Inferences
- Un clic `open_url` abre el navegador y luego aplica el `after_action` del diálogo. Con el `WAIT_FOR_RESPONSE` de Puerta, el cliente quita el formulario y pone la pantalla de espera. Ese clic no genera `PlayerCustomClickEvent`, así que `Puerta.alPulsar` no corre y la espera del servidor sigue abierta.
- El mismo riesgo alcanza al código dorado: es un click event del cuerpo, y el changelog de la 1.21.6 Pre-Release 1 hace que esos clics ejecuten `after_action`.
- Atrás, al activarse, devuelve a la pantalla de «uniéndose», que es la pantalla anterior. La configuración en el servidor sigue bloqueada en `espera.get` hasta 180 segundos (`ESPERA_SEGUNDOS`).
- Para que un clic de URL deje el formulario quieto hace falta `after_action` `NONE`, y el códec solo acepta `NONE` con `pause` en false. Paper 26.3 puede ponerlo con `pause(false)`. El stub con el que compila Recuperar todavía no declara ese método, y Puerta deja el valor por defecto, que es pausar.

### Gaps
- No se ha leído el método del cliente de Paper 26.1.2 ni de Minecraft 26.1. La regla «todo clic ejecuta after_action» sale del changelog de la 1.21.6 Pre-Release 1 y de la wiki actual; no apareció una nota posterior que la retirara.
- No se ha comprobado en el jar 26.1.2 si `DialogBase.Builder.pause` ya estaba. Está en el javadoc 26.3.

## Qué impide el aviso de PlayerLoginEvent: diálogo al entrar o devolver al jugador a configuración

### Takeaway
El diálogo de la primera conexión se muestra en `AsyncPlayerConnectionConfigureEvent` y ese camino sigue disponible. `PlayerGameConnection#reenterConfiguration()` no hace nada mientras LuckPerms, Recuperar o DiscordSRV escuchen `PlayerLoginEvent`: Paper no devuelve a esa persona a la fase de configuración.

### Cited Findings
- Paper 1.21.7 marcó `PlayerLoginEvent` como obsoleto para poder tener API de la fase de configuración. Esa fase está entre el login y la entrada al mundo, y es donde se puede enviar un diálogo o un paquete de recursos antes de salir de la pantalla de carga. Sustitutos: `PlayerConnectionValidateLoginEvent` para permitir o negar la entrada (corre dos veces, al entrar y al salir de la configuración, y solo hay `PlayerProfile`), `PlayerServerFullCheckEvent` para el aforo, `PlayerConnectionInitialConfigureEvent`, `AsyncPlayerConnectionConfigureEvent` y `PlayerConnectionReconfigureEvent`. Devolver a alguien ya dentro usa `PlayerGameConnection#reenterConfiguration` y `PlayerConfigurationConnection#completeReconfiguration`. — [Anuncio 1.21.7, PaperMC](https://forums.papermc.io/threads/1-21-7.1635/)
- Si hay algún listener de `PlayerLoginEvent`, `HorriblePlayerLoginEventHack.execute` marca la conexión (`handledLegacyLoginEvent = true`) y dispara el evento viejo. El aviso de log es: «You have plugins listening to the PlayerLoginEvent, this will cause re-configuration APIs to be unavailable», seguido de los nombres de los plugins. `warnReenterConfiguration` escribe que `reenterConfiguration()` exige que ningún plugin escuche `PlayerLoginEvent` y devuelve true. `PaperPlayerGameConnection.reenterConfiguration` sale en ese caso y no llama a `switchToConfig()`. — [HorriblePlayerLoginEventHack.java, commit b5e72578](https://github.com/PaperMC/Paper/blob/b5e72578/paper-server/src/main/java/io/papermc/paper/connection/HorriblePlayerLoginEventHack.java); [PaperPlayerGameConnection.java, mismo commit](https://github.com/PaperMC/Paper/blob/b5e72578/paper-server/src/main/java/io/papermc/paper/connection/PaperPlayerGameConnection.java)
- El comentario dentro del hack dice que el evento se llama durante la fase de configuración para imitar el comportamiento antiguo, y que hace falta conservar la misma referencia de `ServerPlayer`. — [HorriblePlayerLoginEventHack.java](https://github.com/PaperMC/Paper/blob/b5e72578/paper-server/src/main/java/io/papermc/paper/connection/HorriblePlayerLoginEventHack.java)
- Recuperar escucha `AsyncPlayerConnectionConfigureEvent` y ahí llama a `puerta.atender`, que muestra el diálogo y bloquea el hilo del evento con `espera.get` hasta 180 segundos. El `PlayerLoginEvent` de Recuperar solo lee `puerta.rechazo` y, si hay motivo guardado, hace `disallow`. — [RecuperarPlugin.java](tools/recuperar/src/com/preguntones/recuperar/RecuperarPlugin.java) líneas 120-140; [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 113-183
- El documento de entrada ya registra el aviso con LuckPerms, DiscordSRV y Recuperar, y deja el rechazo de Cancelar en ese evento. — [entrada.md](docs/entrada.md)
- En configuración, el cliente rechaza `run_command`. El `Player` de Bukkit todavía no existe: el ejemplo de Paper que lee casillas en juego hace un cast a `PlayerGameConnection` y a `Player`. — [minecraftdocs.dev](https://minecraftdocs.dev/systems/commands/dialogs); [Dialog API](https://docs.papermc.io/paper/dev/dialogs/)
- Mantener el hilo de `AsyncPlayerConnectionConfigureEvent` bloqueado es el mecanismo documentado para no dejar salir de la configuración. Paper tiene un informe de que ese ejemplo puede retener hilos si muchas conexiones se quedan esperando. — [Dialog API](https://docs.papermc.io/paper/dev/dialogs/); [Paper issue 13365](https://github.com/PaperMC/Paper/issues/13365)

### Inferences
- La primera conexión puede enseñar el diálogo: `atender` ya corre en la configuración inicial, muestra la ventana y no suelta al jugador hasta que el futuro termina. Ese camino no usa `reenterConfiguration`.
- Una persona que ya está en el mundo no puede volver a esa pantalla de conexión para ver el mismo diálogo. Con LuckPerms, Recuperar y DiscordSRV en `PlayerLoginEvent`, `reenterConfiguration()` registra el aviso y regresa sin cambiar de fase.
- Un botón de comando, un callback que exige `Player`, un inventario o el chat del mundo no están disponibles en esa pantalla. Lo que sí viaja es `showDialog`, `closeDialog`, `disconnect` y el `PlayerCustomClickEvent` de un `customClick`.
- El `PlayerLoginEvent` que sigue vivo puede cortar la entrada si algún listener hace `disallow`. En Recuperar 1.10 ese corte solo repite un motivo que la propia configuración haya guardado al terminar la espera, al cancelar o al fallar la ventana.

### Gaps
- El fuente del hack es el commit `b5e72578`, no un tag llamado 26.1.2. El texto del aviso coincide con el que el servidor ya ha escrito. No se ha seguido, dentro del jar 26.1.2, el orden exacto entre `PlayerLoginEvent` y `AsyncPlayerConnectionConfigureEvent`.
- No se ha medido si el hilo bloqueado 180 segundos llega a ser un problema con la gente que entra a Preguntones. El informe de Paper describe el riesgo del ejemplo oficial.

## Un botón que abra una URL y avise al servidor, y la secuencia que no deja la pantalla colgada

### Takeaway
Un botón no puede abrir una URL y avisar al servidor. La secuencia que el protocolo sostiene es: la ventana se queda abierta, los enlaces solo abren el navegador, y otro botón (`customClick`) es el que avisa. DiscordSRV ya puede adelantar esa espera cuando el vínculo llega solo.

### Cited Findings
- Cada botón de `multi_action` tiene un objeto `action`. `open_url` y `custom` son tipos distintos. — [Dialog, Minecraft Wiki](https://minecraft.wiki/w/Dialog)
- Las casillas se envían con la acción custom o con la plantilla de comando. Una acción estática no depende del valor de las casillas. — [DialogAction, paper-api 26.3](https://jd.papermc.io/paper/26.3/io/papermc/paper/registry/data/dialog/action/DialogAction.html); [Dialog, Minecraft Wiki](https://minecraft.wiki/w/Dialog)
- Puerta, en el paso de vínculo, puede poner hasta cuatro botones: «Unirme a Preguntones» (`openUrl` de la invitación), «Abrir chat del bot» (`openUrl` de `https://discord.com/users/` más el id del bot), «Conectar» (`customClick` de `preguntones:listo`) y «Cancelar» (`customClick`). El `after_action` de esa ventana es `WAIT_FOR_RESPONSE`. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 614-694
- `conectado(uuid)` solo sigue si la sesión está en el paso `VINCULO` y la espera no ha terminado. Entonces llama a `conectar` con el código ya generado. Lo dispara `AccountLinkedEvent` de DiscordSRV. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 241-249; [RecuperarPlugin.java](tools/recuperar/src/com/preguntones/recuperar/RecuperarPlugin.java) líneas 193-197
- Si al pulsar Conectar todavía no hay Discord vinculado, la misma ventana vuelve con el texto «Si ya estás en el Discord, pega el código en el chat del bot. Sigo en cuanto llegue.» — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 349-373
- El documento de entrada ya dice que un botón que solo abre una URL puede cerrar la ventana, que la invitación no avisa de una entrada que no ocurre, y que el botón del bot abre el perfil (`https://discord.com/users/`), no un privado ya escrito. — [entrada.md](docs/entrada.md)

### Inferences
- «Unirme» y «Abrir chat del bot», con el `WAIT_FOR_RESPONSE` actual, abren el navegador y cambian la pantalla de Minecraft a la de espera. El servidor no recibe ese clic, así que no puede enseñar el siguiente paso por culpa de ese botón. Quien ya estaba en el Discord no genera `AccountLinkedEvent` al abrir el perfil: nadie sustituye la pantalla de espera. A los 5 segundos Atrás devuelve a la pantalla de conexión y la espera del servidor sigue hasta el tiempo máximo.
- La secuencia que no promete un botón imposible, y que no deja esa espera a ciegas, tiene tres momentos separados. Primero la ventana permanece: `pause(false)` y `after_action` `NONE`, que es la única combinación en la que un `open_url` o un clic de copiar deja el formulario donde está. Después, dos botones de URL distintos: la invitación para quien no está en el Discord, y el perfil del bot para quien ya está. Cada uno solo abre el navegador. Al final, un `customClick` distinto —el Conectar que ya existe— es el aviso, y es el que manda el código de la casilla. Si DiscordSRV avisa del vínculo durante esa misma sesión, `conectado()` ya continúa sin ese botón.
- Mientras `after_action` siga en `WAIT_FOR_RESPONSE`, un botón de URL en esa ventana es una pantalla que se va. El servidor no tiene un segundo canal para enterarse.

### Gaps
- No hay un tipo de acción documentado que combine `open_url` y `custom` en el mismo botón. No se ha buscado un mod de cliente que lo añada; el cliente vanilla es el que entra a Preguntones.
- `pause(false)` está en el javadoc 26.3 y falta en el stub local. Sin esa llamada, `NONE` choca con la pausa por defecto y el códec puede rechazar el diálogo. No se ha probado ese rechazo contra 26.1.2.

## Texto y botones que se leen en la pantalla de conexión

### Takeaway
La pantalla de conexión usa el mismo sistema de coordenadas del GUI. Un cuerpo a 320, casillas a 300 y botones a 280 en una sola columna entran en los límites del protocolo (1–1024). Dos columnas de botones a 280 se salen de una ventana habitual. La lista de `multi_action` hace scroll; no hay una cifra oficial de cuántos botones se ven a la vez.

### Cited Findings
- El ancho de un `plain_message` va de 1 a 1024 y el defecto es 200. El de un botón de `multi_action` va de 1 a 1024 y el defecto es 150. `columns` es un entero positivo y el defecto es 2. La lista de botones es desplazable. El cuerpo va entre el título y las acciones o las casillas. La cabecera lleva el título y el botón de aviso. — [Dialog, Minecraft Wiki](https://minecraft.wiki/w/Dialog)
- Una casilla `text` es de una sola línea, ancho 1–1024 (defecto 200) y `max_length` por defecto 32. `multiline` es un objeto aparte, con altura de 1 a 512. — [Dialog, Minecraft Wiki](https://minecraft.wiki/w/Dialog)
- `DialogScreen` siempre añade un botón de aviso que abre una confirmación para desconectarse, y lo recoloca si el layout lo empuja fuera de la pantalla. El cuerpo va en un `ScrollableLayout`. — [minecraftdocs.dev](https://minecraftdocs.dev/systems/commands/dialogs); campos `warningButton` y `bodyScroll` en [DialogScreen 1.21.6, mappings.dev](https://mappings.dev/1.21.6/net/minecraft/client/gui/screens/dialog/DialogScreen.html)
- Puerta usa cuerpo a 320, casillas a 300, botones a 280 y `columns(1)`. La casilla del código tiene máximo 8; las de contraseña, 30. El diálogo de vínculo lleva ese cuerpo, una casilla y de dos a cuatro botones. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 614-694
- `canCloseWithEscape(false)` solo afecta a la tecla de escape. El botón de aviso de la cabecera es otro control. — [Dialog, Minecraft Wiki](https://minecraft.wiki/w/Dialog); [minecraftdocs.dev](https://minecraftdocs.dev/systems/commands/dialogs)
- El diálogo de la configuración es un `Screen` encima de la pantalla de unión. El paquete existe en el protocolo de configuración y viaja en línea, sin id de registro. — [minecraftdocs.dev](https://minecraftdocs.dev/systems/commands/dialogs); [Dialog API](https://docs.papermc.io/paper/dev/dialogs/)

### Inferences
- 320, 300 y 280 son anchos legales. Una columna evita el defecto de dos columnas: dos botones de 280 suman 560 px antes del hueco, y una ventana a escala 4 en 1920×1080 tiene 480 px de GUI. Una columna de 280 cabe en ese ancho.
- Cuatro botones apilados, más el texto y la casilla, entran en la lista con scroll. Los que no quepan se desplazan; el protocolo no los tira. No hay una medida publicada de cuántas líneas quedan visibles antes de ese scroll.
- El botón de aviso (desconectar) ocupa la cabecera aunque escape esté cerrado. Un diseño que cuente solo los botones de Puerta se deja ese control fuera de la cuenta.
- El texto largo se parte al ancho del `plain_message` (320 en Puerta, 200 si no se dice nada). La casilla sigue siendo una línea: un código largo no se enseña en varias líneas salvo que se añada `multiline`.

### Gaps
- No se ha medido en un cliente real, ni a escala automática ni en una ventana pequeña, cuántas líneas del texto de vínculo se ven sin scroll. No hay una cifra de Mojang del tipo «N caracteres» o «N botones» para la pantalla de conexión.
- El tamaño en píxeles de `WARNING_BUTTON_SIZE` y la altura útil del scroll no salen en las páginas de mappings consultadas; solo salen los nombres de los campos.

## La frase del 13 de noviembre de 2026 en Puerta y RecuperarPlugin

### Takeaway
La frase «Para entrar hay que vincular Discord. Entra otra vez y conecta la cuenta en la ventana.» no está en `Puerta.java` ni en `RecuperarPlugin.java` de la 1.10. El `PlayerLoginEvent` ya no decide por sí mismo que falta Discord. Sigue pudiendo echar, después de la espera, con otra frase más corta si la configuración guardó ese motivo.

### Cited Findings
- El documento de entrada prohíbe echar en la pantalla de conexión con «entra otra vez y conecta la cuenta en la ventana» antes de enseñar la ventana. Atribuye ese corte al login de Paper, que saltaba antes de la configuración. `srpanch0` lo vio el 13 de noviembre de 2026. Dice que desde la 1.9 ese login ya no echa por falta de Discord y que la ventana es la que espera. — [entrada.md](docs/entrada.md)
- Una búsqueda de «conecta la cuenta» y de «Entra otra vez y conecta» en `tools/recuperar/src` no devuelve coincidencias. `plugin.yml` declara la versión 1.10. — [plugin.yml](tools/recuperar/plugin.yml)
- `alIntentarEntrar` hace `disallow` solo cuando `puerta.rechazo(nombre)` devuelve un texto. `rechazo` devuelve y borra lo que haya en `expulsiones`, o null. — [RecuperarPlugin.java](tools/recuperar/src/com/preguntones/recuperar/RecuperarPlugin.java) líneas 135-140; [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 298-307
- El texto «Para entrar hay que vincular Discord.» se guarda en `expulsiones` en un solo sitio: después de `espera.get`, si el resultado es entrar y `Correo.discordDe(uuid)` sigue siendo null. Eso ocurre cuando la espera de la configuración ya ha terminado. Los otros motivos de esa misma salida son el tiempo de 180 segundos, el corte, «No he podido abrir la entrada.», «No he podido abrir la ventana. Entra otra vez.» y, por `cancelar`, «Has cancelado la entrada.». — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 151-173 y 292-296
- Antes de esa espera, `atender` abre la ventana de vínculo, de login, de código o de crear contraseña, o deja pasar si ya hay sesión, Discord y código entregado. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 126-150
- `Resultado.entrar` se completa al acertar la contraseña con Discord ya vinculado y el código ya entregado, al pulsar Conectar en las mismas condiciones, o al confirmar el envío del privado. Esos caminos leen `Correo.discordDe` antes de completar. El guardia de la línea 154 cubre el caso de que, al salir de la espera, el vínculo ya no esté. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java) líneas 338-346, 371-381 y 469-514

### Inferences
- El corte que `srpanch0` vio, con la frase larga y sin ventana, no lo construye el código actual de Puerta ni el de RecuperarPlugin. El login ya no contiene esa frase ni una comprobación propia de Discord.
- Sigue existiendo un expulse posterior, con la frase corta «Para entrar hay que vincular Discord.», si la configuración termina en «entrar» y para entonces el vínculo ha desaparecido. Ese texto sale en el `PlayerLoginEvent`, que es cuando la configuración ya ha soltado al jugador. Es otro momento que la frase del 13 de noviembre.

### Gaps
- No se ha recorrido el historial de git de la 1.9 para citar el commit que quitó la frase larga. La ausencia está comprobada en el fuente actual, y el documento de entrada fecha el cambio en la 1.9.
- No se ha leído el log del 13 de noviembre. La frase y la persona salen de `docs/entrada.md`.
