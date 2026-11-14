package com.preguntones.xo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public final class Pruebas {
    public static void main(String[] args) {
        if (fecha(2026, 11, 14, 21, 30).getDayOfWeek() != DayOfWeek.SATURDAY) {
            throw new IllegalStateException("14 de noviembre de 2026 no es sábado");
        }
        igual(new Cita.Momento(false, false, false), Cita.de(fecha(2026, 11, 13, 21, 20)));
        igual(new Cita.Momento(false, false, false), Cita.de(fecha(2026, 11, 14, 21, 14)));
        igual(new Cita.Momento(true, false, false), Cita.de(fecha(2026, 11, 14, 21, 15)));
        igual(new Cita.Momento(true, false, false), Cita.de(fecha(2026, 11, 14, 21, 29)));
        igual(new Cita.Momento(false, true, true), Cita.de(fecha(2026, 11, 14, 21, 30)));
        igual(new Cita.Momento(false, true, true), Cita.de(fecha(2026, 11, 14, 21, 44)));
        igual(new Cita.Momento(false, false, true), Cita.de(fecha(2026, 11, 14, 21, 45)));
        igual(new Cita.Momento(false, false, true), Cita.de(fecha(2026, 11, 14, 22, 29)));
        igual(new Cita.Momento(false, false, false), Cita.de(fecha(2026, 11, 14, 22, 30)));
        if (Cita.ultimoHueco(21 * 60 + 34) != -1 || Cita.ultimoHueco(21 * 60 + 35) != 21 * 60 + 35) {
            throw new IllegalStateException("El primer hueco no es las 21:35");
        }
        if (Cita.ultimoHueco(21 * 60 + 44) != 21 * 60 + 35 || Cita.ultimoHueco(22 * 60 + 25) != 22 * 60 + 25) {
            throw new IllegalStateException("El hueco en curso no es el último ya empezado");
        }
        if (Cita.ultimoHueco(22 * 60 + 29) != 22 * 60 + 25 || Cita.ultimoHueco(22 * 60 + 30) != -1) {
            throw new IllegalStateException("A las 22:30 ya no hay hueco");
        }
        if (Cita.siguienteHueco(21 * 60 + 35) != 21 * 60 + 45 || Cita.siguienteHueco(22 * 60 + 25) != -1) {
            throw new IllegalStateException("El hueco siguiente no cuadra");
        }
        if (!Cita.esCierre(22 * 60 + 30) || Cita.esCierre(22 * 60 + 29)) {
            throw new IllegalStateException("El cierre no es las 22:30");
        }
        if (!Cita.proxima(fecha(2026, 11, 14, 18, 0)).equals(fecha(2026, 11, 14, 21, 30))) {
            throw new IllegalStateException("El sábado por la tarde la cita no es esa noche");
        }
        if (!Cita.proxima(fecha(2026, 11, 14, 22, 30)).equals(fecha(2026, 11, 21, 21, 30))) {
            throw new IllegalStateException("Después del cierre la cita no es el sábado siguiente");
        }
        if (!Cita.proxima(fecha(2026, 11, 15, 12, 0)).equals(fecha(2026, 11, 21, 21, 30))) {
            throw new IllegalStateException("El domingo la cita no es el sábado siguiente");
        }
        if (!Cita.sabadoPendiente(fecha(2026, 11, 12, 15, 0)).equals(LocalDate.of(2026, 11, 14))) {
            throw new IllegalStateException("El jueves el sábado pendiente no es el 14");
        }
        if (!Cita.sabadoPendiente(fecha(2026, 11, 14, 21, 0)).equals(LocalDate.of(2026, 11, 14))) {
            throw new IllegalStateException("El sábado pendiente no es el mismo día");
        }
        if (!Cita.sabadoPendiente(fecha(2026, 11, 15, 12, 0)).equals(LocalDate.of(2026, 11, 21))) {
            throw new IllegalStateException("El domingo el sábado pendiente no es el 21");
        }
        if (Cita.ultimoHuecoSesion(4) != -1 || Cita.ultimoHuecoSesion(5) != 5 || Cita.ultimoHuecoSesion(59) != 55) {
            throw new IllegalStateException("Los huecos de una sesión a mano no cuadran");
        }
        if (Cita.ultimoHuecoSesion(60) != -1 || Cita.siguienteHuecoSesion(0) != 5 || Cita.siguienteHuecoSesion(55) != -1) {
            throw new IllegalStateException("La sesión a mano no cierra a los 60 minutos");
        }
        if (Bote.premio(5) != 8 || Bote.premio(1) != 8 || Bote.premio(0) != 8) {
            throw new IllegalStateException("el premio no son 8 duros fijos");
        }
        System.out.println("Cita de la XO: bien");
    }

    private static ZonedDateTime fecha(int ano, int mes, int dia, int hora, int minuto) {
        return ZonedDateTime.of(ano, mes, dia, hora, minuto, 0, 0, Cita.MADRID);
    }

    private static void igual(Cita.Momento esperado, Cita.Momento real) {
        if (!esperado.equals(real)) {
            throw new IllegalStateException(esperado + " != " + real);
        }
    }
}
