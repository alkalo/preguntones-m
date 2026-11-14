package com.preguntones.censura;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sustituye los argumentos de los comandos de cuenta por asteriscos. */
public final class Clave {

    private static final Pattern COMANDO = Pattern.compile(
        "(?i)(^|[\\s:])(/(?:login|log|l|register|reg|changepassword|changepass|cp|unregister|unreg|recuperar))\\b((?:\\s+\\S+)*)");

    private Clave() {
    }

    public static String censurar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        Matcher matcher = COMANDO.matcher(texto);
        StringBuilder salida = new StringBuilder();
        while (matcher.find()) {
            String argumentos = matcher.group(3).replaceAll("\\S+", "****");
            matcher.appendReplacement(salida, Matcher.quoteReplacement(matcher.group(1) + matcher.group(2) + argumentos));
        }
        matcher.appendTail(salida);
        return salida.toString();
    }
}
