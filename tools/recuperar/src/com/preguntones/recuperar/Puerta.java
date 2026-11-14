package com.preguntones.recuperar;

import fr.xephi.authme.api.v3.AuthMeApi;
import github.scarsz.discordsrv.DiscordSRV;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

/** La entrada se resuelve en la ventana, antes de cargar el mundo. */
final class Puerta {

    private static final Key ENTRAR = Key.key("preguntones", "entrar");
    private static final Key RECUPERAR = Key.key("preguntones", "recuperar");
    private static final Key CANCELAR = Key.key("preguntones", "cancelar");
    private static final Key GUARDAR = Key.key("preguntones", "guardar");
    private static final Key ENVIAR = Key.key("preguntones", "enviar");
    private static final Key VOLVER = Key.key("preguntones", "volver");
    private static final Key LISTO = Key.key("preguntones", "listo");
    private static final Key CREAR = Key.key("preguntones", "crear");
    private static final Key VINCULAR = Key.key("preguntones", "vincular");
    private static final long ESPERA_SEGUNDOS = 180;

    private enum Paso {
        LOGIN, RECUPERAR, VINCULO, CREAR
    }

    private enum TrasVincular {
        CREAR, ENTRAR, RECUPERAR
    }

    private static final class Resultado {
        final boolean entrar;
        final String expulsion;
        final boolean avisar;

        private Resultado(boolean entrar, String expulsion, boolean avisar) {
            this.entrar = entrar;
            this.expulsion = expulsion;
            this.avisar = avisar;
        }

        static Resultado entrar(boolean avisar) {
            return new Resultado(true, null, avisar);
        }

        static Resultado echar(String motivo) {
            return new Resultado(false, motivo, false);
        }

        static Resultado cortada() {
            return new Resultado(false, null, false);
        }
    }

    private final class Sesion {
        final UUID uuid;
        final String nombre;
        final Audience audiencia;
        final CompletableFuture<Resultado> espera = new CompletableFuture<>();
        Paso paso = Paso.LOGIN;
        TrasVincular trasVincular = TrasVincular.CREAR;
        String codigoDiscord;
        int fallosClave;
        boolean reintentarEnvio;
        final AtomicBoolean entrando = new AtomicBoolean();

        Sesion(UUID uuid, String nombre, Audience audiencia) {
            this.uuid = uuid;
            this.nombre = nombre;
            this.audiencia = audiencia;
        }
    }

    private final RecuperarPlugin plugin;
    private final Map<UUID, Sesion> sesiones = new ConcurrentHashMap<>();
    private final Map<String, String> expulsiones = new ConcurrentHashMap<>();
    private final Map<String, String> ips = new ConcurrentHashMap<>();
    private final Set<String> entradas = ConcurrentHashMap.newKeySet();
    private final Set<String> avisos = ConcurrentHashMap.newKeySet();
    private Object servicioSesion;
    private Method sesionValida;

    Puerta(RecuperarPlugin plugin) {
        this.plugin = plugin;
    }

