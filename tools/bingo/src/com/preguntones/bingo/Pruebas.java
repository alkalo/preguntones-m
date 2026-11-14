package com.preguntones.bingo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Pruebas {
    public static void main(String[] args) {
        ZoneId madrid = ZoneId.of("Europe/Madrid");
        if (!Dia.id(ZonedDateTime.of(2026, 11, 13, 4, 59, 0, 0, madrid)).equals("2026-11-12")) {
            throw new IllegalStateException("Antes de las 05:00 el día no es el anterior");
        }
        if (!Dia.id(ZonedDateTime.of(2026, 11, 13, 5, 0, 0, 0, madrid)).equals("2026-11-13")) {
            throw new IllegalStateException("A las 05:00 el día no cambia");
        }
        if (!Dia.id(ZonedDateTime.of(2026, 11, 13, 23, 30, 0, 0, madrid)).equals("2026-11-13")) {
            throw new IllegalStateException("Por la noche el día no se mantiene");
        }
        if (!Dia.id(ZonedDateTime.of(2027, 1, 1, 4, 0, 0, 0, madrid)).equals("2026-12-31")) {
            throw new IllegalStateException("El cambio de año no cruza a las 05:00");
        }

        Set<String> ids = new HashSet<>();
        int faciles = 0;
        for (Casilla casilla : Casilla.values()) {
            if (casilla.texto.isBlank() || !ids.add(casilla.id)) {
                throw new IllegalStateException("La casilla " + casilla + " no tiene id propio");
            }
            if (casilla.facil) {
                faciles++;
            }
        }
        if (faciles < 4 || Casilla.values().length < Carta.TAMANO) {
            throw new IllegalStateException("No hay casillas bastantes para una carta");
        }

        Set<String> distintas = new HashSet<>();
        LocalDate fecha = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 365; i++) {
            List<Casilla> carta = Carta.elegir(fecha.toString());
            if (!carta.equals(Carta.elegir(fecha.toString()))) {
                throw new IllegalStateException("La carta del " + fecha + " no se queda quieta");
            }
            if (carta.size() != Carta.TAMANO || new HashSet<>(carta).size() != Carta.TAMANO) {
                throw new IllegalStateException("La carta del " + fecha + " no tiene 9 casillas distintas");
            }
            int suaves = 0;
            for (Casilla casilla : carta) {
                if (casilla.facil) {
                    suaves++;
                }
            }
            if (suaves < 4) {
                throw new IllegalStateException("La carta del " + fecha + " sale demasiado dura");
            }
            distintas.add(carta.toString());
            fecha = fecha.plusDays(1);
        }
        if (distintas.size() < 2) {
            throw new IllegalStateException("Todas las cartas del año son la misma");
        }

        if (Premio.toca(8, 9, false) || !Premio.toca(9, 9, false) || Premio.toca(9, 9, true)) {
            throw new IllegalStateException("El premio no sale al llenar la carta una sola vez");
        }
    }
}
