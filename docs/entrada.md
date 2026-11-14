# Entrada al servidor

Así se entra a Preguntones. La ventana sale antes del mundo. Cancelar no abre sesión.

## Quién entra

Discord vinculado es obligatorio para todo el mundo, cuenta nueva o ya existente.

- Cuenta nueva: primero vincula Discord. Luego inventa la contraseña. El código de recuperación llega por privado a ese Discord y solo entonces entra.
- Cuenta que ya existe, mismo sitio, sesión de AuthMe vigente y Discord ya vinculado, con el código ya enviado: entra sin pedir la contraseña.
- Cuenta que ya existe, mismo sitio, sin Discord: la ventana pide vincularlo. No pide otra vez la contraseña.
- Cuenta que ya existe, otro sitio o sin sesión: pide la contraseña. Si falta Discord, lo pide después. Si el código nunca salió por privado, lo manda y espera a que llegue.
- Quien ya vinculó y ya recibió el código no recibe otro en cada entrada. El anterior seguiría sin valer si se mandara uno nuevo.

El código no se muestra en el juego. El privado explica cómo usarlo: entrar, Recuperar Contraseña, el código y la contraseña nueva dos veces.

## La ventana

Cuenta que ya tiene contraseña y no tiene sesión abierta:

- Iniciar Sesión
- Recuperar Contraseña
- Cancelar

Vincular Discord. Esta ventana se queda abierta al pulsar un enlace o al copiar:

- **Unirme a Preguntones** abre una invitación de esa persona. Si no estaba en el Discord, al entrar la ventana sigue sola.
- Si ya estaba dentro, abre el bot, pega las cuatro cifras y la ventana sigue sola. **Comprobar** solo vuelve a mirar si el vínculo ya está.
- Las cuatro cifras se ven y se copian. No hay casilla: no se envían desde el juego.
- Quien olvidó la contraseña y no tiene Discord pulsa **Vincular Discord**. Al vincular llega el código de recuperación y el formulario. No entra hasta guardar la contraseña nueva.
- Cancelar no entra. El quinto fallo de contraseña dice «Demasiados intentos.»

Recuperar contraseña, ya con Discord:

- Código, contraseña nueva y otra vez.
- Guardar cambia la contraseña y manda un código nuevo por privado.
- Enviar código manda otro privado si no lo tiene. El anterior deja de valer.

## Qué no puede pasar

- Entrar al mundo sin Discord vinculado.
- Echar en la pantalla de conexión con «entra otra vez y conecta la cuenta en la ventana» antes de enseñar esa ventana. Ese corte era el login de Paper, que salta antes de la configuración. `srpanch0` lo vio el 13 de noviembre de 2026. Desde la 1.9 ese login ya no echa por falta de Discord: la ventana es la que espera.
- Cancelar y quedar dentro.
- Enseñar el código de recuperación en el chat del juego.

## Pendiente

- Quien ya está en el Discord pega las cuatro cifras en el privado del bot. Ninguna URL abre ese privado con el texto ya escrito: el botón abre el perfil.
- Paper avisa de que LuckPerms, DiscordSRV y Recuperar escuchan `PlayerLoginEvent`. No se puede devolver al jugador a la ventana una vez está en el mundo. El rechazo de Cancelar sigue en ese evento.
- La ventana en marcha es la 1.11.
