package com.preguntones.xo;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class XoPlugin extends JavaPlugin implements Listener {
    private static final String ANUNCIOS = "1570176102255829195";
    private static final long REUNIR_MS = 5 * 60 * 1000L;
    private static final long SESION_MS = 60 * 60 * 1000L;
    private static final int FRASE_SEGUNDOS = 15;
    private static final String AVISO =
        "La XO empieza a las 21:30 y sigue con rondas hasta las 22:30, hora peninsular. Entra en play.preguntones.es, pulsa Escape y entra por el botón dorado de abajo, Panel Preguntones. X es falso y O es verdadero. Tienes 15 segundos para colocarte. 8 duros al que gana la ronda.";
    private static final String ABIERTA =
        "La XO está abierta. Entra desde Escape, el botón dorado de abajo: Panel Preguntones. play.preguntones.es";

    private static XoPlugin instancia;
    private String ultimo = "";
    private String aviso = "";
    private boolean avisoDiscord;
    private String jugada = "";
    private String cancelada = "";
    private boolean enCurso;

    private final Set<UUID> vivos = new HashSet<>();
    private final Set<UUID> participantes = new HashSet<>();
    private final Set<UUID> retenidos = new HashSet<>();
    private final Map<UUID, Estado> estados = new HashMap<>();
    private int bote = 1;
    private boolean barridoHecho;
    private final List<Pregunta> mazo = new ArrayList<>();
    private Economy economia;
    private BossBar barra;
    private boolean abierta;
    private boolean enPregunta;
    private boolean empezo;
    private boolean discordAbierta;
    private boolean canalAusente;
    private int espera;
    private int segundos;
    private int cursor;
    private boolean verdad;
    private String frase = "";
    private long abiertaEn;
    private long reunirHasta;
    private long proximoAviso;
    private long proximoAbierta;
    private int huecoHecho = -1;
    private boolean cerrarAlAcabar;
    private String textoAbierta = ABIERTA;
    private long manualDesde;
    private long manualHasta;
    private long mazoPedidoEn;
    private long mazoEsperaHasta;
    private long proximoPedido;
    private volatile boolean pedidoEnviado;
    private String pedidoClave = "";
    private boolean esperaAvisada;

    public static boolean estaAbierta() {
        return instancia != null && instancia.abierta;
    }

    public static String ultimoGanador() {
        return instancia == null || instancia.ultimo == null ? "" : instancia.ultimo;
    }

    public static String textoCita() {
        if (instancia == null) {
            return "";
        }
        if (instancia.abierta) {
            return "abierta";
        }
        ZonedDateTime ahora = ZonedDateTime.now(Cita.MADRID);
        ZonedDateTime cita = Cita.proxima(ahora);
        if (cita.toLocalDate().equals(ahora.toLocalDate())) {
            return "hoy 21:30";
        }
        if (cita.toLocalDate().equals(ahora.toLocalDate().plusDays(1))) {
            return "mañana";
        }
        long dias = java.time.temporal.ChronoUnit.DAYS.between(ahora.toLocalDate(), cita.toLocalDate());
        return "en " + Math.max(1, dias) + " d";
    }

    public static void llevar(Player jugador) {
        if (instancia != null) {
            instancia.entrar(jugador);
        }
    }

    @Override
    public void onEnable() {
        instancia = this;
        saveDefaultConfig();
        saveResource("preguntas.yml", false);
        cargarPreguntas();
        cargarUltimo();
        cargarCita();
        prepararEconomia();
        crearMundo();
        construir();
        barra = Bukkit.createBossBar("XO", BarColor.YELLOW, BarStyle.SOLID);
        barra.setVisible(false);
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("xo") != null) {
            getCommand("xo").setExecutor((sender, command, label, args) -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("empezar")) {
                    if (!sender.hasPermission("xo.admin")) {
                        sender.sendMessage("§eLa XO se abre sola el sábado a las 21:30.");
                        return true;
                    }
                    if (abierta) {
                        sender.sendMessage("§eLa XO ya está abierta.");
                        return true;
                    }
                    long ahoraMs = System.currentTimeMillis();
                    manualDesde = ahoraMs;
                    manualHasta = ahoraMs + SESION_MS;
                    guardarCita();
                    if (esperaMazo()) {
                        pedirMazo("manual-" + manualDesde);
                        sender.sendMessage("§ePreparando el mazo. La sesión se abre cuando esté listo.");
                        return true;
                    }
                    abrir(false);
                    sender.sendMessage("§eHas abierto una sesión de la XO. Dura una hora. La primera ronda sale en cinco minutos.");
                    return true;
                }
                if (args.length > 0 && args[0].equalsIgnoreCase("parar")) {
                    if (!sender.hasPermission("xo.admin")) {
                        sender.sendMessage("§eLa XO se abre sola el sábado a las 21:30.");
                        return true;
                    }
                    if (manualHasta > 0) {
                        if (abierta) {
                            cerrar("La sesión de la XO se ha cerrado.", "La sesión de la XO se ha cerrado.", false);
                        } else {
                            manualDesde = 0;
                            manualHasta = 0;
                            guardarCita();
                        }
                        sender.sendMessage("§eSesión cerrada. El sábado a las 21:30 se abre sola.");
                        return true;
                    }
                    cancelar();
                    return true;
                }
                if (sender.hasPermission("xo.admin")) {
                    sender.sendMessage("§e/xo empezar abre una sesión de una hora. /xo parar la cierra, o cancela el sábado si no hay sesión a mano.");
                    return true;
                }
                sender.sendMessage("§eLa XO se abre sola el sábado a las 21:30. Entra desde el Panel Preguntones cuando esté abierta.");
                return true;
            });
            getCommand("xo").setTabCompleter((sender, command, alias, args) -> {
                if (!sender.hasPermission("xo.admin") || args.length != 1) {
                    return java.util.List.of();
                }
                String escrito = args[0].toLowerCase(java.util.Locale.ROOT);
                java.util.List<String> opciones = new ArrayList<>();
                if ("empezar".startsWith(escrito)) {
                    opciones.add("empezar");
                }
                if ("parar".startsWith(escrito)) {
                    opciones.add("parar");
                }
                return opciones;
            });
        }
        getServer().getScheduler().runTaskTimer(this, this::pulso, 20L, 20L);
        getLogger().info("XO lista, con " + mazo.size() + " frases. Se abre sola el sábado a las 21:30. /xo empezar abre una sesión de una hora.");
    }

    @Override
    public void onDisable() {
        for (Player jugador : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            boolean enXo = jugador.getWorld().getName().equals("xo");
            if (!enXo && jugador.getGameMode() != GameMode.ADVENTURE && !retenidos.contains(jugador.getUniqueId())) {
                continue;
            }
            soltar(jugador);
            if (enXo) {
                llevarAlBarrio(jugador);
            }
        }
        guardarCita();
        if (barra != null) {
            barra.removeAll();
        }
        instancia = null;
    }

    private boolean esperaMazo() {
        return !getConfig().getString("mazo-webhook", "").isBlank();
    }

    private boolean mazoReciente() {
        if (mazoPedidoEn <= 0) {
            return false;
        }
        java.io.File archivo = new java.io.File(getDataFolder(), "preguntas.yml");
        return archivo.isFile() && archivo.lastModified() > mazoPedidoEn;
    }

    private boolean listoParaAbrir(String clave) {
        if (!esperaMazo()) {
            return true;
        }
        if (!clave.equals(pedidoClave)) {
            pedidoClave = clave;
            pedidoEnviado = false;
            mazoPedidoEn = 0;
            esperaAvisada = false;
        }
        if (mazoReciente()) {
            return true;
        }
        long ahora = System.currentTimeMillis();
        if (mazoPedidoEn > 0 && ahora > mazoEsperaHasta) {
            if (!esperaAvisada) {
                esperaAvisada = true;
                getLogger().warning("El mazo nuevo no llegó. La XO sigue con el anterior.");
            }
            return true;
        }
        pedirMazo(clave);
        return false;
    }

    private void pedirMazo(String clave) {
        if (!clave.equals(pedidoClave)) {
            pedidoClave = clave;
            pedidoEnviado = false;
            mazoPedidoEn = 0;
            esperaAvisada = false;
        }
        long ahora = System.currentTimeMillis();
        if (mazoPedidoEn == 0) {
            mazoPedidoEn = ahora;
            mazoEsperaHasta = ahora + 20 * 60 * 1000L;
            getLogger().info("Pidiendo el mazo. Sin aviso hasta que llegue.");
        }
        if (pedidoEnviado || ahora < proximoPedido) {
            return;
        }
        proximoPedido = ahora + 30_000L;
        String url = getConfig().getString("mazo-webhook", "");
        String autorizacion = getConfig().getString("mazo-webhook-autorizacion", "");
        getServer().getScheduler().runTaskAsynchronously(this, () -> pedidoEnviado = postMazo(url, autorizacion));
    }

    private boolean postMazo(String url, String autorizacion) {
        try {
            java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"evento\":\"xo\"}"));
            if (autorizacion != null && !autorizacion.isBlank()) {
                builder.header("Authorization", autorizacion);
            }
            java.net.http.HttpResponse<String> respuesta = java.net.http.HttpClient.newHttpClient()
                .send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() >= 200 && respuesta.statusCode() < 300) {
                getLogger().info("Pedido de mazo enviado.");
                return true;
            }
            getLogger().warning("El pedido de mazo respondió " + respuesta.statusCode() + ".");
            return false;
        } catch (Exception e) {
            getLogger().warning("No pude pedir el mazo.");
            return false;
        }
    }

    private void cargarPreguntas() {
        java.io.File archivo = new java.io.File(getDataFolder(), "preguntas.yml");
        List<Pregunta> nuevas = new ArrayList<>();
        if (archivo.isFile()) {
            var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(archivo);
            List<?> preguntas = yaml.getList("preguntas");
            if (preguntas != null) {
                for (Object item : preguntas) {
                    String texto = "";
                    boolean verdad = false;
                    if (item instanceof org.bukkit.configuration.ConfigurationSection seccion) {
                        texto = seccion.getString("texto", "");
                        verdad = seccion.getBoolean("verdad");
                    } else if (item instanceof java.util.Map<?, ?> mapa) {
                        Object crudo = mapa.get("texto");
                        Object cierto = mapa.get("verdad");
                        texto = crudo == null ? "" : String.valueOf(crudo);
                        verdad = Boolean.TRUE.equals(cierto) || "true".equals(String.valueOf(cierto));
                    }
                    if (texto.isBlank()) {
                        continue;
                    }
                    nuevas.add(new Pregunta(texto, verdad));
                }
            }
        }
        if (nuevas.isEmpty()) {
            getLogger().warning("El mazo de la XO ha llegado vacío. Sigo con el que ya tenía.");
            return;
        }
        mazo.clear();
        mazo.addAll(nuevas);
    }

    private void crearMundo() {
        if (Bukkit.getWorld("xo") == null) {
            WorldCreator creador = new WorldCreator("xo");
            creador.type(WorldType.FLAT);
            creador.generateStructures(false);
            creador.generatorSettings("{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biome\":\"minecraft:the_void\"}");
            Bukkit.createWorld(creador);
        }
        World mundo = Bukkit.getWorld("xo");
        if (mundo != null) {
            mundo.setPVP(false);
        }
    }

    private void construir() {
        World mundo = Bukkit.getWorld("xo");
        if (mundo == null) {
            return;
        }
        rellenar(mundo, -24, 64, -22, 24, 64, 16, Material.POLISHED_DEEPSLATE);
        rellenar(mundo, -20, 64, -18, -2, 64, -2, Material.RED_CONCRETE);
        rellenar(mundo, 2, 64, -18, 20, 64, -2, Material.LIME_CONCRETE);
        rellenar(mundo, -8, 64, 2, 8, 64, 12, Material.SMOOTH_QUARTZ);
        rellenar(mundo, -24, 65, -22, 24, 67, -22, Material.BARRIER);
        rellenar(mundo, -24, 65, 16, 24, 67, 16, Material.BARRIER);
        rellenar(mundo, -24, 65, -22, -24, 67, 16, Material.BARRIER);
        rellenar(mundo, 24, 65, -22, 24, 67, 16, Material.BARRIER);
        rellenar(mundo, -10, 68, 14, 10, 68, 18, Material.GLASS);
        rellenar(mundo, -11, 69, 13, 11, 73, 13, Material.BARRIER);
        rellenar(mundo, -11, 69, 19, 11, 73, 19, Material.BARRIER);
        rellenar(mundo, -11, 69, 13, -11, 73, 19, Material.BARRIER);
        rellenar(mundo, 11, 69, 13, 11, 73, 19, Material.BARRIER);
        rellenar(mundo, -11, 74, 13, 11, 74, 19, Material.BARRIER);
    }

    private void rellenar(World mundo, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int xa = Math.min(x1, x2);
        int xb = Math.max(x1, x2);
        int ya = Math.min(y1, y2);
        int yb = Math.max(y1, y2);
        int za = Math.min(z1, z2);
        int zb = Math.max(z1, z2);
        for (int x = xa; x <= xb; x++) {
            for (int y = ya; y <= yb; y++) {
                for (int z = za; z <= zb; z++) {
                    mundo.getBlockAt(x, y, z).setType(material, false);
                }
            }
        }
    }

    private void pulso() {
        reloj();
        if (!barridoHecho) {
            barridoHecho = true;
            for (Player jugador : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                revisarModo(jugador);
            }
        }
        if (abierta && !discordAbierta && System.currentTimeMillis() >= proximoAbierta) {
            if (enviarDiscord(textoAbierta)) {
                discordAbierta = true;
                getLogger().info("Aviso de apertura enviado a Discord.");
            } else {
                proximoAbierta = System.currentTimeMillis() + 30_000L;
            }
        }
        if (!abierta) {
            return;
        }
        World xo = Bukkit.getWorld("xo");
        if (xo == null) {
            return;
        }
        for (Player jugador : xo.getPlayers()) {
            if (espectador(jugador)) {
                if (fueraDeGrada(jugador.getLocation())) {
                    jugador.setFallDistance(0f);
                    jugador.teleport(grada(xo, jugador.getLocation().getYaw(), jugador.getLocation().getPitch()));
                    jugador.setFallDistance(0f);
                }
                continue;
            }
            if (jugador.getLocation().getY() < 60) {
                jugador.setFallDistance(0f);
                jugador.teleport(new Location(xo, 0.5, 65, 8.5, 180, 0));
                jugador.setFallDistance(0f);
            }
        }
        ZonedDateTime madrid = ZonedDateTime.now(Cita.MADRID);
        boolean manual = sesionManual();
        int minuto = manual
            ? (int) ((System.currentTimeMillis() - manualDesde) / 60_000L)
            : madrid.getHour() * 60 + madrid.getMinute();
        boolean cierre = manual
            ? System.currentTimeMillis() >= manualHasta
            : madrid.getDayOfWeek() == DayOfWeek.SATURDAY && Cita.esCierre(minuto);
        if (cierre && !enPregunta) {
            cerrarPorTiempo();
            return;
        }
        if (cierre) {
            cerrarAlAcabar = true;
        }
        if (System.currentTimeMillis() < reunirHasta) {
            cuentaParaEmpezar();
            return;
        }
        if (enPregunta) {
            segundos--;
            barra.setProgress(Math.max(0, segundos / (double) FRASE_SEGUNDOS));
            barra.setTitle(frase + " · " + segundos);
            if (segundos > 0) {
                for (Player jugador : xo.getPlayers()) {
                    jugador.sendActionBar("§e" + frase);
                }
                return;
            }
            revelar(xo);
            return;
        }
        if (empezo) {
            long enPie = xo.getPlayers().stream().filter(p -> vivos.contains(p.getUniqueId())).count();
            if (enPie < 2) {
                acabarRonda(xo);
                return;
            }
            int hueco = huecoActual(minuto, manual);
            if (hueco > huecoHecho) {
                huecoHecho = hueco;
            }
            return;
        }
        int hueco = huecoActual(minuto, manual);
        if (hueco > 0 && hueco != huecoHecho) {
            long enEntrada = xo.getPlayers().stream().filter(p -> vivos.contains(p.getUniqueId())).count();
            huecoHecho = hueco;
            if (enEntrada >= 2) {
                empezarPregunta(xo);
            } else {
                for (Player jugador : xo.getPlayers()) {
                    jugador.sendMessage("§eHacen falta dos personas. Esta ronda se salta.");
                }
            }
            return;
        }
        int siguiente = manual ? Cita.siguienteHuecoSesion(minuto) : Cita.siguienteHueco(minuto);
        if (siguiente > 0) {
            ZonedDateTime cuando = manual
                ? java.time.Instant.ofEpochMilli(manualDesde + siguiente * 60_000L).atZone(Cita.MADRID)
                : madrid.withHour(siguiente / 60).withMinute(siguiente % 60).withSecond(0).withNano(0);
            long seg = Math.max(0, java.time.Duration.between(madrid, cuando).toSeconds());
            barra.setTitle("Siguiente en " + reloj(seg));
            barra.setProgress(Math.min(1.0, Math.max(0.0, seg / 600.0)));
        }
    }

    private boolean sesionManual() {
        return manualHasta > 0;
    }

    private int huecoActual(int minuto, boolean manual) {
        return manual ? Cita.ultimoHuecoSesion(minuto) : Cita.ultimoHueco(minuto);
    }

    private void cerrarPorTiempo() {
        cerrar(manualHasta > 0 ? "La sesión de la XO se ha cerrado." : "La XO se ha cerrado.", textoCierreDiscord());
    }

    private void reloj() {
        long ahoraMs = System.currentTimeMillis();
        if (manualHasta > ahoraMs) {
            if (!abierta && listoParaAbrir("manual-" + manualDesde)) {
                abrir(ahoraMs >= manualDesde + REUNIR_MS);
            }
            return;
        }
        if (manualHasta > 0 && !abierta) {
            manualDesde = 0;
            manualHasta = 0;
            guardarCita();
        }
        ZonedDateTime madrid = ZonedDateTime.now(Cita.MADRID);
        String hoy = madrid.toLocalDate().toString();
        if (hoy.equals(cancelada)) {
            if (abierta || enCurso) {
                cerrar("Este sábado no hay XO.", null);
            }
            return;
        }
        Cita.Momento momento = Cita.de(madrid);
        if ((momento.aviso() || momento.apertura() || momento.recuperar()) && !listoParaAbrir(hoy)) {
            return;
        }
        if (momento.aviso() && !hoy.equals(aviso)) {
            Bukkit.broadcastMessage("§6La XO se abre enseguida. §fEntra desde Escape, el botón dorado de abajo: Panel Preguntones.");
            aviso = hoy;
            avisoDiscord = false;
            proximoAviso = 0;
            guardarCita();
        }
        if (momento.aviso() && hoy.equals(aviso) && !avisoDiscord && System.currentTimeMillis() >= proximoAviso) {
            if (enviarDiscord(AVISO)) {
                avisoDiscord = true;
                guardarCita();
                getLogger().info("Aviso de la XO enviado a Discord.");
            } else {
                proximoAviso = System.currentTimeMillis() + 30_000L;
            }
        }
        boolean dentro = momento.apertura() || momento.recuperar();
        boolean toca = !hoy.equals(jugada) && (momento.apertura() || (enCurso && momento.recuperar())
            || (esperaMazo() && mazoPedidoEn > 0 && dentro));
        if (!abierta && toca) {
            abrir(enCurso);
        }
        if (enCurso && !abierta && !momento.recuperar() && !momento.apertura()) {
            enCurso = false;
            guardarCita();
            getLogger().info("La XO se quedó a medias. El sábado a las 21:30 se abre sola.");
        }
    }

    private void abrir(boolean recuperar) {
        if (abierta) {
            return;
        }
        cargarPreguntas();
        boolean manual = sesionManual();
        abierta = true;
        enPregunta = false;
        empezo = false;
        discordAbierta = false;
        proximoAbierta = 0;
        cerrarAlAcabar = false;
        huecoHecho = -1;
        espera = 0;
        vivos.clear();
        participantes.clear();
        cursor = 0;
        abiertaEn = manual ? manualDesde : System.currentTimeMillis();
        reunirHasta = recuperar ? System.currentTimeMillis() : abiertaEn + REUNIR_MS;
        if (manual) {
            textoAbierta = recuperar
                ? "La XO sigue abierta durante esta sesión. Entra desde Escape, el botón dorado de abajo: Panel Preguntones. play.preguntones.es"
                : "La XO está abierta durante una hora. Entra desde Escape, el botón dorado de abajo: Panel Preguntones. play.preguntones.es";
        } else {
            textoAbierta = recuperar
                ? "La XO sigue abierta hasta las 22:30. Entra desde Escape, el botón dorado de abajo: Panel Preguntones. play.preguntones.es"
                : ABIERTA;
        }
        if (!manual) {
            enCurso = true;
        }
        guardarCita();
        if (manual) {
            Bukkit.broadcastMessage(recuperar
                ? "§6La XO sigue abierta. §fEntra desde Escape, el botón dorado de abajo: Panel Preguntones."
                : "§6La XO está abierta. §fEntra desde Escape, el botón dorado de abajo: Panel Preguntones. 8 duros al que gana la ronda. Esta sesión dura una hora.");
        } else {
            Bukkit.broadcastMessage(recuperar
                ? "§6La XO sigue abierta hasta las 22:30. §fEntra desde Escape, el botón dorado de abajo: Panel Preguntones."
                : "§6La XO está abierta. §fEntra desde Escape, el botón dorado de abajo: Panel Preguntones. 8 duros al que gana la ronda.");
        }
        barra.setVisible(true);
        if (System.currentTimeMillis() < reunirHasta) {
            cuentaParaEmpezar();
        } else {
            barra.setTitle("XO abierta");
            barra.setProgress(1);
        }
        World xo = Bukkit.getWorld("xo");
        if (xo != null) {
            for (Player jugador : new ArrayList<>(xo.getPlayers())) {
                entrar(jugador);
            }
        }
        getLogger().info((manual ? "La XO se ha abierto a mano." : "La XO se ha abierto sola.") + " Mazo: " + mazo.size() + " frases.");
    }

    private void cancelar() {
        ZonedDateTime madrid = ZonedDateTime.now(Cita.MADRID);
        cancelada = Cita.sabadoPendiente(madrid).toString();
        guardarCita();
        String discord = "Este sábado no hay XO. El sábado que viene se abre sola a las 21:30.";
        if (abierta) {
            cerrar("Este sábado no hay XO.", discord);
            return;
        }
        enviarDiscord(discord);
        getLogger().info("Este sábado no hay XO.");
        if (madrid.getDayOfWeek() == DayOfWeek.SATURDAY) {
            Bukkit.broadcastMessage("§eEste sábado no hay XO.");
        }
    }

    private void cerrar(String texto, String discord) {
        cerrar(texto, discord, true);
    }

    private void cerrar(String texto, String discord, boolean reanudarSabado) {
        abierta = false;
        enPregunta = false;
        empezo = false;
        vivos.clear();
        participantes.clear();
        barra.removeAll();
        barra.setVisible(false);
        World xo = Bukkit.getWorld("xo");
        World barrio = Bukkit.getWorld("world");
        if (xo != null && barrio != null) {
            Location spawn = new Location(barrio, 0.5, 65, 12.5, 180, 0);
            for (Player jugador : new ArrayList<>(xo.getPlayers())) {
                soltar(jugador);
                jugador.setFallDistance(0f);
                jugador.teleport(spawn);
                jugador.setFallDistance(0f);
            }
        }
        for (UUID id : new ArrayList<>(retenidos)) {
            Player jugador = Bukkit.getPlayer(id);
            if (jugador != null) {
                soltar(jugador);
            }
        }
        if (manualHasta > 0) {
            manualDesde = 0;
            manualHasta = 0;
            if (reanudarSabado && sabadoPendiente()) {
                enCurso = true;
            }
        } else {
            jugada = ZonedDateTime.now(Cita.MADRID).toLocalDate().toString();
            enCurso = false;
        }
        guardarCita();
        if (texto != null && !texto.isEmpty()) {
            Bukkit.broadcastMessage("§e" + texto);
            getLogger().info(texto);
        }
        if (discord != null) {
            enviarDiscord(discord);
        }
    }

    public void entrar(Player jugador) {
        World xo = Bukkit.getWorld("xo");
        if (xo == null) {
            jugador.sendMessage("§cLa arena no está lista.");
            return;
        }
        jugador.setFallDistance(0f);
        retener(jugador);
        if (abierta && empezo) {
            jugador.teleport(grada(xo));
            jugador.setFallDistance(0f);
            barra.addPlayer(jugador);
            jugador.sendMessage("§eLa ronda ya ha empezado. Puedes mirar. La siguiente te toca.");
            return;
        }
        jugador.teleport(new Location(xo, 0.5, 65, 8.5, 180, 0));
        jugador.setFallDistance(0f);
        if (abierta) {
            vivos.add(jugador.getUniqueId());
            participantes.add(jugador.getUniqueId());
        }
        barra.addPlayer(jugador);
        jugador.sendMessage("§6X es falso. O es verdadero. §fLéelo y ponte en la letra. Los golpes no cuentan.");
        if (abierta && System.currentTimeMillis() < reunirHasta) {
            cuentaParaEmpezar();
        }
    }

    private void cuentaParaEmpezar() {
        long seg = Math.max(0, (reunirHasta - System.currentTimeMillis()) / 1000);
        barra.setTitle("Empieza en " + reloj(seg));
        barra.setProgress(Math.min(1.0, Math.max(0.0, seg / 300.0)));
        barra.setVisible(true);
    }

    private void empezarPregunta(World xo) {
        if (mazo.isEmpty()) {
            cerrar("No quedan frases.", "No quedan frases. La XO se cierra.");
            return;
        }
        if (cursor >= mazo.size()) {
            cursor = 0;
        }
        if (!empezo) {
            bote = Math.max(vivos.size(), 1);
        }
        Pregunta pregunta = mazo.get(cursor++);
        frase = pregunta.texto;
        verdad = pregunta.verdad;
        enPregunta = true;
        empezo = true;
        segundos = FRASE_SEGUNDOS;
        espera = 0;
        for (Player jugador : xo.getPlayers()) {
            jugador.sendTitle("§fX  ·  O", "§e" + frase, 5, FRASE_SEGUNDOS * 20, 10);
            jugador.sendActionBar("§e" + frase);
            jugador.sendMessage("§e" + frase + " §7· X falso, O verdadero");
        }
        barra.setTitle(frase);
        barra.setProgress(1);
    }

    private void revelar(World xo) {
        enPregunta = false;
        List<Player> dentro = new ArrayList<>();
        for (Player jugador : new ArrayList<>(xo.getPlayers())) {
            if (!vivos.contains(jugador.getUniqueId())) {
                continue;
            }
            int x = jugador.getLocation().getBlockX();
            boolean enX = x <= -2;
            boolean enO = x >= 2;
            boolean acierta = (enX && !verdad) || (enO && verdad);
            if (!acierta) {
                vivos.remove(jugador.getUniqueId());
                jugador.teleport(grada(xo));
                jugador.sendMessage("§cFuera. §fLa respuesta era " + (verdad ? "verdadero, O." : "falso, X."));
            } else {
                dentro.add(jugador);
                jugador.teleport(new Location(xo, 0.5, 65, 8.5, 180, 0));
                jugador.sendMessage("§aSigues. §fEra " + (verdad ? "verdadero." : "falso."));
            }
        }
        if (dentro.size() <= 1) {
            acabarRonda(xo);
            return;
        }
        if (cerrarAlAcabar || cierreAhora()) {
            cerrarPorTiempo();
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (abierta && !cerrarAlAcabar && !cierreAhora()) {
                empezarPregunta(xo);
            } else if (abierta) {
                cerrarPorTiempo();
            }
        }, 60L);
    }

    private void acabarRonda(World xo) {
        Player ganador = null;
        for (Player jugador : xo.getPlayers()) {
            if (vivos.contains(jugador.getUniqueId())) {
                ganador = jugador;
                break;
            }
        }
        if (ganador != null) {
            int duros = premiar(ganador);
            String texto = duros > 0
                ? ganador.getName() + " se lleva " + duros + " duros."
                : ganador.getName() + " gana la ronda.";
            Bukkit.broadcastMessage("§e" + texto);
        } else {
            Bukkit.broadcastMessage("§eEsta ronda no tiene ganador.");
        }
        if (cerrarAlAcabar || cierreAhora()) {
            cerrarPorTiempo();
            return;
        }
        prepararSiguiente(xo);
    }

    private void prepararSiguiente(World xo) {
        enPregunta = false;
        empezo = false;
        espera = 0;
        vivos.clear();
        participantes.clear();
        for (Player jugador : new ArrayList<>(xo.getPlayers())) {
            jugador.setFallDistance(0f);
            retener(jugador);
            jugador.teleport(new Location(xo, 0.5, 65, 8.5, 180, 0));
            jugador.setFallDistance(0f);
            vivos.add(jugador.getUniqueId());
            barra.addPlayer(jugador);
            jugador.sendMessage("§6Ronda siguiente en el próximo hueco. §fX es falso. O es verdadero.");
        }
    }

    private boolean cierreAhora() {
        if (manualHasta > 0) {
            return System.currentTimeMillis() >= manualHasta;
        }
        ZonedDateTime madrid = ZonedDateTime.now(Cita.MADRID);
        int minuto = madrid.getHour() * 60 + madrid.getMinute();
        return madrid.getDayOfWeek() == DayOfWeek.SATURDAY && Cita.esCierre(minuto);
    }

    private boolean sabadoPendiente() {
        ZonedDateTime madrid = ZonedDateTime.now(Cita.MADRID);
        String hoy = madrid.toLocalDate().toString();
        if (hoy.equals(jugada) || hoy.equals(cancelada)) {
            return false;
        }
        return Cita.de(madrid).recuperar();
    }

    private String textoCierreDiscord() {
        String base = manualHasta > 0 ? "La sesión de la XO se ha cerrado." : "La XO se ha cerrado.";
        if (ultimo == null || ultimo.isEmpty()) {
            return base;
        }
        return base + " La última ronda la gana " + ultimo + ".";
    }

    private int premiar(Player jugador) {
        ultimo = jugador.getName();
        guardarUltimo();
        if (economia == null) {
            prepararEconomia();
        }
        int duros = Bote.premio(bote);
        if (economia == null) {
            jugador.sendMessage("§cHas ganado la XO, pero la economía no está lista.");
            return 0;
        }
        EconomyResponse respuesta = economia.depositPlayer(jugador, duros);
        if (!respuesta.transactionSuccess()) {
            jugador.sendMessage("§cHas ganado la XO, pero no he podido darte los duros.");
            return 0;
        }
        jugador.sendMessage("§aGanas la XO. §f+" + duros + " duros.");
        return duros;
    }

    private void prepararEconomia() {
        RegisteredServiceProvider<Economy> registro = getServer().getServicesManager().getRegistration(Economy.class);
        if (registro != null) {
            economia = registro.getProvider();
        }
    }

    private String reloj(long segundosRestantes) {
        return String.format(java.util.Locale.ROOT, "%d:%02d", segundosRestantes / 60, segundosRestantes % 60);
    }

    private boolean enviarDiscord(String texto) {
        try {
            Plugin srv = getServer().getPluginManager().getPlugin("DiscordSRV");
            if (srv == null || !srv.isEnabled()) {
                return false;
            }
            Object jda = srv.getClass().getMethod("getJda").invoke(srv);
            if (jda == null) {
                return false;
            }
            Object estado = jda.getClass().getMethod("getStatus").invoke(jda);
            if (estado == null || !"CONNECTED".equals(String.valueOf(estado))) {
                return false;
            }
            Object canal = canal(jda);
            if (canal == null) {
                if (!canalAusente) {
                    canalAusente = true;
                    getLogger().warning("No está el canal de anuncios en Discord.");
                }
                return false;
            }
            Object accion = accionEnviar(canal, texto);
            for (java.lang.reflect.Method metodo : accion.getClass().getMethods()) {
                if (metodo.getName().equals("queue") && metodo.getParameterCount() == 0) {
                    metodo.invoke(accion);
                    return true;
                }
            }
            throw new NoSuchMethodException("queue");
        } catch (ReflectiveOperationException e) {
            Throwable causa = e.getCause() == null ? e : e.getCause();
            getLogger().warning("No pude avisar en Discord: " + causa.getMessage());
            return false;
        }
    }

    private Object canal(Object jda) throws ReflectiveOperationException {
        String[] nombres = {"getNewsChannelById", "getTextChannelById", "getGuildChannelById"};
        for (String nombre : nombres) {
            try {
                Object canal = jda.getClass().getMethod(nombre, String.class).invoke(jda, ANUNCIOS);
                if (canal != null) {
                    return canal;
                }
            } catch (NoSuchMethodException ignorado) {
                continue;
            }
        }
        return null;
    }

    private Object accionEnviar(Object canal, String texto) throws ReflectiveOperationException {
        java.lang.reflect.Method elegido = null;
        for (java.lang.reflect.Method metodo : canal.getClass().getMethods()) {
            if (!metodo.getName().equals("sendMessage") || metodo.getParameterCount() != 1) {
                continue;
            }
            Class<?> tipo = metodo.getParameterTypes()[0];
            if (tipo == String.class) {
                elegido = metodo;
                break;
            }
            if (tipo.isAssignableFrom(String.class)) {
                elegido = metodo;
            }
        }
        if (elegido == null) {
            throw new NoSuchMethodException("sendMessage");
        }
        return elegido.invoke(canal, texto);
    }

    private void cargarUltimo() {
        java.io.File archivo = new java.io.File(getDataFolder(), "ultimo.yml");
        if (!archivo.exists()) {
            return;
        }
        ultimo = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(archivo).getString("nombre", "");
    }

    private void guardarUltimo() {
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.set("nombre", ultimo);
        try {
            yaml.save(new java.io.File(getDataFolder(), "ultimo.yml"));
        } catch (java.io.IOException e) {
            getLogger().warning("No pude guardar al ganador de la XO: " + e.getMessage());
        }
    }

    private void cargarCita() {
        java.io.File archivo = new java.io.File(getDataFolder(), "cita.yml");
        if (!archivo.exists()) {
            return;
        }
        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(archivo);
        aviso = yaml.getString("aviso", "");
        avisoDiscord = yaml.getBoolean("avisoDiscord", false);
        jugada = yaml.getString("jugada", "");
        cancelada = yaml.getString("cancelada", "");
        enCurso = yaml.getBoolean("enCurso", false);
        manualDesde = yaml.getLong("manualDesde", 0L);
        manualHasta = yaml.getLong("manualHasta", 0L);
        retenidos.clear();
        estados.clear();
        var guardados = yaml.getConfigurationSection("estados");
        if (guardados != null) {
            for (String clave : guardados.getKeys(false)) {
                try {
                    GameMode modo = GameMode.valueOf(guardados.getString(clave + ".modo", "SURVIVAL"));
                    estados.put(UUID.fromString(clave), new Estado(
                        modo,
                        guardados.getBoolean(clave + ".vuelo"),
                        guardados.getBoolean(clave + ".volando")
                    ));
                } catch (IllegalArgumentException ignorado) {
                    continue;
                }
            }
        }
        for (String id : yaml.getStringList("retenidos")) {
            try {
                retenidos.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignorado) {
                continue;
            }
        }
    }

    private void guardarCita() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        yaml.set("aviso", aviso);
        yaml.set("avisoDiscord", avisoDiscord);
        yaml.set("jugada", jugada);
        yaml.set("cancelada", cancelada);
        yaml.set("enCurso", enCurso);
        yaml.set("manualDesde", manualDesde);
        yaml.set("manualHasta", manualHasta);
        yaml.set("retenidos", retenidos.stream().map(UUID::toString).toList());
        for (Map.Entry<UUID, Estado> entrada : estados.entrySet()) {
            String base = "estados." + entrada.getKey();
            Estado estado = entrada.getValue();
            yaml.set(base + ".modo", estado.modo.name());
            yaml.set(base + ".vuelo", estado.vuelo);
            yaml.set(base + ".volando", estado.volando);
        }
        try {
            yaml.save(new java.io.File(getDataFolder(), "cita.yml"));
        } catch (java.io.IOException e) {
            getLogger().warning("No pude guardar la cita de la XO: " + e.getMessage());
        }
    }

    private void retener(Player jugador) {
        if (retenidos.add(jugador.getUniqueId())) {
            estados.put(jugador.getUniqueId(), new Estado(
                jugador.getGameMode(),
                jugador.getAllowFlight(),
                jugador.isFlying()
            ));
            guardarCita();
        }
        jugador.setGameMode(GameMode.ADVENTURE);
        jugador.setCollidable(false);
        jugador.setInvulnerable(true);
    }

    private boolean soltar(Player jugador) {
        jugador.setCollidable(true);
        jugador.setInvulnerable(false);
        UUID id = jugador.getUniqueId();
        boolean cambio = retenidos.remove(id);
        Estado estado = estados.remove(id);
        if (estado != null) {
            jugador.setGameMode(estado.modo);
            aplicarVuelo(jugador, estado);
            Bukkit.getScheduler().runTask(this, () -> {
                if (jugador.isOnline()) {
                    aplicarVuelo(jugador, estado);
                }
            });
            return true;
        }
        if (jugador.getGameMode() == GameMode.ADVENTURE) {
            jugador.setGameMode(GameMode.SURVIVAL);
            cambio = true;
        }
        return cambio;
    }

    private void aplicarVuelo(Player jugador, Estado estado) {
        boolean puede = estado.vuelo
            || estado.modo == GameMode.CREATIVE
            || estado.modo == GameMode.SPECTATOR;
        jugador.setAllowFlight(puede);
        if (estado.volando && puede) {
            jugador.setFlying(true);
        }
    }

    private void revisarModo(Player jugador) {
        boolean enXo = jugador.getWorld().getName().equals("xo");
        if (abierta && enXo) {
            entrar(jugador);
            return;
        }
        if (!enXo && jugador.getGameMode() != GameMode.ADVENTURE && !retenidos.contains(jugador.getUniqueId())) {
            return;
        }
        boolean guardar = soltar(jugador);
        if (enXo) {
            llevarAlBarrio(jugador);
            guardar = true;
        }
        if (guardar) {
            guardarCita();
        }
    }

    private void llevarAlBarrio(Player jugador) {
        World barrio = Bukkit.getWorld("world");
        if (barrio == null) {
            return;
        }
        jugador.setFallDistance(0f);
        jugador.teleport(new Location(barrio, 0.5, 65, 12.5, 180, 0));
        jugador.setFallDistance(0f);
    }

    private Location grada(World xo) {
        return grada(xo, 180f, 20f);
    }

    private Location grada(World xo, float giro, float alto) {
        return new Location(xo, 0.5, 69, 16.5, giro, alto);
    }

    private boolean espectador(Player jugador) {
        return abierta && !vivos.contains(jugador.getUniqueId());
    }

    private boolean fueraDeGrada(Location sitio) {
        return sitio.getX() < -10 || sitio.getX() > 11
            || sitio.getZ() < 14 || sitio.getZ() > 19
            || sitio.getY() < 68.5 || sitio.getY() > 73.5;
    }

    private void fuera(Player jugador) {
        jugador.setInvulnerable(false);
        if (!abierta) {
            return;
        }
        if (!vivos.remove(jugador.getUniqueId())) {
            return;
        }
        barra.removePlayer(jugador);
    }

    @EventHandler
    public void alDano(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player jugador && jugador.getWorld().getName().equals("xo")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void alUnirse(PlayerJoinEvent event) {
        Player jugador = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> {
            if (jugador.isOnline()) {
                revisarModo(jugador);
            }
        });
    }

    @EventHandler
    public void alSalir(PlayerQuitEvent event) {
        Player jugador = event.getPlayer();
        if (jugador.getWorld().getName().equals("xo")) {
            fuera(jugador);
        }
        if (jugador.getWorld().getName().equals("xo") || retenidos.contains(jugador.getUniqueId()) || jugador.getGameMode() == GameMode.ADVENTURE) {
            if (soltar(jugador)) {
                guardarCita();
            }
        }
    }

    @EventHandler
    public void alCambiarMundo(PlayerChangedWorldEvent event) {
        if (!event.getFrom().getName().equals("xo")) {
            return;
        }
        fuera(event.getPlayer());
        if (soltar(event.getPlayer())) {
            guardarCita();
        }
    }

    @EventHandler
    public void alCaer(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }
        if (!event.getTo().getWorld().getName().equals("xo")) {
            return;
        }
        Player jugador = event.getPlayer();
        if (espectador(jugador)) {
            if (fueraDeGrada(event.getTo())) {
                event.setTo(grada(event.getTo().getWorld(), jugador.getLocation().getYaw(), jugador.getLocation().getPitch()));
                jugador.setFallDistance(0f);
            }
            return;
        }
        if (event.getTo().getY() < 60) {
            event.setTo(new Location(event.getTo().getWorld(), 0.5, 65, 8.5, 180, 0));
            jugador.setFallDistance(0f);
        }
    }

    private record Pregunta(String texto, boolean verdad) {}

    private static final class Estado {
        private final GameMode modo;
        private final boolean vuelo;
        private final boolean volando;

        private Estado(GameMode modo, boolean vuelo, boolean volando) {
            this.modo = modo;
            this.vuelo = vuelo;
            this.volando = volando;
        }
    }
}
