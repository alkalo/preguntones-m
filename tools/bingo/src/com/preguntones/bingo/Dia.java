package com.preguntones.bingo;

import java.time.ZoneId;
import java.time.ZonedDateTime;

final class Dia {
    static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final int CORTE = 5;

    private Dia() {
    }

    static String id(ZonedDateTime ahora) {
        if (ahora.getHour() < CORTE) {
            return ahora.toLocalDate().minusDays(1).toString();
        }
        return ahora.toLocalDate().toString();
    }
}
