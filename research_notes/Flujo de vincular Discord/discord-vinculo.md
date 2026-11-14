# Vínculo de Discord en Preguntones

Hay dos códigos distintos. El de DiscordSRV son cuatro cifras: la persona lo envía al bot y eso une la cuenta de Minecraft con la de Discord. El de recuperación son ocho signos, con forma `ABCD-EFGH`: el bot lo envía por privado después del vínculo y sirve para cambiar la contraseña. Una invitación no transporta ninguno de los dos.

No se copian tokens ni otros secretos. `DiscordInviteLink` existe en la configuración local de DiscordSRV y su valor es el mismo enlace público que el código ya tiene escrito: `https://discord.gg/ZjcnvSkqsQ`.

## Cómo generan, procesan y vinculan DiscordSRV, y cuándo salta AccountLinkedEvent

### Takeaway

`generateCode` solo guarda en memoria un código de cuatro cifras para ese UUID. El vínculo se escribe en `link`, y `AccountLinkedEvent` se dispara al final de `link`, no al generar el código ni al pulsar la invitación. `process` es el camino del mensaje: limpia el texto, y solo llama a `link` si el código sigue pendiente.

### Cited Findings

- En el jar `servidor/plugins/DiscordSRV-Build-1.30.5.jar`, `AccountLinkManager` declara `generateCode(UUID)`, `process(String, String)` y `link(String, UUID)`. `javap` muestra que `AbstractFileAccountLinkManager.link` termina llamando a `afterLink`, y que `process` consulta la clave `MinecraftDiscordAccountLinkedAllowRelinkBySendingANewCode`. — [jar DiscordSRV 1.30.5](servidor/plugins/DiscordSRV-Build-1.30.5.jar); [AbstractAccountLinkManager 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/objects/managers/link/AbstractAccountLinkManager.java); [AbstractFileAccountLinkManager 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/objects/managers/link/file/AbstractFileAccountLinkManager.java)
- `generateCode` sortea un entero de 0 a 9999, lo formatea a cuatro cifras y lo repite si esa clave ya está en `linkingCodes`. El mapa es un `ConcurrentHashMap` en memoria. No borra un código anterior del mismo jugador y no escribe el vínculo. — [AbstractAccountLinkManager 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/objects/managers/link/AbstractAccountLinkManager.java)
- La ventana de entrada llama a `generateCode` una vez por sesión, solo si todavía no hay código y el cliente de Discord está conectado. El número sale en la ventana, se puede copiar y la casilla ya lo trae. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java)
- `process` quita todo lo que no sea dígito. Si ese Discord ya está vinculado y la opción de reenlace está en falso, responde que ya está vinculado y no llama a `link`. Si el código está en el mapa, llama a `link`, borra el código y devuelve el texto de éxito. Cuatro cifras desconocidas producen `UnknownCode`. Cualquier otra longitud produce `InvalidCode`. — [AbstractFileAccountLinkManager 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/objects/managers/link/file/AbstractFileAccountLinkManager.java)
- `link` rechaza un identificador de Discord vacío, desvincula ese Discord y ese UUID si ya estaban en otro par, guarda el par y llama a `afterLink`. — [AbstractFileAccountLinkManager 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/objects/managers/link/file/AbstractFileAccountLinkManager.java)
- `afterLink` construye `AccountLinkedEvent` con el usuario que devuelve `DiscordUtil.getUserById` y el UUID, y lo publica. Después puede lanzar comandos de consola, sincronizar grupos o intentar poner un rol y un apodo. El evento no es cancelable en esa clase: se publica cuando el par ya se guardó. — [AbstractAccountLinkManager 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/objects/managers/link/AbstractAccountLinkManager.java); [AccountLinkedEvent 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/api/events/AccountLinkedEvent.java)
- El mensaje privado entra en `onPrivateMessageReceived`. Ignora los mensajes del propio bot. Solo llama a `process` si `MinecraftDiscordAccountLinkedUsePM` es verdadero, y contesta en ese mismo privado con lo que `process` devuelve. Un mensaje en un canal del servidor solo se procesa si ese canal es el destino del canal de juego llamado `link`. — [DiscordAccountLinkListener 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/listeners/DiscordAccountLinkListener.java)
- En el jar, esa opción sale verdadera y el reenlace sale falso, tanto en `config/en.yml` como en `config/es.yml`. El `config.yml` local no redefine ninguna de las dos. `ForcedLanguage` está en `es`. Los textos locales dicen: código desconocido, «Los códigos de enlace tienen 4 números de largo», y «Su código de vinculación es %code%. Envíele un MP al bot». — [messages.yml](servidor/plugins/DiscordSRV/messages.yml); valores por defecto del jar 1.30.5
- `Channels` local solo tiene `global` y `broadcasts`. No hay un canal `link`. El comentario viejo del propio `config.yml` dice que Discord no hace falta para jugar; el documento de entrada actual dice lo contrario: sin Discord vinculado no se entra al mundo. — [config.yml](servidor/plugins/DiscordSRV/config.yml); [docs/entrada.md](docs/entrada.md)
- `Invitacion.vincular` llama a `link` solo si ese UUID todavía no tiene Discord. Si el Discord ya pertenece a otra cuenta, no llama a `link` y la ventana dice que ese Discord ya está en otra cuenta. Si el UUID ya estaba vinculado, no llama a `link` y, por tanto, no sale `AccountLinkedEvent`, pero sí avisa a la puerta. — [Invitacion.java](tools/recuperar/src/com/preguntones/recuperar/Invitacion.java)
- Recuperar escucha `AccountLinkedEvent`. Si el jugador o el usuario vienen nulos, no hace nada. Si llegan, avisa a la puerta y, ya dentro del mundo, manda el código de recuperación si todavía no se había entregado. — [RecuperarPlugin.java](tools/recuperar/src/com/preguntones/recuperar/RecuperarPlugin.java)
- El mismo `onGuildMemberJoin` de DiscordSRV no vincula a nadie. Si el Discord que entra ya tenía UUID, vuelve a poner el rol de vinculado y, si la sincronización de apodos está encendida, el apodo. En local esa sincronización está apagada. — [DiscordAccountLinkListener 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/listeners/DiscordAccountLinkListener.java); [synchronization.yml](servidor/plugins/DiscordSRV/synchronization.yml)