    void atender(PlayerConfigurationConnection conexion) {
        if (conexion.getProfile() == null) {
            return;
        }
        UUID uuid = conexion.getProfile().getId();
        String nombre = conexion.getProfile().getName();
        if (uuid == null || nombre == null || nombre.isBlank()) {
            return;
        }
        recordarIpConexion(nombre, conexion);
        Sesion previa = sesiones.get(uuid);
        if (previa != null && !previa.espera.isDone()) {
            previa.espera.complete(Resultado.cortada());
        }
        boolean conSesion = sesionRecordada(nombre);
        if (conSesion && Correo.discordDe(uuid) != null && plugin.cuentas().entregado(nombre)) {
            entradas.add(clave(nombre));
            return;
        }
        Sesion sesion = new Sesion(uuid, nombre, conexion.getAudience());
        sesiones.put(uuid, sesion);
        try {
            if (conSesion && Correo.discordDe(uuid) == null) {
                plugin.getLogger().info("Entrada de " + nombre + ": falta Discord.");
                abrirVinculo(sesion, TrasVincular.ENTRAR, "Tu sesión sigue abierta. Falta vincular Discord.");
            } else if (conSesion) {
                plugin.getLogger().info("Entrada de " + nombre + ": mando el código.");
                mandarCodigo(sesion, plugin.cuentas().tiene(nombre));
            } else if (AuthMeApi.getInstance().isRegistered(nombre)) {
                plugin.getLogger().info("Entrada de " + nombre + ": contraseña.");
                abrirLogin(sesion, null);
            } else if (Correo.discordDe(uuid) != null) {
                plugin.getLogger().info("Entrada de " + nombre + ": crear contraseña.");
                abrirCrear(sesion, null);
            } else {
                plugin.getLogger().info("Entrada de " + nombre + ": falta Discord.");
                abrirVinculo(sesion, TrasVincular.CREAR, null);
            }
            Resultado resultado = sesion.espera.get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
            if (expulsiones.containsKey(clave(nombre))) {
                entradas.remove(clave(nombre));
            } else if (resultado.entrar && Correo.discordDe(uuid) == null) {
                expulsiones.put(clave(nombre), "Para entrar hay que vincular Discord.");
            } else if (resultado.entrar) {
                entradas.add(clave(nombre));
                if (resultado.avisar) {
                    avisos.add(clave(nombre));
                }
            } else if (resultado.expulsion != null) {
                entradas.remove(clave(nombre));
                expulsiones.put(clave(nombre), resultado.expulsion);
            }
        } catch (TimeoutException e) {
            expulsiones.put(clave(nombre), "Tiempo de espera para inicio de sesión excedido");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            expulsiones.put(clave(nombre), "Se ha cortado la entrada.");
        } catch (ExecutionException e) {
            expulsiones.put(clave(nombre), "No he podido abrir la entrada.");
        } catch (RuntimeException e) {
            plugin.getLogger().warning("No pude preparar la entrada de " + nombre);
            expulsiones.put(clave(nombre), "No he podido abrir la ventana. Entra otra vez.");
        } finally {
            plugin.invitacion().retirar(uuid);
            sesiones.remove(uuid, sesion);
            try {
                conexion.getAudience().closeDialog();
            } catch (RuntimeException ignorada) {
                // La conexión ya se cerró.
            }
        }
    }

    void cortar(UUID uuid) {
        if (uuid == null) {
            return;
        }
        Sesion sesion = sesiones.get(uuid);
        if (sesion != null && !sesion.espera.isDone()) {
            sesion.espera.complete(Resultado.cortada());
        }
    }

    void alPulsar(Key identificador, DialogResponseView vista, PlayerConfigurationConnection conexion) {
        if (conexion.getProfile() == null || conexion.getProfile().getId() == null) {
            return;
        }
        Sesion sesion = sesiones.get(conexion.getProfile().getId());
        if (sesion == null || sesion.espera.isDone()) {
            return;
        }
        String password = texto(vista, "password");
        String codigo = texto(vista, "codigo");
        String nueva = texto(vista, "nueva");
        String repetida = texto(vista, "repetida");
        if (CANCELAR.equals(identificador)) {
            cancelar(sesion.nombre);
            sesion.espera.complete(Resultado.echar("Has cancelado la entrada."));
            return;
        }
        if (VOLVER.equals(identificador)) {
            abrirLogin(sesion, null);
            return;
        }
        if (VINCULAR.equals(identificador)) {
            abrirVinculo(sesion, TrasVincular.RECUPERAR, "Vincula Discord y te mando el código para la contraseña nueva.");
            return;
        }
        if (RECUPERAR.equals(identificador)) {
            abrirRecuperar(sesion, null);
            return;
        }
        if (ENTRAR.equals(identificador)) {
            enPrincipal(() -> entrar(sesion, password));
            return;
        }
        if (LISTO.equals(identificador)) {
            enPrincipal(() -> conectar(sesion, codigo));
            return;
        }
        if (CREAR.equals(identificador)) {
            enPrincipal(() -> crear(sesion, nueva, repetida));
            return;
        }
        if (ENVIAR.equals(identificador)) {
            enPrincipal(() -> enviarCodigo(sesion));
            return;
        }
        if (GUARDAR.equals(identificador)) {
            enPrincipal(() -> guardar(sesion, codigo, nueva, repetida));
        }
    }

    void conectado(UUID uuid) {
        Sesion sesion = sesiones.get(uuid);
        if (sesion == null || sesion.paso != Paso.VINCULO || sesion.espera.isDone()) {
            return;
        }
        enPrincipal(() -> conectar(sesion, sesion.codigoDiscord));
    }

