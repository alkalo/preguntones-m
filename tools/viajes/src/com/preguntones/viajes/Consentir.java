package com.preguntones.viajes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Consentir {
    public static final long ESPERA = 30_000L;

    public enum Tipo {
        VISITA,
        SALIDA
    }

    public enum Que {
        VISITA_ACEPTADA,
        VISITA_RECHAZADA,
        VISITA_CADUCADA,
        VISITA_SUSTITUIDA,
        VISITA_AUSENTE,
        VISITA_RETIRADA,
        SALIDA_ACEPTADA,
        SALIDA_RECHAZADA,
        SALIDA_CANCELADA,
        SALIDA_APARTADA,
        SALIDA_AUSENTE,
        GRUPO_LISTO
    }

    public static final class Peticion {
        public final UUID pide;
        public final UUID responde;
        public final Tipo tipo;
        public final long vence;

        private Peticion(UUID pide, UUID responde, Tipo tipo, long vence) {
            this.pide = pide;
            this.responde = responde;
            this.tipo = tipo;
            this.vence = vence;
        }
    }

    public static final class Hecho {
        public final Que que;
        public final UUID pide;
        public final UUID responde;
        public final List<UUID> aceptados;
        public final List<UUID> callados;

        private Hecho(Que que, UUID pide, UUID responde, List<UUID> aceptados, List<UUID> callados) {
            this.que = que;
            this.pide = pide;
            this.responde = responde;
            this.aceptados = aceptados;
            this.callados = callados;
        }

        public String toString() {
            return que + " pide=" + pide + " responde=" + responde;
        }
    }

    private static final class Grupo {
        private final UUID lider;
        private final Set<UUID> esperando = new LinkedHashSet<>();
        private final Set<UUID> aceptados = new LinkedHashSet<>();
        private final long vence;

        private Grupo(UUID lider, long vence) {
            this.lider = lider;
            this.vence = vence;
        }
    }

    private final Map<UUID, Peticion> entrantes = new HashMap<>();
    private final Map<UUID, Grupo> grupos = new HashMap<>();

    public Peticion pedirVisita(UUID pide, UUID responde, long ahora, List<Hecho> efectos) {
        if (pide == null || responde == null || pide.equals(responde)) {
            return null;
        }
        Peticion ya = entrantes.get(responde);
        if (ya != null && ahora >= ya.vence) {
            efectos.addAll(caducar(ahora));
            ya = entrantes.get(responde);
        }
        if (ya != null && ya.tipo == Tipo.VISITA && ya.pide.equals(pide)) {
            Peticion nueva = new Peticion(pide, responde, Tipo.VISITA, ahora + ESPERA);
            entrantes.put(responde, nueva);
            return nueva;
        }
        if (ya != null) {
            efectos.addAll(soltar(responde));
        }
        Peticion nueva = new Peticion(pide, responde, Tipo.VISITA, ahora + ESPERA);
        entrantes.put(responde, nueva);
        return nueva;
    }

    public List<Hecho> pedirSalida(UUID lider, List<UUID> cerca, long ahora) {
        List<Hecho> hechos = new ArrayList<>();
        if (lider == null) {
            return hechos;
        }
        hechos.addAll(cancelarGrupo(lider));
        hechos.addAll(apartarDeGrupos(lider, false));
        long vence = ahora + ESPERA;
        Grupo grupo = new Grupo(lider, vence);
        if (cerca != null) {
            for (UUID id : cerca) {
                if (id == null || id.equals(lider) || !grupo.esperando.add(id)) {
                    continue;
                }
                hechos.addAll(soltar(id));
                entrantes.put(id, new Peticion(lider, id, Tipo.SALIDA, vence));
            }
        }
        if (grupo.esperando.isEmpty()) {
            return hechos;
        }
        grupos.put(lider, grupo);
        return hechos;
    }

    public List<Hecho> aceptar(UUID responde, long ahora) {
        return responder(responde, ahora, true);
    }

    public List<Hecho> rechazar(UUID responde, long ahora) {
        return responder(responde, ahora, false);
    }

    public List<Hecho> caducar(long ahora) {
        List<Peticion> vencidas = new ArrayList<>();
        for (Peticion peticion : entrantes.values()) {
            if (ahora >= peticion.vence) {
                vencidas.add(peticion);
            }
        }
        Map<UUID, List<UUID>> callados = new HashMap<>();
        List<Hecho> hechos = new ArrayList<>();
        for (Peticion peticion : vencidas) {
            entrantes.remove(peticion.responde);
            if (peticion.tipo == Tipo.VISITA) {
                hechos.add(hecho(Que.VISITA_CADUCADA, peticion.pide, peticion.responde));
                continue;
            }
            Grupo grupo = grupos.get(peticion.pide);
            if (grupo == null) {
                continue;
            }
            grupo.esperando.remove(peticion.responde);
            callados.computeIfAbsent(peticion.pide, id -> new ArrayList<>()).add(peticion.responde);
        }
        for (Map.Entry<UUID, List<UUID>> entry : callados.entrySet()) {
            Grupo grupo = grupos.get(entry.getKey());
            if (grupo != null && (grupo.esperando.isEmpty() || ahora >= grupo.vence)) {
                hechos.add(cerrar(grupo, null, entry.getValue()));
            }
        }
        return hechos;
    }

    public List<Hecho> salir(UUID quien) {
        List<Hecho> hechos = new ArrayList<>();
        if (quien == null) {
            return hechos;
        }
        hechos.addAll(cancelarGrupo(quien));
        hechos.addAll(retirarVisitas(quien));
        hechos.addAll(apartarDeGrupos(quien, true));
        Peticion entrada = entrantes.remove(quien);
        if (entrada != null && entrada.tipo == Tipo.VISITA) {
            hechos.add(hecho(Que.VISITA_AUSENTE, entrada.pide, quien));
        }
        return hechos;
    }

    public List<Hecho> dejarSalida(UUID lider) {
        List<Hecho> hechos = new ArrayList<>();
        if (lider == null) {
            return hechos;
        }
        hechos.addAll(cancelarGrupo(lider));
        hechos.addAll(apartarDeGrupos(lider, false));
        return hechos;
    }

    public boolean tieneSalida(UUID lider) {
        return grupos.containsKey(lider);
    }

    public List<UUID> esperandoDe(UUID lider) {
        Grupo grupo = grupos.get(lider);
        if (grupo == null) {
            return List.of();
        }
        return List.copyOf(grupo.esperando);
    }

    private List<Hecho> responder(UUID responde, long ahora, boolean acepta) {
        Peticion peticion = entrantes.get(responde);
        if (peticion == null) {
            return List.of();
        }
        if (ahora >= peticion.vence) {
            return caducar(ahora);
        }
        entrantes.remove(responde);
        if (peticion.tipo == Tipo.VISITA) {
            Que que = acepta ? Que.VISITA_ACEPTADA : Que.VISITA_RECHAZADA;
            return List.of(hecho(que, peticion.pide, responde));
        }
        Grupo grupo = grupos.get(peticion.pide);
        if (grupo == null) {
            return List.of();
        }
        grupo.esperando.remove(responde);
        if (acepta) {
            grupo.aceptados.add(responde);
        }
        if (grupo.esperando.isEmpty() || ahora >= grupo.vence) {
            return List.of(cerrar(grupo, responde, List.of()));
        }
        Que que = acepta ? Que.SALIDA_ACEPTADA : Que.SALIDA_RECHAZADA;
        return List.of(new Hecho(que, grupo.lider, responde, List.copyOf(grupo.aceptados), List.of()));
    }

    private List<Hecho> soltar(UUID responde) {
        Peticion previa = entrantes.remove(responde);
        if (previa == null) {
            return List.of();
        }
        if (previa.tipo == Tipo.VISITA) {
            return List.of(hecho(Que.VISITA_SUSTITUIDA, previa.pide, responde));
        }
        Grupo grupo = grupos.get(previa.pide);
        if (grupo == null) {
            return List.of();
        }
        grupo.esperando.remove(responde);
        grupo.aceptados.remove(responde);
        List<Hecho> hechos = new ArrayList<>();
        hechos.add(hecho(Que.SALIDA_APARTADA, grupo.lider, responde));
        if (grupo.esperando.isEmpty()) {
            hechos.add(cerrar(grupo, null, List.of()));
        }
        return hechos;
    }

    private List<Hecho> cancelarGrupo(UUID lider) {
        Grupo grupo = grupos.remove(lider);
        if (grupo == null) {
            return List.of();
        }
        List<Hecho> hechos = new ArrayList<>();
        Set<UUID> gente = new LinkedHashSet<>();
        gente.addAll(grupo.esperando);
        gente.addAll(grupo.aceptados);
        for (UUID id : gente) {
            Peticion peticion = entrantes.get(id);
            if (peticion != null && peticion.tipo == Tipo.SALIDA && peticion.pide.equals(lider)) {
                entrantes.remove(id);
            }
            hechos.add(hecho(Que.SALIDA_CANCELADA, lider, id));
        }
        return hechos;
    }

    private List<Hecho> retirarVisitas(UUID pide) {
        List<UUID> responden = new ArrayList<>();
        for (Peticion peticion : entrantes.values()) {
            if (peticion.tipo == Tipo.VISITA && peticion.pide.equals(pide)) {
                responden.add(peticion.responde);
            }
        }
        List<Hecho> hechos = new ArrayList<>();
        for (UUID responde : responden) {
            entrantes.remove(responde);
            hechos.add(hecho(Que.VISITA_RETIRADA, pide, responde));
        }
        return hechos;
    }

    private List<Hecho> apartarDeGrupos(UUID quien, boolean ausente) {
        List<Hecho> hechos = new ArrayList<>();
        for (UUID lider : new ArrayList<>(grupos.keySet())) {
            if (lider.equals(quien)) {
                continue;
            }
            Grupo grupo = grupos.get(lider);
            if (grupo == null) {
                continue;
            }
            boolean esperaba = grupo.esperando.remove(quien);
            boolean iba = grupo.aceptados.remove(quien);
            if (!esperaba && !iba) {
                continue;
            }
            if (esperaba) {
                Peticion peticion = entrantes.get(quien);
                if (peticion != null && peticion.tipo == Tipo.SALIDA && peticion.pide.equals(lider)) {
                    entrantes.remove(quien);
                }
            }
            Que que = ausente ? Que.SALIDA_AUSENTE : Que.SALIDA_APARTADA;
            hechos.add(hecho(que, lider, quien));
            if (grupo.esperando.isEmpty()) {
                hechos.add(cerrar(grupo, null, List.of()));
            }
        }
        return hechos;
    }

    private Hecho cerrar(Grupo grupo, UUID ultimo, List<UUID> callados) {
        grupos.remove(grupo.lider);
        return new Hecho(Que.GRUPO_LISTO, grupo.lider, ultimo, List.copyOf(grupo.aceptados), List.copyOf(callados));
    }

    private static Hecho hecho(Que que, UUID pide, UUID responde) {
        return new Hecho(que, pide, responde, List.of(), List.of());
    }
}
