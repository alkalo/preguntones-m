package com.preguntones.viajes;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** El botón de Escape vive en el datapack del mundo. Se copia al arrancar, sin subir el barrio. */
public final class PanelBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        copiarEn(List.of(
            Path.of("world"),
            Path.of(System.getProperty("user.dir", ".")).resolve("world"),
            Path.of("/home/container/world")
        ), Logger.getLogger("Viajes"));
    }

    static boolean copiarEn(List<Path> raices, Logger log) {
        for (Path raiz : raices) {
            Path mundo = buscar(raiz);
            if (mundo != null) {
                return copiar(mundo, log);
            }
        }
        log.warning("No está el datapack del panel.");
        return false;
    }

    private static Path buscar(Path inicio) {
        Path actual = inicio.toAbsolutePath().normalize();
        for (int i = 0; i < 8 && actual != null; i++) {
            if (Files.isDirectory(actual.resolve("datapacks/preguntones/data/preguntones/dialog"))) {
                return actual;
            }
            actual = actual.getParent();
        }
        return null;
    }

    static boolean copiar(Path mundo, Logger log) {
        try {
            byte[] nuevo = leer();
            if (nuevo == null) {
                log.warning("El jar de Viajes no trae el panel de Escape.");
                return false;
            }
            Path destino = mundo.resolve("datapacks/preguntones/data/preguntones/dialog/panel.json");
            if (!Files.isDirectory(destino.getParent())) {
                log.warning("No está el datapack del panel: " + destino.toAbsolutePath());
                return false;
            }
            if (Files.exists(destino) && Arrays.equals(Files.readAllBytes(destino), nuevo)) {
                return false;
            }
            Path temporal = destino.resolveSibling("panel.json.tmp");
            Files.write(temporal, nuevo);
            try {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomico) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Panel de Escape actualizado.");
            return true;
        } catch (Exception e) {
            log.warning("No pude actualizar el panel de Escape: " + e.getMessage());
            return false;
        }
    }

    private static byte[] leer() throws IOException, URISyntaxException {
        try (InputStream entrada = PanelBootstrap.class.getResourceAsStream("/panel.json")) {
            if (entrada != null) {
                return entrada.readAllBytes();
            }
        }
        var origen = PanelBootstrap.class.getProtectionDomain().getCodeSource();
        if (origen == null || origen.getLocation() == null) {
            return null;
        }
        Path jar = Path.of(origen.getLocation().toURI());
        if (!Files.isRegularFile(jar)) {
            return null;
        }
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("panel.json");
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                return in.readAllBytes();
            }
        }
    }
}