    void ocupado(UUID uuid) {
        enPrincipal(() -> {
            Sesion sesion = sesiones.get(uuid);
            if (sesion == null || sesion.espera.isDone()) {
                return;
            }
            abrirVinculo(sesion, sesion.trasVincular, "Ese Discord ya está en otra cuenta de Minecraft.");
        });
    }

    void recordarIp(String nombre, String ip) {
        if (nombre != null && ip != null && !ip.isBlank()) {
            ips.put(clave(nombre), ip);
        }
    }

    void prepararSesiones() {
        try {
            Object authme = AuthMeApi.getInstance().getPlugin();
            Field campo = authme.getClass().getDeclaredField("injector");
            campo.setAccessible(true);
            Object inyector = campo.get(authme);
            Method obtener = null;
            for (Method metodo : inyector.getClass().getMethods()) {
                if (metodo.getParameterCount() == 1 && metodo.getParameterTypes()[0] == Class.class) {
                    obtener = metodo;
                    break;
                }
            }
            if (obtener == null) {
                return;
            }
            Class<?> tipo = Class.forName("fr.xephi.authme.service.SessionService");
            servicioSesion = obtener.invoke(inyector, tipo);
            sesionValida = tipo.getMethod("hasValidSession", String.class, String.class);
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("No pude leer las sesiones. La contraseña se pedirá en cada entrada.");
        }
    }

    void cancelar(String nombre) {
        String id = clave(nombre);
        entradas.remove(id);
        expulsiones.put(id, "Has cancelado la entrada.");
    }

    /** Null si esta entrada está permitida. Si no, el motivo para echarle. */
    String rechazo(String nombre) {
        String id = clave(nombre);
        String expulsion = expulsiones.remove(id);
        if (expulsion != null) {
            entradas.remove(id);
            return expulsion;
        }
        return null;
    }

    boolean consumirEntrada(String nombre) {
        return entradas.remove(clave(nombre));
    }

    boolean consumirAviso(String nombre) {
        return avisos.remove(clave(nombre));
    }

    private void entrar(Sesion sesion, String password) {
        if (password == null || password.isBlank()) {
            abrirLogin(sesion, "Escribe la contraseña.");
            return;
        }
        boolean vale;
        try {
            vale = AuthMeApi.getInstance().checkPassword(sesion.nombre, password);
        } catch (RuntimeException e) {
            abrirLogin(sesion, "No he podido comprobar la contraseña. Prueba otra vez.");
            return;
        }
        if (!vale) {
            sesion.fallosClave++;
            if (sesion.fallosClave >= 5) {
                sesion.espera.complete(Resultado.echar("Demasiados intentos."));
                return;
            }
            abrirLogin(sesion, "Esa contraseña no es.");
            return;
        }
        if (Correo.discordDe(sesion.uuid) == null) {
            abrirVinculo(sesion, TrasVincular.ENTRAR, "Contraseña correcta. Falta vincular Discord.");
            return;
        }
        if (!plugin.cuentas().entregado(sesion.nombre)) {
            mandarCodigo(sesion, plugin.cuentas().tiene(sesion.nombre));
            return;
        }
        sesion.espera.complete(Resultado.entrar(false));
    }

    private void conectar(Sesion sesion, String escrito) {
        if (sesion.reintentarEnvio) {
            sesion.reintentarEnvio = false;
            mandarCodigo(sesion, plugin.cuentas().tiene(sesion.nombre));
            return;
        }
        if (sesion.paso == Paso.VINCULO) {
            if (!Correo.listo()) {
                abrirVinculo(sesion, sesion.trasVincular, "Discord todavía no está conectado. Espera un momento y pulsa Comprobar.");
                return;
            }
            if (Correo.discordDe(sesion.uuid) == null) {
                abrirVinculo(sesion, sesion.trasVincular,
                        "Todavía no llega. Si no estás, entra con Unirme. Si ya estás, pega las cuatro cifras en el privado del bot.");
                return;
            }
        }
        if (!sesion.entrando.compareAndSet(false, true)) {
            return;
        }
        if (sesion.trasVincular == TrasVincular.RECUPERAR) {
            mandarCodigo(sesion, plugin.cuentas().tiene(sesion.nombre), false);
            return;
        }
        if (AuthMeApi.getInstance().isRegistered(sesion.nombre)) {
            if (!plugin.cuentas().entregado(sesion.nombre)) {
                mandarCodigo(sesion, plugin.cuentas().tiene(sesion.nombre));
                return;
            }
            sesion.espera.complete(Resultado.entrar(false));
            return;
        }
        abrirCrear(sesion, null);
    }

