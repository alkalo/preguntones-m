package com.preguntones.recuperar;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Códigos ya entregados. La clave del mapa es el nombre dentro del juego. */
final class Cuentas {

    static final int MAX_FALLOS = 5;
    static final long BLOQUEO_MS = 15 * 60 * 1000L;

    static final class Ficha {
        String huella;
        String sal;
        int fallos;
        long hasta;
        long reenvio;
    }

    static final long REENVIO_PUERTA_MS = 10 * 60 * 1000L;
    static final long REENVIO_DENTRO_MS = 60 * 1000L;

    static final class Borrador {
        final String codigo;
        final String sal;
        final String huella;

        Borrador(String codigo, String sal, String huella) {
            this.codigo = codigo;
            this.sal = sal;
            this.huella = huella;
        }
    }

    private final File archivo;
    private final SecureRandom azar = new SecureRandom();
    private final Map<String, Ficha> fichas = new LinkedHashMap<>();

    Cuentas(File archivo) {
        this.archivo = archivo;
        if (archivo.getParentFile() != null) {
            archivo.getParentFile().mkdirs();
        }
        if (!archivo.isFile()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        for (String nombre : yaml.getKeys(false)) {
            ConfigurationSection seccion = yaml.getConfigurationSection(nombre);
            if (seccion == null) {
                continue;
            }
            Ficha ficha = new Ficha();
            ficha.huella = seccion.getString("huella", "");
            ficha.sal = seccion.getString("sal", "");
            ficha.fallos = seccion.getInt("fallos");
            ficha.hasta = seccion.getLong("hasta");
            ficha.reenvio = seccion.getLong("reenvio");
            if (ficha.huella.isEmpty() || ficha.sal.isEmpty()) {
                continue;
            }
            fichas.put(nombre, ficha);
        }
    }

    int cuantas() {
        return fichas.size();
    }

    synchronized boolean tiene(String nombre) {
        return buscar(nombre) != null;
    }

    /** El código ya salió por Discord. Un código viejo, solo guardado aquí, no cuenta. */
    synchronized boolean entregado(String nombre) {
        Ficha ficha = buscar(nombre);
        return ficha != null && ficha.reenvio != 0;
    }

    /** El código en claro, solo si esta cuenta aún no tenía uno. */
    synchronized String crearSiFalta(String nombre) {
        if (buscar(nombre) != null) {
            return null;
        }
        return escribir(nombre);
    }

    synchronized String reemplazar(String nombre) {
        return escribir(nombre);
    }

    synchronized Borrador preparar() {
        String codigo = Codigo.generar(azar);
        String sal = Huella.sal(azar);
        return new Borrador(codigo, sal, Huella.calcular(sal, codigo));
    }

    synchronized void confirmar(String nombre, Borrador borrador, long reenvio) {
        String clave = claveDe(nombre);
        if (clave == null) {
            clave = nombre;
        }
        Ficha ficha = new Ficha();
        ficha.sal = borrador.sal;
        ficha.huella = borrador.huella;
        ficha.fallos = 0;
        ficha.hasta = 0;
        ficha.reenvio = reenvio;
        Ficha anterior = fichas.get(clave);
        fichas.put(clave, ficha);
        try {
            guardar();
        } catch (RuntimeException e) {
            if (anterior == null) {
                fichas.remove(clave);
            } else {
                fichas.put(clave, anterior);
            }
            throw e;
        }
    }

    /** Null si se puede pedir otro código. */
    synchronized String esperaReenvio(String nombre, long ahora) {
        Ficha ficha = buscar(nombre);
        if (ficha == null || ficha.reenvio == 0 || ahora >= ficha.reenvio) {
            return null;
        }
        long minutos = Math.max(1, (ficha.reenvio - ahora + 59_999) / 60_000);
        return "Acabo de enviarte un código a Discord. Espera " + minutos + (minutos == 1 ? " minuto." : " minutos.");
    }

    synchronized void borrar(String nombre) {
        String clave = claveDe(nombre);
        if (clave == null) {
            return;
        }
        fichas.remove(clave);
        guardar();
    }

    synchronized boolean coincide(String nombre, String codigo) {
        Ficha ficha = buscar(nombre);
        return ficha != null && Huella.coincide(ficha.sal, ficha.huella, codigo);
    }

    /** Null si ahora mismo se puede intentar. */
    synchronized String espera(String nombre, long ahora) {
        Ficha ficha = buscar(nombre);
        if (ficha == null || ahora >= ficha.hasta) {
            return null;
        }
        long minutos = Math.max(1, (ficha.hasta - ahora + 59_999) / 60_000);
        return "Demasiados intentos. Espera " + minutos + (minutos == 1 ? " minuto." : " minutos.");
    }

    synchronized void acertar(String nombre) {
        Ficha ficha = buscar(nombre);
        if (ficha == null) {
            return;
        }
        ficha.fallos = 0;
        ficha.hasta = 0;
        guardar();
    }

    synchronized String fallar(String nombre, long ahora) {
        Ficha ficha = buscar(nombre);
        if (ficha == null) {
            return "Esta cuenta no tiene código de recuperación.";
        }
        if (ahora < ficha.hasta) {
            return espera(nombre, ahora);
        }
        ficha.fallos++;
        if (ficha.fallos >= MAX_FALLOS) {
            ficha.fallos = 0;
            ficha.hasta = ahora + BLOQUEO_MS;
            guardar();
            return "Demasiados intentos. Espera 15 minutos.";
        }
        int quedan = MAX_FALLOS - ficha.fallos;
        guardar();
        return "Código incorrecto. Te quedan " + quedan + (quedan == 1 ? " intento." : " intentos.");
    }

    private String escribir(String nombre) {
        String clave = claveDe(nombre);
        if (clave == null) {
            clave = nombre;
        }
        String codigo = Codigo.generar(azar);
        Ficha ficha = new Ficha();
        ficha.sal = Huella.sal(azar);
        ficha.huella = Huella.calcular(ficha.sal, codigo);
        ficha.fallos = 0;
        ficha.hasta = 0;
        Ficha anterior = fichas.get(clave);
        fichas.put(clave, ficha);
        try {
            guardar();
        } catch (RuntimeException e) {
            if (anterior == null) {
                fichas.remove(clave);
            } else {
                fichas.put(clave, anterior);
            }
            throw e;
        }
        return codigo;
    }

    private Ficha buscar(String nombre) {
        String clave = claveDe(nombre);
        return clave == null ? null : fichas.get(clave);
    }

    private String claveDe(String nombre) {
        for (String clave : fichas.keySet()) {
            if (clave.equalsIgnoreCase(nombre)) {
                return clave;
            }
        }
        return null;
    }

    private void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Ficha> entrada : fichas.entrySet()) {
            String ruta = entrada.getKey();
            Ficha ficha = entrada.getValue();
            yaml.set(ruta + ".huella", ficha.huella);
            yaml.set(ruta + ".sal", ficha.sal);
            yaml.set(ruta + ".fallos", ficha.fallos);
            yaml.set(ruta + ".hasta", ficha.hasta);
            yaml.set(ruta + ".reenvio", ficha.reenvio);
        }
        try {
            yaml.save(archivo);
        } catch (IOException e) {
            throw new IllegalStateException("No pude guardar los códigos de recuperación", e);
        }
    }
}
