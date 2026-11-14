package com.preguntones.censura;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/** Quita la contraseña de las líneas que el servidor escribe en la consola y reenvía a los operadores. */
public final class FiltroConsola extends AbstractFilter {

    private static final ThreadLocal<Boolean> DENTRO = ThreadLocal.withInitial(() -> false);

    public FiltroConsola() {
        super(Result.NEUTRAL, Result.NEUTRAL);
    }

    @Override
    public Result filter(LogEvent event) {
        if (DENTRO.get()) {
            return Result.NEUTRAL;
        }
        String texto = event.getMessage().getFormattedMessage();
        String censurado = Clave.censurar(texto);
        if (censurado.equals(texto)) {
            return Result.NEUTRAL;
        }
        DENTRO.set(true);
        try {
            LogManager.getLogger(event.getLoggerName()).log(event.getLevel(), censurado);
        } finally {
            DENTRO.set(false);
        }
        return Result.DENY;
    }
}