    private void crear(Sesion sesion, String nueva, String repetida) {
        if (nueva == null || repetida == null || !nueva.equals(repetida)) {
            abrirCrear(sesion, "Las dos contraseñas no coinciden.");
            return;
        }
        String rechazo = Politica.rechazo(sesion.nombre, nueva, "");
        if (rechazo != null) {
            abrirCrear(sesion, rechazo);
            return;
        }
        boolean creada;
        try {
            creada = AuthMeApi.getInstance().registerPlayer(sesion.nombre, nueva);
        } catch (RuntimeException e) {
            creada = false;
        }
        if (!creada) {
            abrirCrear(sesion, "No he podido crear la cuenta. Prueba con otra contraseña.");
            return;
        }
        mandarCodigo(sesion, false);
    }

    private void guardar(Sesion sesion, String codigo, String nueva, String repetida) {
        String bloqueo = plugin.cuentas().espera(sesion.nombre, System.currentTimeMillis());
        if (bloqueo != null) {
            abrirRecuperar(sesion, bloqueo);
            return;
        }
        if (!plugin.cuentas().tiene(sesion.nombre)) {
            abrirRecuperar(sesion, "Esta cuenta no tiene código. Pulsa Enviar código.");
            return;
        }
        String normal = Codigo.normalizar(codigo);
        if (normal == null) {
            abrirRecuperar(sesion, "Ese no parece un código. Tiene esta forma: ABCD-EFGH");
            return;
        }
        if (!plugin.cuentas().coincide(sesion.nombre, normal)) {
            abrirRecuperar(sesion, plugin.cuentas().fallar(sesion.nombre, System.currentTimeMillis()));
            return;
        }
        if (nueva == null || repetida == null || !nueva.equals(repetida)) {
            abrirRecuperar(sesion, "Las dos contraseñas no coinciden.");
            return;
        }
        String rechazo = Politica.rechazo(sesion.nombre, nueva, normal);
        if (rechazo != null) {
            abrirRecuperar(sesion, rechazo);
            return;
        }
        plugin.cuentas().acertar(sesion.nombre);
        try {
            AuthMeApi.getInstance().changePassword(sesion.nombre, nueva);
        } catch (RuntimeException e) {
            abrirRecuperar(sesion, "No he podido usar esa contraseña. Tiene que ser distinta de tu nombre, de 5 a 30 caracteres, y no una clave muy fácil.");
            return;
        }
        try {
            plugin.cuentas().borrar(sesion.nombre);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("No pude retirar el código usado de " + sesion.nombre);
        }
        mandarCodigo(sesion, true);
    }

    private void enviarCodigo(Sesion sesion) {
        if (Correo.discordDe(sesion.uuid) == null) {
            abrirRecuperar(sesion, "Esta cuenta no tiene Discord vinculado. Sin eso no se puede recuperar la contraseña.");
            return;
        }
        String espera = plugin.cuentas().esperaReenvio(sesion.nombre, System.currentTimeMillis());
        if (espera != null) {
            abrirRecuperar(sesion, espera);
            return;
        }
        mandarCodigo(sesion, plugin.cuentas().tiene(sesion.nombre), false);
    }

    private void mandarCodigo(Sesion sesion, boolean sustituye) {
        mandarCodigo(sesion, sustituye, true);
    }