### Inferences

- El orden real del mensaje privado es: llega el texto, `process` llama a `link`, `link` guarda y dispara el evento, y solo después el bot contesta en el privado. La ventana puede avanzar por el evento aunque la contestación del bot falle.
- Como el reenlace está en falso, un Discord que ya tiene cuenta no cambia de pareja al enviar otro código. Un `link` directo, el de la invitación, sí desvincula antes las dos puntas y luego dispara el evento.
- Sin canal `link`, pegar el código en un canal del servidor no vincula. El único texto que DiscordSRV acepta solo es el mensaje privado al bot.

### Gaps

- No se desensambló `DiscordUtil.getUserById`. El evento se construye con lo que ese método devuelve. Si devuelve null, Recuperar ignora el evento. No está comprobado en el jar cuándo el usuario todavía no está en la caché.
- Existe `JdbcAccountLinkManager` en el jar. El `config.yml` local no activa una base de datos, y el `process` y el `link` citados son los del almacén en fichero. No se comparó el `link` de JDBC línea por línea.

## Qué pasa con la invitación pública, con la invitación del bot, y si el plugin sabe qué cuenta de Minecraft entró

### Takeaway

Discord no dice qué invitación usó cada persona ni conoce la cuenta de Minecraft. El plugin solo atribuye la entrada si, al entrar alguien, exactamente una invitación que él mismo creó ha subido de usos. El enlace público no está en esa lista, así que entrar por él no vincula.

### Cited Findings

