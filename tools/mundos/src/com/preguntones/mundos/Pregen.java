package com.preguntones.mundos;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

final class Pregen {
    private final MundosPlugin plugin;
    private final Object api;
    private final Method corriendo;
    private final Method empezar;
    private final Method cancelar;
    private final Method mundoDe;
    private final Method motivoDe;
    private final Set<String> arrancados = new HashSet<>();

    private Pregen(MundosPlugin plugin, Object api, Method corriendo, Method empezar, Method cancelar,
            Method mundoDe, Method motivoDe) {
        this.plugin = plugin;
        this.api = api;
        this.corriendo = corriendo;
        this.empezar = empezar;
        this.cancelar = cancelar;
        this.mundoDe = mundoDe;
        this.motivoDe = motivoDe;
    }

    static Pregen enganchar(MundosPlugin plugin) {
        Plugin chunky = plugin.getServer().getPluginManager().getPlugin("Chunky");
        if (chunky == null) {
            return null;
        }
        try {
            ClassLoader loader = chunky.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("org.popcraft.chunky.api.ChunkyAPI", true, loader);
            RegisteredServiceProvider<?> registro = plugin.getServer().getServicesManager().getRegistration(apiClass);
            if (registro == null) {
                plugin.getLogger().warning("Chunky está instalado pero no publicó su API.");
                return null;
            }
            Class<?> evento = Class.forName(
                "org.popcraft.chunky.api.event.task.GenerationCompleteEvent", true, loader);
            Method motivo = null;
            try {
                motivo = evento.getMethod("reason");
            } catch (NoSuchMethodException ignorado) {
                motivo = null;
            }
            Pregen pregen = new Pregen(
                plugin,
                registro.getProvider(),
                apiClass.getMethod("isRunning", String.class),
                apiClass.getMethod("startTask", String.class, String.class, double.class, double.class,
                    double.class, double.class, String.class),
                apiClass.getMethod("cancelTask", String.class),
                evento.getMethod("world"),
                motivo);
            Method alta = apiClass.getMethod("onGenerationComplete", Consumer.class);
            alta.invoke(pregen.api, (Consumer<Object>) aviso -> pregen.alTerminar(aviso));
            plugin.getServer().getScheduler().runTaskLater(plugin, pregen::iniciarSiFalta, 100L);
            return pregen;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("No pude enganchar Chunky: " + e.getMessage());
            return null;
        }
    }

    void prepararReset() {
        arrancados.clear();
        for (String nombre : plugin.ordenPregen()) {
            if (estaCorriendo(nombre)) {
                try {
                    cancelar.invoke(api, nombre);
                } catch (ReflectiveOperationException e) {
                    plugin.getLogger().warning("No pude parar la pregeneración de " + nombre + ".");
                }
            }
        }
    }

    void iniciarSiFalta() {
        if (plugin.pregenAlDia()) {
            return;
        }
        boolean hay = false;
        for (String nombre : plugin.ordenPregen()) {
            if (estaCorriendo(nombre)) {
                arrancados.add(nombre);
                hay = true;
            }
        }
        if (hay) {
            return;
        }
        empezarDesde(0);
    }

    private void alTerminar(Object aviso) {
        try {
            if (!terminoBien(aviso)) {
                return;
            }
            String nombre = (String) mundoDe.invoke(aviso);
            plugin.getServer().getScheduler().runTask(plugin, () -> siguiente(nombre));
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("No pude leer el aviso de Chunky: " + e.getMessage());
        }
    }

    private boolean terminoBien(Object aviso) throws ReflectiveOperationException {
        if (motivoDe == null) {
            return true;
        }
        Object motivo = motivoDe.invoke(aviso);
        return motivo != null && "COMPLETED".equals(motivo.toString());
    }

    private void siguiente(String terminado) {
        if (!arrancados.contains(terminado) || plugin.pregenAlDia()) {
            return;
        }
        List<String> orden = plugin.ordenPregen();
        int indice = orden.indexOf(terminado);
        if (indice < 0) {
            return;
        }
        if (indice + 1 >= orden.size()) {
            plugin.marcarPregen();
            plugin.getLogger().info("El círculo de aterrizaje ya está generado.");
            return;
        }
        empezarDesde(indice + 1);
    }

    private void empezarDesde(int indice) {
        List<String> orden = plugin.ordenPregen();
        for (int i = indice; i < orden.size(); i++) {
            if (estaCorriendo(orden.get(i))) {
                return;
            }
            if (lanzar(orden.get(i))) {
                return;
            }
        }
        if (indice == 0) {
            plugin.getLogger().warning("Chunky no pudo pregenerar el círculo de aterrizaje.");
        }
    }

    private boolean lanzar(String mundo) {
        double radio = plugin.radioPregen(mundo);
        try {
            boolean arranco = (boolean) empezar.invoke(api, mundo, "circle", 0d, 0d, radio, radio, "region");
            if (arranco) {
                arrancados.add(mundo);
                plugin.getLogger().info("Pregenero " + mundo + " en un radio de " + (int) radio + " bloques.");
            }
            return arranco;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Chunky no arrancó " + mundo + ": " + e.getMessage());
            return false;
        }
    }

    private boolean estaCorriendo(String mundo) {
        try {
            return (boolean) corriendo.invoke(api, mundo);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