    private void mandarCodigo(Sesion sesion, boolean sustituye, boolean entrarAlAcabar) {
        String discord = Correo.discordDe(sesion.uuid);
        if (discord == null || !Correo.listo()) {
            if (entrarAlAcabar) {
                sesion.reintentarEnvio = true;
                mostrar(sesion, dialogo("Código de recuperación",
                        "No puedo escribir a tu Discord ahora. No entras hasta que esa cuenta reciba el código. Pulsa Reintentar.",
                        List.of(),
                        List.of(
                                boton("Reintentar", NamedTextColor.GREEN, LISTO),
                                boton("Cancelar", NamedTextColor.RED, CANCELAR))));
            } else {
                abrirRecuperar(sesion, "Discord todavía no está conectado. Espera un momento y pulsa Enviar código.");
            }
            return;
        }
        Cuentas.Borrador borrador = plugin.cuentas().preparar();
        String visible = Codigo.mostrar(borrador.codigo);
        if (entrarAlAcabar) {
            mostrar(sesion, dialogo("Código de recuperación",
                    "Te mando el código por privado a tu Discord. Entras cuando llegue.",
                    List.of(),
                    List.of(boton("Cancelar", NamedTextColor.RED, CANCELAR))));
        }
        Correo.enviar(discord, Aviso.texto(sesion.nombre, visible, sustituye), () -> enPrincipal(() -> {
            if (sesion.espera.isDone()) {
                return;
            }
            try {
                plugin.cuentas().confirmar(sesion.nombre, borrador, System.currentTimeMillis() + Cuentas.REENVIO_DENTRO_MS);
            } catch (RuntimeException e) {
                plugin.getLogger().warning("No pude guardar el código de " + sesion.nombre);
                if (entrarAlAcabar) {
                    sesion.reintentarEnvio = true;
                    mostrar(sesion, dialogo("Código de recuperación",
                            "El mensaje salió, pero no he podido guardarlo. Pulsa Reintentar.",
                            List.of(),
                            List.of(
                                    boton("Reintentar", NamedTextColor.GREEN, LISTO),
                                    boton("Cancelar", NamedTextColor.RED, CANCELAR))));
                }
                return;
            }
            plugin.getLogger().info("Código de " + sesion.nombre + " enviado al Discord vinculado.");
            if (entrarAlAcabar) {
                sesion.espera.complete(Resultado.entrar(true));
            } else {
                abrirRecuperar(sesion, "Te lo acabo de mandar por privado a tu Discord. Escríbelo abajo.");
            }
        }), error -> enPrincipal(() -> {
            if (sesion.espera.isDone()) {
                return;
            }
            plugin.getLogger().warning("No pude enviar el privado de " + sesion.nombre + ": " + Correo.explicar(error));
            if (entrarAlAcabar) {
                sesion.reintentarEnvio = true;
                mostrar(sesion, dialogo("Código de recuperación",
                        Correo.explicar(error) + "\n\nNo entras hasta que ese Discord reciba el código. Pulsa Reintentar.",
                        List.of(),
                        List.of(
                                boton("Reintentar", NamedTextColor.GREEN, LISTO),
                                boton("Cancelar", NamedTextColor.RED, CANCELAR))));
            } else {
                abrirRecuperar(sesion, Correo.explicar(error));
            }
        }));
    }

    private void abrirLogin(Sesion sesion, String aviso) {
        sesion.paso = Paso.LOGIN;
        String cuerpo = aviso == null
                ? "Escribe la contraseña del servidor."
                : aviso + "\n\nEscribe la contraseña del servidor.";
        mostrar(sesion, dialogo("Inicio de sesión", cuerpo,
                List.of(campo("password", "Contraseña", 30)),
                List.of(
                        boton("Iniciar Sesión", NamedTextColor.GREEN, ENTRAR),
                        boton("Recuperar Contraseña", NamedTextColor.GOLD, RECUPERAR),
                        boton("Cancelar", NamedTextColor.RED, CANCELAR))));
    }

    private void abrirRecuperar(Sesion sesion, String aviso) {
        sesion.paso = Paso.RECUPERAR;
        if (Correo.discordDe(sesion.uuid) == null) {
            mostrar(sesion, dialogo("Recuperar contraseña",
                    "Esta cuenta no tiene Discord vinculado. Sin eso no se puede recuperar la contraseña.",
                    List.of(),
                    List.of(
                            boton("Vincular Discord", NamedTextColor.GOLD, VINCULAR),
                            boton("Volver", NamedTextColor.WHITE, VOLVER),
                            boton("Cancelar", NamedTextColor.RED, CANCELAR))));
            return;
        }
        String cuerpo = aviso == null
                ? "El código está en tu Discord. Si no lo tienes, pulsa Enviar código.\nLuego escribe el código y la contraseña nueva, dos veces."
                : aviso;
        mostrar(sesion, dialogo("Recuperar contraseña", cuerpo,
                List.of(
                        campo("codigo", "Código de recuperación", 12),
                        campo("nueva", "Contraseña nueva", 30),
                        campo("repetida", "Otra vez", 30)),
                List.of(
                        boton("Guardar", NamedTextColor.GREEN, GUARDAR),
                        boton("Enviar código", NamedTextColor.GOLD, ENVIAR),
                        boton("Volver", NamedTextColor.WHITE, VOLVER))));
    }

