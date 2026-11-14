package com.preguntones.xo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

final class Cita {
    static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    record Momento(boolean aviso, boolean apertura, boolean recuperar) {}

    private Cita() {}

    static Momento de(ZonedDateTime ahora) {
        if (ahora.getDayOfWeek() != DayOfWeek.SATURDAY) {
            return new Momento(false, false, false);
        }
        int minuto = ahora.getHour() * 60 + ahora.getMinute();
        boolean aviso = minuto >= 21 * 60 + 15 && minuto < 21 * 60 + 30;
        boolean apertura = minuto >= 21 * 60 + 30 && minuto < 21 * 60 + 45;
        boolean recuperar = minuto >= 21 * 60 + 30 && minuto < 22 * 60 + 30;
        return new Momento(aviso, apertura, recuperar);
    }

    static int ultimoHueco(int minutoDia) {
        if (minutoDia < 21 * 60 + 35 || minutoDia >= 22 * 60 + 30) {
            return -1;
        }
        int ultimo = -1;
        for (int hueco : huecos()) {
            if (minutoDia >= hueco) {
                ultimo = hueco;
            }
        }
        return ultimo;
    }

    static int siguienteHueco(int minutoDia) {
        for (int hueco : huecos()) {
            if (minutoDia < hueco) {
                return hueco;
            }
        }
        return -1;
    }

    static boolean esCierre(int minutoDia) {
        return minutoDia >= 22 * 60 + 30;
    }

    static int ultimoHuecoSesion(int minuto) {
        if (minuto < 5 || minuto >= 60) {
            return -1;
        }
        int ultimo = -1;
        for (int hueco : huecosSesion()) {
            if (minuto >= hueco) {
                ultimo = hueco;
            }
        }
        return ultimo;
    }

    static int siguienteHuecoSesion(int minuto) {
        for (int hueco : huecosSesion()) {
            if (minuto < hueco) {
                return hueco;
            }
        }
        return -1;
    }

    static ZonedDateTime proxima(ZonedDateTime ahora) {
        ZonedDateTime cita = ahora.withHour(21).withMinute(30).withSecond(0).withNano(0);
        ZonedDateTime cierre = ahora.withHour(22).withMinute(30).withSecond(0).withNano(0);
        if (ahora.getDayOfWeek() == DayOfWeek.SATURDAY && ahora.isBefore(cierre)) {
            return cita;
        }
        int faltan = DayOfWeek.SATURDAY.getValue() - ahora.getDayOfWeek().getValue();
        if (faltan <= 0) {
            faltan += 7;
        }
        return ahora.toLocalDate().plusDays(faltan).atTime(21, 30).atZone(MADRID);
    }

    private static int[] huecos() {
        return new int[] {21 * 60 + 35, 21 * 60 + 45, 21 * 60 + 55, 22 * 60 + 5, 22 * 60 + 15, 22 * 60 + 25};
    }

    private static int[] huecosSesion() {
        return new int[] {5, 15, 25, 35, 45, 55};
    }

    static LocalDate sabadoPendiente(ZonedDateTime ahora) {
        LocalDate fecha = ahora.toLocalDate();
        int faltan = DayOfWeek.SATURDAY.getValue() - fecha.getDayOfWeek().getValue();
        if (faltan < 0) {
            faltan += 7;
        }
        return fecha.plusDays(faltan);
    }
}
