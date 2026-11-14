# Subida al servidor

Nada llega al servidor si la comprobación falla. `push` y `reiniciar` la lanzan solos, antes de entrar al panel.

## Qué ve la persona

Al subir o al reiniciar, primero salen las pruebas de todos los plugins, de la web, de Discord y de las reglas del barrio. Si una falla, el mensaje es «Hay fallos. No subo nada y no aplico cambios.» El servidor sigue como estaba: no se para y no se le copia ningún archivo.

Si todo pasa y el envío incluye un jar nuestro (`Parcelas.jar`, `Viajes.jar`, `Mundos.jar`, `Bingo.jar`, `Xo.jar`, `Susto.jar`, `Reinicio.jar`, `Impulso.jar`, `Recuperar.jar`, `Censura.jar`, `Karma.jar`), ese plugin se vuelve a compilar y el jar nuevo solo se copia cuando sus pruebas pasan. Luego sigue el envío de siempre.

Para lanzarla a mano, sin subir ni reiniciar:

```powershell
node scripts/comprobar.mjs
```

En GitHub, el workflow `Comprobar` hace el mismo pase en cada push y en cada pull request.

## Qué no puede pasar

- Subir o reiniciar con una prueba rota o un plugin que no compila.
- Saltarse la comprobación.
- Subir `world`, `world_nether`, `world_the_end`, `world-barrio` o un `server.properties` cuyo `level-name` no sea `world`.
- Copiar un jar al `servidor/plugins` local si las pruebas de ese plugin acaban de fallar. La compilación prueba antes de copiar.

## Qué sigue pendiente

La comprobación no arranca Paper. Un plugin que compila y pasa sus pruebas puede romperse después, al activarse en el servidor. Eso no lo ve este pase.