    private void abrirCrear(Sesion sesion, String aviso) {
        sesion.paso = Paso.CREAR;
        String cuerpo = aviso == null
                ? "Inventa una contraseña solo para Preguntones. No es la de Microsoft.\nAl crearla te mando el código de recuperación por privado."
                : aviso;
        mostrar(sesion, dialogo("Contraseña del servidor", cuerpo,
                List.of(
                        campo("nueva", "Contraseña", 30),
                        campo("repetida", "Otra vez", 30)),
                List.of(
                        boton("Crear cuenta", NamedTextColor.GREEN, CREAR),
                        boton("Cancelar", NamedTextColor.RED, CANCELAR))));
    }

    private void abrirVinculo(Sesion sesion, TrasVincular despues, String aviso) {
        sesion.paso = Paso.VINCULO;
        sesion.trasVincular = despues;
        sesion.entrando.set(false);
        if (sesion.codigoDiscord == null && Correo.listo()) {
            try {
                sesion.codigoDiscord = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(sesion.uuid);
            } catch (RuntimeException e) {
                sesion.codigoDiscord = null;
            }
        }
        String lista = plugin.invitacion().urlDe(sesion.uuid);
        if (lista != null) {
            pintarVinculo(sesion, aviso, lista, true);
            return;
        }
        pintarVinculo(sesion, aviso, null, false);
        plugin.invitacion().preparar(sesion.uuid, (url, personal) -> enPrincipal(() -> {
            if (sesion.espera.isDone() || sesion.paso != Paso.VINCULO) {
                return;
            }
            pintarVinculo(sesion, aviso, url, personal);
        }));
    }