- Una invitación de canal «añade a un usuario a un servidor». Crearla permite `max_age` en segundos, `max_uses` de 0 a 100, y `unique`. `unique` en verdadero significa que Discord no reutilice una invitación parecida; el propio texto lo explica como forma de crear muchas invitaciones distintas de un solo uso. No identifica a quien entra. Hace falta el permiso `CREATE_INSTANT_INVITE`. Listar las de un canal exige `MANAGE_CHANNELS`. — [Create Channel Invite](https://docs.discord.com/developers/resources/channel#create-channel-invite); [Get Channel Invites](https://docs.discord.com/developers/resources/channel#get-channel-invites)
- Los metadatos traen `uses`, «number of times this invite has been used», además de `max_uses`, `max_age`, `temporary` y `created_at`. No traen la lista de quien las usó. — [Invite Resource](https://docs.discord.com/developers/resources/invite)
- El evento Guild Member Add exige el intent `GUILD_MEMBERS`. Discord dice que se envía cuando un usuario entra y que también puede enviarse para quien ya es miembro. El campo extra documentado es solo `guild_id`; el resto es el objeto de miembro. No hay código de invitación ni identidad de Minecraft. La API no entrega «qué invitación se usó para entrar». — [Guild Member Add](https://docs.discord.com/developers/events/gateway-events#guild-member-add); [Tracking Used Invites](https://anidiots.guide/coding-guides/tracking-used-invites/)
- El comentario de `Invitacion` dice «de un solo uso». La llamada crea otra cosa: caduca a los 10 minutos, admite 5 usos y pide `unique`. El código de esa invitación se guarda junto al UUID, con usos de partida en 0. Si no hay canal o Discord rechaza la creación, el botón abre `https://discord.gg/ZjcnvSkqsQ` y no se guarda ningún código. — [Invitacion.java](tools/recuperar/src/com/preguntones/recuperar/Invitacion.java)
- Ese mismo URL es `DiscordInviteLink` en el `config.yml` local. DiscordSRV lo usa como marcador `{INVITE}` en textos como el comando de Discord. No es una invitación creada para una cuenta de Minecraft. — [config.yml](servidor/plugins/DiscordSRV/config.yml); [messages.yml](servidor/plugins/DiscordSRV/messages.yml)
- Al entrar un humano en el mismo servidor, el plugin vuelve a leer las invitaciones del canal donde las creó (`chat` o `como-jugar`; si no, el canal de sistema; si no, el primer canal de texto). Solo mira códigos que él tiene pendientes. Si exactamente uno subió de usos, en el hilo del servidor llama a `link` con el Discord de quien disparó el evento y el UUID de esa invitación. Si no sube ninguno, o sube más de uno, o la lectura falla, no vincula. — [Invitacion.java](tools/recuperar/src/com/preguntones/recuperar/Invitacion.java)
- La guía de la ventana, cuando la invitación es la personal, dice: si no estás en el Discord, entras y la ventana sigue sola; si ya estás dentro, abre el chat del bot y pega el código. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java); [docs/entrada.md](docs/entrada.md)

### Inferences

- Entrar por `https://discord.gg/ZjcnvSkqsQ` puede aumentar los usos de esa invitación pública y Discord avisará de un miembro nuevo. El plugin no tiene ese código en su mapa, así que no puede decir qué cuenta de Minecraft entró. El botón público es el recurso cuando el bot no logra crear la invitación propia.
- La invitación del bot solo relaciona Minecraft y Discord porque Preguntones apuntó el código al UUID antes de entregarla. Discord, al sumar un uso, no devuelve ese UUID. La atribución es: «en este evento de entrada, una sola invitación pendiente subió de usos; doy por hecho que quien disparó el evento es quien la gastó».
- Cinco usos y diez minutos desmienten el comentario de un solo uso. Un segundo desconocido que entre por el mismo enlace, antes de que el primero retire el código, puede ser el Discord que quede escrito si su evento es el que observa la subida. Si el UUID ya quedó vinculado, un segundo evento no vuelve a llamar a `link`, pero el registro igual dice «Discord vinculado por la invitación».
- Dos entradas casi juntas, o un Guild Member Add de alguien que ya era miembro coincidiendo con una subida real, pueden atribuir el uso a la persona equivocada o no atribuirlo a nadie. El propio Discord documenta que ese evento también puede salir para quien ya es miembro, y no adjunta la invitación.

### Gaps

- No hay una frase oficial, en las páginas citadas, que diga «abrir el enlace cuando ya se es miembro no incrementa `uses`». El contador se define como veces que la invitación se ha usado. El comportamiento del plugin no depende de esa frase: sin una subida de una invitación pendiente, no vincula.
- No se consultó el estado en vivo de `https://discord.gg/ZjcnvSkqsQ` (usos actuales, caducidad, canal). Habría hecho falta el bot o una cuenta, y no entra en esta lectura.

## Si la persona ya está en el servidor, por qué la invitación no vincula y qué tiene que hacer

### Takeaway

Abrir la invitación no une las cuentas si no hay una entrada que haga subir los usos de una invitación pendiente. Quien ya está dentro tiene que enviar al bot, por mensaje privado, las cuatro cifras que muestra Minecraft.

### Cited Findings

- La ventana lo dice así: «Si ya estás dentro, abre el chat del bot y pega el código». El botón Conectar no envía ese código a Discord. Vuelve a mirar si el UUID ya tiene Discord; si no lo tiene, repite que hay que pegar el código en el chat del bot. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java)
- El documento de entrada deja el mismo límite como pendiente: quien ya está en el Discord tiene que pegar el código en el privado del bot, porque la invitación no avisa de una entrada que no ocurre. — [docs/entrada.md](docs/entrada.md)
- El código que hay que pegar es el de `generateCode`, de cuatro cifras. El bot solo lo acepta por privado con la opción de mensajes privados encendida, que es el valor por defecto y no está tapado en el `config.yml` local. — [DiscordAccountLinkListener 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/listeners/DiscordAccountLinkListener.java); [messages.yml](servidor/plugins/DiscordSRV/messages.yml)
- DiscordSRV, al ver entrar a un miembro, no lee invitaciones ni llama a `process`. Solo restaura rol y apodo de quien ya estaba vinculado. — [DiscordAccountLinkListener 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/listeners/DiscordAccountLinkListener.java)

### Inferences

- Para quien ya es miembro, la invitación personal y la pública hacen el mismo trabajo visible: abrir el servidor. Ninguna de las dos escribe el par Minecraft–Discord. El vínculo sigue siendo el mensaje privado con las cuatro cifras.
- Conectar es un reintento de la puerta, útil si el evento tardó. No sustituye al mensaje al bot.

### Gaps

- No está medido en el cliente de Discord, con una cuenta que ya sea miembro, si el clic sobre el enlace llega a emitir Guild Member Add en este servidor concreto. La documentación dice que ese evento puede salir también para quien ya es miembro, pero no dice que abrir una invitación sea una de esas causas.

## Si una URL puede abrir el privado del bot con el código ya escrito

### Takeaway

No. El botón local abre el perfil del bot en `https://discord.com/users/` más el identificador del bot. Ningún esquema documentado abre el compositor del privado con un texto ya puesto.

### Cited Findings

- El botón «Abrir chat del bot» usa `https://discord.com/users/` seguido del identificador del propio bot. Si el cliente de Discord no está listo, el botón no se añade. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java)
- El documento de entrada lo resume: ese botón abre el perfil, no el mensaje privado ya escrito. — [docs/entrada.md](docs/entrada.md)
- En la discusión del repositorio de la API, `discord://-/users/<user_id>` abre el perfil en el cliente de escritorio de Windows. Quien responde desaconseja ese protocolo porque no vale en Linux, en la web ni en el móvil, y pide arreglar el enlace `https://discord.com/users/USER_ID`. Nadie describe un parámetro de texto. — [discord-api-docs discussion 5183](https://github.com/discord/discord-api-docs/discussions/5183)
- Un catálogo de protocolos no oficial abre un privado ya existente con `discord://-/channels/@me/<channel_id>` y un mensaje ya enviado con un tercer identificador. El identificador es el del canal, no el del usuario. El perfil va aparte, en `discord://-/users/<user_id>`. — [kedyjs/discord-protocols](https://github.com/kedyjs/discord-protocols)
- Un hilo de Reddit de 2018 explica lo mismo para la web: `https://discordapp.com/channels/@me/<DM_ID>` necesita el identificador del canal de ese privado. No se puede fabricar con el identificador de usuario. El perfil `https://discordapp.com/users/` muestra la opción de escribir, sin redactar el mensaje. — [Reddit, abrir un privado por URL](https://www.reddit.com/r/discordapp/comments/a5vjlv/is_it_possible_to_create_a_dm_to_a_user_via_a/)
- Un foro de OutSystems afirma que `discord://-/channels/@me/{USERID}` abre el privado usando el identificador de usuario. Eso choca con el catálogo de protocolos y con el hilo de Reddit, que exigen el identificador del canal. — [Foro OutSystems](https://www.outsystems.com/forums/discussion/85644/clickable-discord-icon-that-allows-you-to-directly-chat-with-the-user/); contradicho por [kedyjs/discord-protocols](https://github.com/kedyjs/discord-protocols) y por [Reddit](https://www.reddit.com/r/discordapp/comments/a5vjlv/is_it_possible_to_create_a_dm_to_a_user_via_a/)
- Crear un canal de privado por API es `POST /users/@me/channels` con el identificador del destinatario, y enviar es otro `POST` al canal. Hace falta un token de bot. Una URL del navegador no hace esas dos llamadas ni rellena el compositor. — [Stack Overflow, DM por API](https://stackoverflow.com/questions/68701824/using-discord-api-curl-to-send-a-discord-dm)

### Inferences

- Aunque el esquema `discord://` o `https://discord.com/channels/@me/<canal>` abriera un privado ya creado, Preguntones no conoce ese identificador de canal en el momento del botón, y el esquema seguiría sin aceptar el código de cuatro cifras como borrador.
- La limitación, dicha lisa: hoy no hay una URL que abra el privado del bot con el código ya escrito. La persona tiene que abrir el perfil, empezar el privado y pegar el código.

### Gaps

- Discord no publica, en las páginas de desarrolladores citadas, un esquema oficial y estable de URLs del cliente. Los protocolos salen de una discusión y de un catálogo de terceros. No apareció, en esas fuentes, un parámetro de mensaje preescrito; eso no demuestra que un cliente futuro no lo añada.

## Si el bot puede escribir por privado a quien acaba de entrar, y qué lo impide

### Takeaway

Puede intentarlo solo después de compartir servidor. Discord lo rechaza con 50278 si no hay ningún servidor en común y con 50007 si la persona no acepta ese privado. El bot no puede saltarse ninguno de los dos.

### Cited Findings

- La lista oficial de errores JSON tiene el 50007, «Cannot send messages to this user», y el 50278, «Cannot send messages to this user due to having no mutual guilds». — [Opcodes and status codes](https://docs.discord.com/developers/topics/opcodes-and-status-codes)
- El ajuste oficial se llama, en la ayuda de Discord, mensajes directos dentro de Content & Social. Por defecto, quien comparte servidor puede escribir. Apagar Direct messages bloquea los privados de quienes no son amigos en esos servidores, en todos o en uno concreto. — [Blocking & Privacy Settings](https://support.discord.com/hc/en-us/articles/217916488-Blocking-Privacy-Settings)
- Guías de bots agrupan el 50007 como ajuste de privacidad, bloqueo del bot o ausencia de servidor común, y reservan el 50278 para la falta de servidor común. Dicen que el bot no puede forzar el envío y que Discord no distingue, dentro del 50007, entre privacidad y bloqueo. — [ESN, error 50007](https://docs.esnoffical.com/fix-discord-cannot-send-messages-to-user-50007.html); [discord.js, códigos 50007 y 50278](https://discord.js.org/docs/packages/core/main/CannotSendMessagesToThisUserErrorCodes:Variable)
- Antes de compartir servidor, abrir y enviar un privado puede fallar por no haber servidores en común, por el ajuste de mensajes de miembros, o porque la persona bloqueó al bot. — [Stack Overflow, DM por API](https://stackoverflow.com/questions/68701824/using-discord-api-curl-to-send-a-discord-dm)
- Recuperar no escribe el código de cuatro cifras. Quien quiere vincular se lo envía al bot. El privado que el bot inicia es el de recuperación, y solo cuando ya conoce el Discord vinculado: abre el canal privado de ese usuario y manda el texto. Si el error contiene `50007` o «cannot send», el juego dice que hay que activar los mensajes directos de miembros del servidor. — [Correo.java](tools/recuperar/src/com/preguntones/recuperar/Correo.java)
- En el camino de la invitación, ese privado sale después de `link`, que solo se intenta en el evento de entrada. En ese momento la persona ya está en el servidor del bot. Si Discord rechaza el privado, la puerta no deja entrar: pide reintentar hasta que el Discord reciba el código. — [Invitacion.java](tools/recuperar/src/com/preguntones/recuperar/Invitacion.java); [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java)
- El `onGuildMemberJoin` de DiscordSRV no manda un privado de bienvenida ni el código de vínculo. — [DiscordAccountLinkListener 1.30.5](https://github.com/DiscordSRV/DiscordSRV/blob/v1.30.5/src/main/java/github/scarsz/discordsrv/listeners/DiscordAccountLinkListener.java)

### Inferences

- Justo después de entrar sí hay servidor en común, así que el 50278 deja de ser el obstáculo de «todavía no está en Preguntones». Siguen pudiendo parar el privado el 50007: el ajuste de mensajes de miembros del servidor, un bloqueo al bot, u otra negativa que Discord resume con el mismo código.
- Antes de la entrada, el bot no tiene un canal garantizado con esa persona. Por eso el diseño no le manda el código de cuatro cifras: la persona escribe primero al bot, y la contestación de DiscordSRV sale en el privado que ella abrió.
- El texto de `Correo.explicar` también encaja con el 50278, porque su mensaje en inglés contiene «cannot send». A quien falle por no compartir servidor, el juego le hablará del ajuste de privacidad. Es una explicación más ancha que el error.

### Gaps

- No hay una frase de Discord, en la ayuda de privacidad citada, que diga si un privado iniciado por la persona sigue aceptando la respuesta del bot cuando el ajuste está apagado. El 50007 está documentado; la excepción de «me escribió primero» no lo está en esas páginas.
- No se hizo una prueba real de envío contra una cuenta con el ajuste apagado ni contra una cuenta fuera del servidor.

## Qué contiene el mensaje privado de recuperación

### Takeaway

Es un mensaje del bot, no un correo. Lleva el código de ocho signos dos veces, dice que solo vale en esa cuenta de Discord y que el juego no lo vuelve a enseñar, y da tres pasos para usarlo si se olvida la contraseña.

### Cited Findings

- `Aviso.texto` arma el mensaje. Quita saltos de línea y comillas invertidas del nombre. Si el código sustituye a otro, empieza por «El código anterior ya no vale.» Luego: «Código de recuperación de {nombre}: {visible}.» Sigue: «Solo esta cuenta de Discord puede usarlo. En el juego no se enseña.» Los pasos son: entrar al servidor, en la ventana pulsar Recuperar Contraseña, y escribir ese código y la contraseña nueva dos veces. Cierra con «Código: {visible}». — [Aviso.java](tools/recuperar/src/com/preguntones/recuperar/Aviso.java)
- `{visible}` es el código de ocho signos partido por un guion, `ABCD-EFGH`. El alfabeto es `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`, sin 0, O, 1 ni I. El juego lo comprueba con esa forma. — [Codigo.java](tools/recuperar/src/com/preguntones/recuperar/Codigo.java); [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java)
- El envío es `openPrivateChannel` y `sendMessage` al Discord ya vinculado. No incluye el código de cuatro cifras de DiscordSRV. — [Correo.java](tools/recuperar/src/com/preguntones/recuperar/Correo.java); [Aviso.java](tools/recuperar/src/com/preguntones/recuperar/Aviso.java)
- En la puerta, la persona ve «Te mando el código por privado a tu Discord. Entras cuando llegue.» Entra cuando Discord acepta el mensaje y el código queda guardado. Si el privado falla, no entra. Dentro del mundo, el juego enseña el título «Míralo en Discord» y dice que allí no se muestra el código. El documento de entrada dice lo mismo. — [Puerta.java](tools/recuperar/src/com/preguntones/recuperar/Puerta.java); [RecuperarPlugin.java](tools/recuperar/src/com/preguntones/recuperar/RecuperarPlugin.java); [docs/entrada.md](docs/entrada.md)

### Inferences

- Quien pegue el código de recuperación en el privado del bot no vincula la cuenta. `process` se queda con los dígitos, y este código está hecho de letras y cifras de otro alfabeto. No es el código de cuatro cifras.
- El privado de recuperación solo puede salir cuando el vínculo ya existe. Sin ese Discord, la recuperación de la contraseña se detiene: la ventana lo dice y no pide el código.

### Gaps

- No se copió un privado real enviado por el bot. El texto de arriba es el que el código construye, no una captura de Discord.
