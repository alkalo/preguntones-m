package com.preguntones.mundos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public final class MundosBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            try {
                Path pack = extraer(context);
                event.registrar().discoverPack(pack, "preguntones_progresos", config -> config
                    .autoEnableOnServerStart(true)
                    .position(true, Datapack.Position.TOP));
            } catch (IOException e) {
                java.util.logging.Logger.getLogger("Mundos").warning("No pude cargar los progresos: " + e.getMessage());
            }
        });
    }

    private static Path extraer(BootstrapContext context) throws IOException {
        Path datos = context.getDataDirectory();
        Files.createDirectories(datos);
        Path origen = context.getPluginSource();
        if (Files.isDirectory(origen)) {
            Path junto = origen.resolve("progresos-pack");
            if (Files.isDirectory(junto)) {
                return junto;
            }
        }
        if (!Files.isRegularFile(origen)) {
            throw new IOException("no está el jar de Mundos");
        }
        try (ZipFile zip = new ZipFile(origen.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String nombre = entry.getName().replace('\\', '/');
                if (!nombre.startsWith("progresos-pack/") || nombre.contains("..")) {
                    continue;
                }
                Path out = datos.resolve(nombre).normalize();
                if (!out.startsWith(datos)) {
                    continue;
                }
                if (entry.isDirectory() || nombre.endsWith("/")) {
                    Files.createDirectories(out);
                    continue;
                }
                Files.createDirectories(out.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Path pack = datos.resolve("progresos-pack");
        if (!Files.isRegularFile(pack.resolve("pack.mcmeta"))) {
            throw new IOException("el paquete de progresos no trae pack.mcmeta");
        }
        return pack;
    }
}