    private void pintarVinculo(Sesion sesion, String aviso, String invitacion, boolean personal) {
        boolean botListo = Correo.listo();
        String codigo = sesion.codigoDiscord == null ? "" : sesion.codigoDiscord;
        String bot = botListo ? enlaceBot() : null;
        String guia;
        boolean cifras;
        String enlace = invitacion;
        if (!botListo) {
            guia = "Discord no está conectado. Unirme abre Preguntones. El vínculo espera a que el bot vuelva. Pulsa Comprobar dentro de un momento.";
            cifras = false;
            enlace = Invitacion.PUBLICA;
        } else if (enlace == null) {
            guia = "Preparo tu invitación. Esta ventana se queda.";
            cifras = false;
        } else if (!personal) {
            guia = "No he podido hacer una invitación solo para ti. Unirme abre el Discord. Para vincular, pega las cuatro cifras en un privado al bot.";
            cifras = !codigo.isEmpty();
        } else {
            guia = "Si no estás en el Discord, pulsa Unirme. Al entrar, sigo yo.\nSi ya estás dentro, pulsa Abrir el bot, abre un mensaje privado y pega las cuatro cifras. También sigo yo.\nSi esta ventana no cambia, pulsa Comprobar.";
            cifras = !codigo.isEmpty();
        }
        Component cuerpo = Component.empty()
                .append(Component.text(aviso == null ? "Hay que vincular Discord para poder recuperar la cuenta." : aviso))
                .append(Component.newline())
                .append(Component.text(guia));
        if (cifras) {
            cuerpo = cuerpo.append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text(codigo, NamedTextColor.GOLD, TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false)
                            .clickEvent(ClickEvent.copyToClipboard(codigo))
                            .hoverEvent(HoverEvent.showText(Component.text("Copiar"))));
        }
        List<ActionButton> botones = new java.util.ArrayList<>();
        boolean preparando = botListo && invitacion == null;
        if (!preparando && enlace != null) {
            botones.add(botonEnlace("Unirme a Preguntones", enlace));
        }
        if (bot != null && !preparando) {
            botones.add(botonEnlace("Abrir el bot", bot));
        }
        if (!preparando) {
            botones.add(boton("Comprobar", NamedTextColor.WHITE, LISTO));
        }
        botones.add(boton("Cancelar", NamedTextColor.RED, CANCELAR));
        mostrar(sesion, dialogo("Conecta Discord", cuerpo, List.of(), botones, true));
    }

    private void mostrar(Sesion sesion, Dialog dialog) {
        try {
            sesion.audiencia.showDialog(dialog);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("No pude abrir la ventana de " + sesion.nombre);
            if (!sesion.espera.isDone()) {
                sesion.espera.complete(Resultado.echar("No he podido abrir la ventana. Entra otra vez."));
            }
        }
    }

    private static Dialog dialogo(String titulo, String cuerpo, List<DialogInput> campos, List<ActionButton> botones) {
        return dialogo(titulo, lineas(cuerpo), campos, botones);
    }

    private static Dialog dialogo(String titulo, Component cuerpo, List<DialogInput> campos, List<ActionButton> botones) {
        return dialogo(titulo, cuerpo, campos, botones, false);
    }

    private static Dialog dialogo(String titulo, Component cuerpo, List<DialogInput> campos, List<ActionButton> botones,
            boolean quieto) {
        var base = DialogBase.builder(Component.text(titulo, NamedTextColor.GOLD))
                .body(List.of(DialogBody.plainMessage(cuerpo, 320)))
                .inputs(campos)
                .canCloseWithEscape(false);
        if (quieto) {
            base.pause(false).afterAction(DialogBase.DialogAfterAction.NONE);
        } else {
            base.afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE);
        }
        return Dialog.create(fabrica -> fabrica.empty()
                .base(base.build())
                .type(DialogType.multiAction(botones).columns(1).build()));
    }

    private static DialogInput campo(String id, String etiqueta, int maximo) {
        return campo(id, etiqueta, maximo, "");
    }

    private static DialogInput campo(String id, String etiqueta, int maximo, String inicial) {
        var builder = DialogInput.text(id, Component.text(etiqueta)).maxLength(maximo).width(300);
        if (inicial != null && !inicial.isEmpty()) {
            builder.initial(inicial);
        }
        return builder.build();
    }

    private static ActionButton boton(String etiqueta, NamedTextColor color, Key clave) {
        return ActionButton.builder(Component.text(etiqueta, color))
                .width(280)
                .action(DialogAction.customClick(clave, null))
                .build();
    }

    private static ActionButton botonEnlace(String etiqueta, String url) {
        return ActionButton.builder(Component.text(etiqueta, NamedTextColor.AQUA))
                .width(280)
                .action(DialogAction.staticAction(ClickEvent.openUrl(url)))
                .build();
    }

    private static String enlaceBot() {
        try {
            var jda = DiscordSRV.getPlugin().getJda();
            if (jda == null || jda.getSelfUser() == null) {
                return null;
            }
            return "https://discord.com/users/" + jda.getSelfUser().getId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Component lineas(String plano) {
        Component resultado = Component.empty();
        String[] partes = plano.split("\n", -1);
        for (int i = 0; i < partes.length; i++) {
            if (i > 0) {
                resultado = resultado.append(Component.newline());
            }
            resultado = resultado.append(Component.text(partes[i]));
        }
        return resultado;
    }

    private static String texto(DialogResponseView vista, String id) {
        if (vista == null) {
            return null;
        }
        try {
            return vista.getText(id);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void enPrincipal(Runnable trabajo) {
        if (Bukkit.isPrimaryThread()) {
            trabajo.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, trabajo);
        }
    }

    private void recordarIpConexion(String nombre, Object conexion) {
        try {
            java.lang.reflect.Method metodo = conexion.getClass().getMethod("getAddress");
            Object direccion = metodo.invoke(conexion);
            if (direccion instanceof java.net.InetSocketAddress socket && socket.getAddress() != null) {
                recordarIp(nombre, socket.getAddress().getHostAddress());
            }
        } catch (ReflectiveOperationException ignorada) {
            // La IP llega en el evento anterior.
        }
    }

    private boolean sesionRecordada(String nombre) {
        var ya = Bukkit.getPlayerExact(nombre);
        if (ya != null && AuthMeApi.getInstance().isAuthenticated(ya)) {
            return true;
        }
        return AuthMeApi.getInstance().isRegistered(nombre) && sesionVigente(nombre);
    }

    private boolean sesionVigente(String nombre) {
        if (servicioSesion == null || sesionValida == null) {
            return false;
        }
        String ip = ips.get(clave(nombre));
        if (ip == null) {
            return false;
        }
        try {
            return (boolean) sesionValida.invoke(servicioSesion, clave(nombre), ip);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static String clave(String nombre) {
        return nombre.toLowerCase(Locale.ROOT);
    }
}
